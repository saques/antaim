package formats;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;


public class Raw extends Image{

    public Raw(int width, int height, Encoding encoding, String path) throws IOException {
        super(width, height, encoding, true);
        byte[] bytes = IOUtils.toByteArray(new FileInputStream(path));
        for(int i = 0; i < data.length; i++) {
            data[i] = byteToDouble(bytes[i]);
        }
    }

}
