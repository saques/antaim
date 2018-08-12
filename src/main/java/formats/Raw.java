package formats;
import java.io.FileInputStream;
import java.io.IOException;


public class Raw extends Image{

    public Raw(int width, int height, Encoding encoding, String path) throws IOException {
        super(width, height, encoding, true);
        new FileInputStream(path).read(data);
    }

}
