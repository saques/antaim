package formats;

import formats.exceptions.FormatException;

import java.io.FileInputStream;
import java.io.IOException;

public class Ppm extends Image {

    public Ppm(String path) throws IOException {
        super(0, 0, Encoding.RGB);

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

        data = new byte[width*height];
        inputStream.read(data);
    }

}
