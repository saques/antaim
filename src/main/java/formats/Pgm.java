package formats;

import formats.exceptions.FormatException;
import java.io.FileInputStream;
import java.io.IOException;

public class Pgm extends Image {

    public Pgm(String path) throws IOException {
        super(0, 0, Encoding.GS,false);

        int depth;

        FileInputStream inputStream = new FileInputStream(path);
        if(!"P5".equals(Utils.readToWhiteSpace(inputStream)))
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
            throw new FormatException("Unsupported greyscale depth");

        data = new byte[width*height];
        inputStream.read(data);
    }

}
