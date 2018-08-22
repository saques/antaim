package formats;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;

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

    @Test
    public void testByteToDoubleConversion(){
        int count[] = new int[256];
        for(int i = 0; i < 256; i++)
            count[0xFF & Image.doubleToByte(Image.byteToDouble((byte)i))] ++;
        for(int i = 0; i < 256; i++)
            assertEquals(1, count[i]);
    }

    @Test
    public void checkCorrectNegativeConversion() throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "images\\BARCO.RAW");
        double[] histogram = barco.histogram(0);
        double[] histogramNegative = barco.negative(0).histogram(0);
        for(int i = 0; i < 256; i++)
            assertEquals(0, Double.compare(histogramNegative[256-1-i], histogram[i]));
    }
}
