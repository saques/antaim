package formats;

import lombok.Getter;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;


public class Raw {

    private byte[] data;
    @Getter
    private int width, height;

    public Raw(int width, int height, String path) throws IOException {
        data = IOUtils.toByteArray(new FileInputStream(path));
    }







}
