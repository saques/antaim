package formats;

import formats.exceptions.FormatException;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;

public class Ppm extends Image {

    public Ppm(String path) throws IOException {
        super(0, 0, Encoding.RGB, false);

        int depth;

        FileInputStream inputStream = new FileInputStream(path);
        if(!"P6".equals(Utils.readToWhiteSpace(inputStream)))
            throw new FormatException("Illegal magic number");
        try {
            String str = Utils.readToWhiteSpace(inputStream);
            width = Integer.valueOf(str);
            str = Utils.readToWhiteSpace(inputStream);
            height = Integer.valueOf(str);
            str = Utils.readToWhiteSpace(inputStream);
            depth = Integer.valueOf(str);
        } catch (NumberFormatException e){
            throw new FormatException("Illegal width format");
        }

        if(depth >= 256)
            throw new FormatException("Unsupported color depth");

        byte[] bytes = new byte[width*height*encoding.getBands()];
        inputStream.read(bytes);

        data = new double[width*height*encoding.getBands()];

        for(int i = 0; i < bytes.length; i++)
            data[i] = byteToDouble(bytes[i]);
    }

}
