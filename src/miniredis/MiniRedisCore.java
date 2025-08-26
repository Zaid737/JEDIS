package miniredis;

import java.util.Map;
import java.util.concurrent.*;

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
    private final ConcurrentMap<String,Entry> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r ->{
        Thread t= new Thread(r,"ttl-cleaner");
        t.setDaemon(true);
        return t;
    });

    public MiniRedisCore(){
        //Preiodically remove expired keys
        cleaner.scheduleAtFixedRate(this::cleanExpiredKeys,1,1,TimeUnit.SECONDS);
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