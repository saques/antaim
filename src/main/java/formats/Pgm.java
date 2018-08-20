package formats;

import formats.exceptions.FormatException;
import utils.FileUtils;

import java.io.FileInputStream;
import java.io.IOException;

public class Pgm extends Image {

    public Pgm(String path) throws IOException {
        super(0, 0, Encoding.GS,false);

        int depth;

        FileInputStream inputStream = new FileInputStream(path);
        if(!"P5".equals(FileUtils.readToWhiteSpace(inputStream)))
            throw new FormatException("Illegal magic number");
        try {
            String str = FileUtils.readToWhiteSpace(inputStream);
            width = Integer.valueOf(str);
            str = FileUtils.readToWhiteSpace(inputStream);
            height = Integer.valueOf(str);
            str = FileUtils.readToWhiteSpace(inputStream);
            depth = Integer.valueOf(str);
        } catch (NumberFormatException e){
            throw new FormatException("Illegal width format");
        }

        if(depth >= 256)
            throw new FormatException("Unsupported greyscale depth");

        byte[] bytes = new byte[width*height*encoding.getBands()];
        inputStream.read(bytes);

        data = new double[width*height*encoding.getBands()];

        for(int i = 0; i < bytes.length; i++)
             data[i] = byteToDouble(bytes[i]);
    }

}
