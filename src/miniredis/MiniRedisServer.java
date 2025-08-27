package miniredis;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiniRedisServer {
    private final MiniRedisCore core;
    private final ExecutorService pool;
    private final int port;

    public MiniRedisServer(int port,int workerThreads){
        this.core = new MiniRedisCore();
        this.pool = Executors.newFixedThreadPool(workerThreads);
        this.port=port;
    }

    public void start() throws IOException{
        try(ServerSocket server = new ServerSocket(port)){
            System.out.println("MiniRedis listeing on port "+ port);
            while(true){
                Socket client = server.accept();
                pool.submit(() -> handleClient(client));
            }
        }
    }

    private void handleClient(Socket client){
        try(client;
        InputStream in = new BufferedInputStream(client.getInputStream());
        OutputStream out = new BufferedOutputStream(client.getOutputStream())){

            RESPParser parser = new RESPParser(in);
            CommandHandler handler = new CommandHandler(core);

            while(true){
                List<String> req = parser.readRequest();
                if(req == null){
                    break; 
                }
                String resp = handler.handle(req);
                out.write(resp.getBytes());
                out.flush();
            }
        }catch(IOException e){
            
        }catch(Exception e){
            System.err.println("Unexpected error: " + e);
        }
    }


    public static void main(String[] args) throws Exception{
        int port = 6380;
        int workers = Runtime.getRuntime().availableProcessors();
        new MiniRedisServer(port,workers).start();
    }
}

