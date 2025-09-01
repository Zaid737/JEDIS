package miniredis;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.Set;
/**
 * Core in-memory KV store with optional TTL per key.
 */

public class MiniRedisCore{
    
    private static class Entry{
        final String value;
        final long expireAtMills; // <=0 means no expiry
        Entry(String value ,long expireAtMills){
            this.value = value;
            this.expireAtMills = expireAtMills;
        }
    }
    private final ConcurrentMap<String, ConcurrentMap<String, String>> hashes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String,Entry> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r ->{
        Thread t= new Thread(r,"ttl-cleaner");
        t.setDaemon(true);
        return t;
    });

    public MiniRedisCore(){
        //Preiodically remove expired keys
        cleaner.scheduleAtFixedRate(this::purgeExpired,1,1,TimeUnit.SECONDS);
    }

    public String set(String key,String value,Long ttlSeconds){
        long expireAt =  (ttlSeconds == null) ? 0 : System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds);
        store.put(key,new Entry(value,expireAt));
        return "OK";
    }

    public String get(String key){
        Entry e = store.get(key);
        if(e == null) return null;
        if(isExpired(e)){
            store.remove(key,e);
            return null;
        }
        return e.value;
    }

    public long del(String key){
        Entry e = store.remove(key);
        if(e == null) return 0;
        return isExpired(e) ? 0 : 1;
    }

    public long pexpire(String key,long ms){
        Entry e = store.get(key);
        if(e == null || isExpired(e)){
            store.remove(key);
            return 0;
        } 
        store.put(key,new Entry(e.value, System.currentTimeMillis() + ms));
        return 1;
    }
     public long ttl(String key) {
        Entry e = store.get(key);
        if (e == null) return -2;
        if (isExpired(e)) {
            store.remove(key, e);
            return -2;
        }
        if (e.expireAtMills <= 0) return -1;
        long ttlMillis = e.expireAtMills - System.currentTimeMillis();
        return ttlMillis > 0 ? ttlMillis / 1000 : -2;
    }
    public long hsset(String key, String field, String value) {
        hashes.putIfAbsent(key, new ConcurrentHashMap<>());
        ConcurrentMap<String, String> map = hashes.get(key);
        boolean isNew = !map.containsKey(field);
        map.put(field, value);
        return isNew ? 1 : 0;
    }

    // HSGET: get field from hash, return value or null
    public String hsget(String key, String field) {
        ConcurrentMap<String, String> map = hashes.get(key);
        if (map == null) return null;
        return map.get(field);
    }

    // HSDEL: delete field from hash, return 1 if deleted, 0 if not found
    public long hsdel(String key, String field) {
        ConcurrentMap<String, String> map = hashes.get(key);
        if (map == null) return 0;
        return map.remove(field) != null ? 1 : 0;
    }

    public Set<String> getStoreKeys() {
        return store.keySet();
    }
    public Set<String> getHashKeys() {
        return hashes.keySet();
    }

    private boolean isExpired(Entry e){
        return e.expireAtMills > 0 && System.currentTimeMillis() > e.expireAtMills;
    }

    private void purgeExpired(){
        long now = System.currentTimeMillis();
        for(Map.Entry<String,Entry> me : store.entrySet()){
            Entry e = me.getValue();
            if(e.expireAtMills > 0 && now > e.expireAtMills){
                store.remove(me.getKey(),e);
            }
        }
    }

} 