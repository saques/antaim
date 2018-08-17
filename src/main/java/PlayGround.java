import formats.Encoding;
import formats.Image;
import formats.Pgm;
import formats.Raw;
import utils.ImageDrawingUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public final class PlayGround {

    private PlayGround() {}

    public static void main(String[] args) throws IOException {
        histogram();
    }

    /**
     * Try me
     */

    public static void negative() throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "D:\\git\\antaim\\images\\BARCO.RAW");
        ImageIO.write(barco.negative(0).toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\BARCO_NEGATIVE.BMP"));
    }

    public static void histogram() throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "D:\\git\\antaim\\images\\BARCO.RAW");
        double[] histogram = barco.histogram(0);
        PrintWriter writer = new PrintWriter(new FileWriter("D:\\git\\antaim\\images\\histogram_barco.txt"));
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);
        writer.flush();
        writer.close();
    }

    public static void drawMonochromeCircle() throws IOException {
        Image image = new Image(200,200, Encoding.GS, true);
        ImageDrawingUtils.drawCircle(image, 99,99,50, (x, y) -> new double[]{1,1,1});
        ImageIO.write(image.toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\CIRCLE.BMP"));
    }

    public static void drawMonochromeSquare() throws IOException{
        Image image = new Image(200,200, Encoding.GS, true);
        ImageDrawingUtils.drawRectangle(image, 50,50, 150, 150, (x, y) -> new double[]{1,1,1});
        ImageIO.write(image.toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\SQUARE.BMP"));
    }

    public static void drawGrayScale() throws IOException{
        ImageIO.write(ImageDrawingUtils.scale(Encoding.GS, ImageDrawingUtils.grayScale).toBufferedImage(),
                "bmp", new File("D:\\git\\antaim\\images\\GRAYSCALE.BMP"));
    }

    public static void selectTEST() throws IOException{
        Image image = new Pgm("/home/nmarcantonio/antaim/images/TEST.PGM");
        ImageIO.write(image.copy(9,69,53,96).toBufferedImage(),
                "bmp", new File("/home/nmarcantonio/antaim/images/SFASFASFAS.PGM"));
    }

    public static void drawFlashCircle() throws IOException{
        Image image = ImageDrawingUtils.drawCircle(
                ImageDrawingUtils.scale(Encoding.HSV,ImageDrawingUtils.fixedRGB(2, 1)),127,127, 50, ImageDrawingUtils.fixedRGB(1, 1));
        ImageIO.write(image.toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\FLASH.BMP"));
    }

    public static void drawCircleWithImage() throws IOException{
        Image barco = new Raw(290, 207, Encoding.GS, "D:\\git\\antaim\\images\\BARCO.RAW");
        ImageDrawingUtils.GradientColour colour = barco::getComponents;
        Image image = ImageDrawingUtils.drawCircle(
                ImageDrawingUtils.scale(Encoding.HSV,ImageDrawingUtils.fixedRGB(2, 1)),127,127, 50, colour);
        ImageIO.write(image.toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\USAIHDKSA.BMP"));
    }



}
