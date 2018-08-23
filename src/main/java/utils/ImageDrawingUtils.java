package utils;

import formats.Encoding;
import formats.Image;

public final class ImageDrawingUtils {


    @FunctionalInterface
    public interface GradientColour {
        double[] compute(int x, int y);
    }

    public static GradientColour fixedRGB(final int fixed, final double value){
        if(fixed < 0 || fixed >= 3 || value < 0 || value > 1)
            throw new IllegalArgumentException();
        return (x, y)-> getRgbOneFixed(fixed, value, x, y);
    }

    private static double[] getRgbOneFixed(int fixed, double value, int x, int y){
        double rgb[] = new double[3];
        rgb[fixed] = value;

        double xD = Image.byteToDouble((byte)x), yD = Image.byteToDouble((byte)y);

        if(fixed == 0){
            rgb[1] = xD;
            rgb[2] = yD;
        } else if(fixed == 1){
            rgb[0] = xD;
            rgb[2] = yD;
        } else {
            rgb[0] = xD;
            rgb[1] = yD;
        }
        return rgb;
    }

    public static final GradientColour grayScale = (x, y) -> {
        double fert = Image.byteToDouble((byte)x);
        return new double[]{fert, fert, fert};
    };

    private ImageDrawingUtils() {}

    /**
     * Attempts to draw a circle of specified radius centered in x, y.
     * If circle exceeds boundaries, the circle is partially drawn.
     * @param image
     * @param x
     * @param y
     * @param radius
     * @param gradientColour
     */
    public static Image drawCircle(Image image, int x, int y, int radius, GradientColour gradientColour){
        if(image.isOutOfBounds(x, y))
            throw new IndexOutOfBoundsException();
        for(int i = x-radius; i < x+radius; i++)
            for (int j = y-radius; j < y+radius; j++)
                if(!image.isOutOfBounds(i, j) && (pithagoras(x, i, y, j) <= radius))
                    image.setComponentsRGB(i, j, gradientColour.compute(i, j));
        return image;
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
     * @param gradientColour
     */
    public static Image drawRectangle(Image image, int x1, int y1, int x2, int y2, GradientColour gradientColour){
        if(image.isOutOfBounds(x1, y1) || image.isOutOfBounds(x2, y2))
            throw new IndexOutOfBoundsException();
        if(x1 >= x2 || y1 >= y2)
            throw new IllegalArgumentException("Point 1 must be lower than point 2");
        for(int x = x1; x < x2; x++)
            for(int y = y1; y < y2; y++)
                image.setComponentsRGB(x, y, gradientColour.compute(x, y));
        return image;
    }

    public static Image scale(Encoding encoding, GradientColour gradientColour){
        Image image = new Image(256, 256, encoding, true);
        for(int x = 0; x < image.getWidth(); x++)
            for(int y = 0; y < image.getWidth(); y++)
                image.setComponentsRGB(x, y, gradientColour.compute(x, y));
        return image;
    }

}
