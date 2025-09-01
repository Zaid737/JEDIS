package miniredis;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiniRedisServer {
    private final MiniRedisCore core;
    private final ExecutorService pool;
    private final int port;
    private final PersistenceManager persistenceManager;
    private final File aofFile;

    public MiniRedisServer (int port, int workerThreads) throws IOException {
        this.core = new MiniRedisCore();
        this.pool = Executors.newFixedThreadPool(workerThreads);
        this.port = port;
        this.aofFile = new File("appendonly.aof");
        this.persistenceManager = new PersistenceManager(aofFile.getPath());
    }

    public void start() throws IOException {
        // Replay AOF before starting server
        CommandHandler handler = new CommandHandler(core, persistenceManager, true);
        replay(handler);

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("MiniRedis listening on port " + port);
            while (true) {
                Socket client = server.accept();
                pool.submit(() -> handleClient(client));
            }
        }
    }

    private void handleClient(Socket client) {
        try (client;
             InputStream in = new BufferedInputStream(client.getInputStream());
             OutputStream out = new BufferedOutputStream(client.getOutputStream())) {

            RESPParser parser = new RESPParser(in);
            CommandHandler handler = new CommandHandler(core, persistenceManager, false);

            while (true) {
                List<String> req = parser.readRequest();
                if (req == null) {
                    break;
                }
                String resp = handler.handle(req);
                out.write(resp.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("I/O error with client: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e);
        }
    }

    private void replay(CommandHandler handler) throws IOException {
        if (!aofFile.exists() || aofFile.length() == 0) return;

        try (FileInputStream fis = new FileInputStream(aofFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             InputStreamReader isr = new InputStreamReader(bis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("*")) continue;
                int argCount = Integer.parseInt(line.substring(1));
                String[] args = new String[argCount];
                for (int i = 0; i < argCount; i++) {
                    String lenLine = reader.readLine();
                    if (lenLine == null || !lenLine.startsWith("$")) break;
                    int len = Integer.parseInt(lenLine.substring(1));
                    String data = reader.readLine();
                    args[i] = data;
                }
                if (args.length > 0) {
                    handler.handle(List.of(args));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 6380;
        int workers = Runtime.getRuntime().availableProcessors();
        new MiniRedisServer(port, workers).start();
    }
}