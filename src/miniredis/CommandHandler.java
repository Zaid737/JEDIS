package miniredis;
import java.util.List;
public final class CommandHandler {
    private final MiniRedisCore core;

    public CommandHandler(MiniRedisCore core) {
        this.core = core;
    }

    public String handle(List<String> args){
        if(args == null || args.isEmpty()){
            return RESPEncoder.error("empty command");
        }
        String cmd = args.get(0).toUpperCase();

        switch(cmd){
            case "PING":
                if(args.size() > 1) return RESPEncoder.bulkString(args.get(1));
                return RESPEncoder.simpleString("PONG");
            case "SET":
                if(args.size() < 3) return RESPEncoder.error("wrong number of arguments for 'set' command");
                String key = args.get(1);
                String value = args.get(2);
                Long ttl = null;
                if(args.size() == 5 && "EX".equalsIgnoreCase(args.get(3))){
                    try{
                        ttl = Long.parseLong(args.get(4));
                    }catch(NumberFormatException e){
                        return RESPEncoder.error("value is not an integer or out of range");
                  }
        }
        String res = core.set(key, value, ttl);
        return RESPEncoder.simpleString(res);
            case "GET":
                if(args.size() != 2) return RESPEncoder.error("wrong number of arguments for 'get' command");
                return RESPEncoder.bulkString(core.get(args.get(1)));
            case "DEL":
                if(args.size() < 2) return RESPEncoder.error("wrong number of arguments for 'del' command");
                long removed = 0;
                for(int i=1;i<args.size();i++){
                    removed += core.del(args.get(i));
                }
                return RESPEncoder.integer(removed);
            case "PEXPIRE":
                if(args.size() != 3) return RESPEncoder.error("wrong number of arguments for 'pexpire' command");
                try{
                    long ms = Long.parseLong(args.get(2));
                    return RESPEncoder.integer(core.pexpire(args.get(1), ms));
                }catch(NumberFormatException e){
                    return RESPEncoder.error("value is not an integer or out of range");
                }
            default:
                return RESPEncoder.error("unknown command '" + cmd + "'");
            }       
                 
    }
}
