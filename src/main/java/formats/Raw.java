package formats;

import lombok.Getter;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;


public class Raw extends Image{

    public Raw(int width, int height, Encoding encoding, String path) throws IOException {
        super(width, height, encoding);
        data = IOUtils.toByteArray(new FileInputStream(path));
    }

    public static void main(String[] args) throws Exception {
        Image image = new Raw(389,164, Encoding.GS, "D:\\git\\antaim\\images\\GIRL.RAW");
    }



}
