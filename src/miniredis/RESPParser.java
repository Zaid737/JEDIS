package miniredis;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Minimal RESP reader supporting Arrays of Bulk Strings (what redis-cli sends).
 */

public final class RESPParser {
    private static final byte CR = 13; //'\r' ASCII
    private static final byte LF = 10; //'\n'
    private final InputStream in;

    public RESPParser(InputStream in){
        this.in = in;
    }
     /**
     * Parses one RESP request (typically an Array of Bulk Strings).
     * Returns null if stream closed gracefully.
     */

    public List<String> readRequest() throws IOException{
        String head = readLine();
        if(head == null) return null; //connection closed

        if(!head.startsWith("*")){
            throw new IOException("Expected  header , found: " + head);
        }

        int count = Integer.parseInt(head.substring(1)); //number of bulk string elements in the RESP array being parsed.
        List<String> args = new ArrayList<>(count);
        for(int i=0;i<count;i++){
            String bulkHead = readLine();
            if(bulkHead == null || !bulkHead.startsWith("$")){
                throw new IOException("Expected bulk string header, found: " + bulkHead);
            }
            int len = Integer.parseInt(bulkHead.substring(1));
            if(len == -1){
                args.add(null); //nill bulk string
            }else{
                byte[] data = readBulkBytes(len);
                args.add(new String(data, StandardCharsets.UTF_8));
            }
        }
        return args;
    }
     /** Read exactly n bytes, then expect CRLF and consume it. */
     private byte[] readBulkBytes(int n) throws IOException{
        byte[] buf = in.readNBytes(n);
        if(buf.length < n){
            throw new EOFException("Expected " + n + " bytes, found only " + buf.length);
        }
        // Consume CRLF
        int cr = in.read();
        int lf = in.read();
        if(cr != CR || lf != LF) {
            throw new IOException("Expected CRLF after bulk string");
        }
        return buf;
     }
        /** Read a line terminated by CRLF, return null if stream closed before any data read. */
        private String readLine() throws IOException{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while((b = in.read()) != -1){
                if(b == CR){
                    int next = in.read();
                    if(next == -1) break; //stream closed
                    if(next == LF) break; //end of line
                    baos.write(b);
                    baos.write(next);
                }else{
                    baos.write(b);
                }
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
}
