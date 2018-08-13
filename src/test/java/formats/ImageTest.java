package formats;

import static org.junit.Assert.*;
import org.junit.Test;

public class ImageTest {

    @Test
    public void rgbToHsv(){

        double[] rgb = {Math.random(), Math.random(), Math.random()};

        double[] hsv = Image.toHSV(rgb[0], rgb[1], rgb[2]);

        double[] rgb2 = Image.toRGB(hsv[0], hsv[1], hsv[2]);

        assertEquals(rgb[0], rgb2[0], 0.0001);
        assertEquals(rgb[1], rgb2[1], 0.0001);
        assertEquals(rgb[2], rgb2[2], 0.0001);
    }
}
