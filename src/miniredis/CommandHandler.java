package miniredis;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public final class CommandHandler {
    private final MiniRedisCore core;
    private final PersistenceManager persistenceManager;
    private final boolean replay; // true when replaying AOF

    public CommandHandler(MiniRedisCore core, PersistenceManager persistenceManager, boolean replay) {
        this.core = core;
        this.persistenceManager = persistenceManager;
        this.replay = replay;
    }

    public String handle(List<String> args) {
        if (args == null || args.isEmpty()) {
            return RESPEncoder.error("empty command");
        }
        String cmd = args.get(0).toUpperCase();

        // Log only write commands when not replaying
        if (!replay && isWriteCommand(cmd)) {
            try {
                persistenceManager.append(args);
            } catch (Exception e) {
                // TODO: handle exception
            }
            
        }

        switch (cmd) {
            case "PING":
                if (args.size() > 1) return RESPEncoder.bulkString(args.get(1));
                return RESPEncoder.simpleString("PONG");

            case "SET":
                if (args.size() < 3) return RESPEncoder.error("wrong number of arguments for 'set' command");
                String key = args.get(1);
                String value = args.get(2);
                Long ttl = null;
                if (args.size() == 5 && "EX".equalsIgnoreCase(args.get(3))) {
                    try {
                        ttl = Long.parseLong(args.get(4));
                    } catch (NumberFormatException e) {
                        return RESPEncoder.error("value is not an integer or out of range");
                    }
                }
                String res = core.set(key, value, ttl);
                return RESPEncoder.simpleString(res);

            case "GET":
                if (args.size() != 2) return RESPEncoder.error("wrong number of arguments for 'get' command");
                return RESPEncoder.bulkString(core.get(args.get(1)));

            case "DEL":
                if (args.size() < 2) return RESPEncoder.error("wrong number of arguments for 'del' command");
                long removed = 0;
                for (int i = 1; i < args.size(); i++) {
                    removed += core.del(args.get(i));
                }
                return RESPEncoder.integer(removed);

            case "PEXPIRE":
                if (args.size() != 3) return RESPEncoder.error("wrong number of arguments for 'pexpire' command");
                try {
                    long ms = Long.parseLong(args.get(2));
                    return RESPEncoder.integer(core.pexpire(args.get(1), ms));
                } catch (NumberFormatException e) {
                    return RESPEncoder.error("value is not an integer or out of range");
                }

            case "EXISTS":
                if (args.size() < 2) return RESPEncoder.error("wrong number of arguments for 'exists' command");
                key = args.get(1);
                return RESPEncoder.integer(core.get(key) != null ? 1 : 0);

            case "TTL":
                if (args.size() != 2) return RESPEncoder.error("wrong number of arguments for 'ttl' command");
                return RESPEncoder.integer(core.ttl(args.get(1)));

            case "HSSET":
                if (args.size() != 4) return RESPEncoder.error("wrong number of arguments for 'hsset' command");
                return RESPEncoder.integer(core.hsset(args.get(1), args.get(2), args.get(3)));

            case "HSGET":
                if (args.size() != 3) return RESPEncoder.error("wrong number of arguments for 'hsget' command");
                return RESPEncoder.bulkString(core.hsget(args.get(1), args.get(2)));

            case "HSDEL":
                if (args.size() != 3) return RESPEncoder.error("wrong number of arguments for 'hsdel' command");
                return RESPEncoder.integer(core.hsdel(args.get(1), args.get(2)));

            case "KEYS":
                if (args.size() != 2 || !args.get(1).equals("*")) {
                    return RESPEncoder.error("only KEYS * is supported");
                }
                Set<String> keys = new HashSet<>();
                keys.addAll(core.getStoreKeys());
                keys.addAll(core.getHashKeys());

                if (keys.isEmpty()) {
                    return "*0\r\n";
                }

                StringBuilder resp = new StringBuilder();
                resp.append("*").append(keys.size()).append("\r\n");
                for (String k : keys) {
                    resp.append("$").append(k.length()).append("\r\n").append(k).append("\r\n");
                }
                return resp.toString();

            default:
                return RESPEncoder.error("unknown command '" + cmd + "'");
        }
    }

    // Helper method to detect write commands
    private boolean isWriteCommand(String cmd) {
        switch (cmd) {
            case "SET":
            case "DEL":
            case "PEXPIRE":
            case "HSSET":
            case "HSDEL":
                return true;
            default:
                return false;
        }
    }
}