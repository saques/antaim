import formats.Encoding;
import formats.Image;
import formats.Raw;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class ImageDrawingUtils {

    private ImageDrawingUtils() {}

    /**
     * Attempts to draw a circle of specified radius centered in x, y.
     * If circle exceeds boundaries, the circle is partially drawn.
     * @param image
     * @param x
     * @param y
     * @param radius
     */
    public static void drawCircle(Image image, int x, int y, int radius, double[] rgb){
        if(image.isOutOfBounds(x, y))
            throw new IndexOutOfBoundsException();
        for(int i = x-radius; i < x+radius; i++)
            for (int j = y-radius; j < y+radius; j++)
                if(!image.isOutOfBounds(i, j) && (pithagoras(x, i, y, j) <= radius))
                    image.setComponents(i, j, rgb);


    }

    public static double pithagoras(double x1, double x2, double y1, double y2){
        return Math.sqrt(Math.pow(x2 - x1,2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Draws a rectangle in the specified area.
     * @param image
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param rgb
     */
    public static void drawRectangle(Image image, int x1, int y1, int x2, int y2, double[] rgb){
        if(image.isOutOfBounds(x1, y1) || image.isOutOfBounds(x2, y2))
            throw new IndexOutOfBoundsException();
        if(x1 >= x2 || y1 >= y2)
            throw new IllegalArgumentException("Point 1 must be lower than point 2");
        for(int x = x1; x < x2; x++)
            for(int y = y1; y < y2; y++)
                image.setComponents(x, y, rgb);
    }

    public static Image greyScale(){
        Image image = new Image(256, 256, Encoding.GS, true);
        for(int x = 0; x < image.getWidth(); x++)
            for(int y = 0; y < image.getWidth(); y++)
                image.setComponent(x, y, 0, ((double)x)/255);
        return image;
    }

    public static Image colorScale(int fixed, double value){
        if(fixed < 0 || fixed >= 3 || value < 0 || value > 1)
            throw new IllegalArgumentException();
        Image image = new Image(256, 256, Encoding.RGB, true);
        for(int x = 0; x < image.getWidth(); x++)
            for(int y = 0; y < image.getWidth(); y++)
                image.setComponents(x, y,  getRgbOneFixed(fixed, value, x, y));
        return image;
    }

    private static double[] getRgbOneFixed(int fixed, double value, int x, int y){
        double rgb[] = new double[3];
        rgb[fixed] = value;
        if(fixed == 0){
            rgb[1] = ((double)x)/255;
            rgb[2] = ((double)y)/255;
        } else if(fixed == 1){
            rgb[0] = ((double)x)/255;
            rgb[2] = ((double)y)/255;
        } else {
            rgb[0] = ((double)x)/255;
            rgb[1] = ((double)y)/255;
        }
        return rgb;
    }


    public static void main(String[] args) throws IOException {
        /*
        Image image = new Raw(290, 207, Encoding.GS, "D:\\git\\antaim\\images\\BARCO.RAW");
        image = image.copy(0,0,100,100);
        ImageIO.write(image.toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\BARCORIG.BMP"));
        */
        Image image = ImageDrawingUtils.colorScale(2,1);
        //double[] rgb = {1};
        //ImageDrawingUtils.drawRectangle(image,50,50,150,150, rgb);
        //ImageDrawingUtils.drawCircle(image,99,99,75,rgb);
        ImageIO.write(image.toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\COLORSCALE_B_1.BMP"));
    }

}
