package miniredis;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.io.Closeable;
// AOF file persistenceusing RESP encoding

public class PersistenceManager implements Closeable{
    private final File aofFile;
    private final FileOutputStream fos;
    private final BufferedOutputStream out;
    private final Object writeLock = new Object();


    public PersistenceManager(String path) throws IOException {
        Objects.requireNonNull(path);
        this.aofFile = new File(path);
        File parent = aofFile.getParentFile();
        if(parent != null) parent.mkdirs();
        //append mode
        this.fos = new FileOutputStream(aofFile,true);
        this.out = new BufferedOutputStream(fos);
    }

//Append a command to the AOF file
public void append(List<String> args) throws IOException{
    if(args == null || args.isEmpty()) return;
   synchronized(writeLock){
        //RESP Array header:<*<number of elements>\r\n
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(args.size()).append("\r\n");

        for(String arg: args){
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            //Bulk string: $<number of bytes>\r\n<data>\r\n
            sb.append("$").append(bytes.length).append("\r\n");
            sb.append(arg).append("\r\n");
        }
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
}
}
@Override
public void close() throws IOException{
    synchronized (writeLock){
        out.close();
        fos.close();
    }
}
}