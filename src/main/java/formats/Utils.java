package formats;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public final class Utils {

    private Utils() {}

    public static String readToWhiteSpace(FileInputStream inputStream) throws IOException {
        List<Byte> list = new LinkedList<>();
        byte c;
        while (!isWhitespace(c = (byte)inputStream.read()))
            list.add(c);
        return new String(toByteArray(list));
    }

    private static byte[] toByteArray(List<Byte> list){
        byte[] ans = new byte[list.size()];
        for(int i = 0; i < list.size(); i++)
            ans[i] = list.get(i);
        return ans;
    }

    private static boolean isWhitespace(byte c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

}
