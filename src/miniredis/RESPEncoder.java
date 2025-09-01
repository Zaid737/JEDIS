package miniredis;
public final class RESPEncoder {
    private static final String CRLF = "\r\n";

    // Encodes a simple string in RESP format
    public static String simpleString(String s){
        return "+" + s + CRLF;
    }
    // Encodes an error message in RESP format
    public static String error(String msg){
        return "-ERR " + msg + CRLF;
    }
    // Encodes an integer in RESP format
    public static String integer(long v){
        return ":" + v + CRLF;
    }
    // Encodes a bulk string in RESP format (null for nil)
    public static String bulkString(String s){
        if(s == null) return "$-1" + CRLF;
        return "$" + s.length() + CRLF + s + CRLF;
    }
}