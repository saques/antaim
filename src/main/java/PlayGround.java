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
        equalizationTest();
    }

    /**
     * Try me
     */

    public static void contrastEnhancement() throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "D:\\git\\antaim\\images\\BARCO.RAW");
        ImageIO.write(barco.automaticContrastEnhancement(0).toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\BARCO_ENHANCED.BMP"));
    }

    public static void negative() throws IOException {
        //Image barco = new Raw(290, 207, Encoding.GS, "D:\\git\\antaim\\images\\BARCO.RAW");
        //ImageIO.write(barco.negative(0).toBufferedImage(), "bmp", new File("D:\\git\\antaim\\images\\BARCO_NEGATIVE.BMP"));
        Image barco = new Raw(290, 207, Encoding.GS, "C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\BARCO.RAW");
        ImageIO.write(barco.negative(0).toBufferedImage(), "bmp", new File("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\BARCO_NEGATIVE.BMP"));
    }

    public static void thresholding(double u) throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\BARCO.RAW");
        ImageIO.write(barco.thresholding(0,u).toBufferedImage(), "bmp", new File("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\BARCO_THRESHOLDED.BMP"));
    }

    public static void productWithScalar(double scalar) throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\BARCO.RAW");
        ImageIO.write(barco.productWithScalar(0,scalar).toBufferedImage(), "bmp", new File("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\BARCO_PRODUCT_SCALAR.BMP"));
    }

    public static void equalization() throws IOException {
        Image lena = new Raw(290, 207, Encoding.GS, "C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\LENA.RAW");
        ImageIO.write(lena.equalization(0).toBufferedImage(), "bmp", new File("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\LENA_EQUALIZATED2.BMP"));
        double[] histogram =  lena.equalization(0).histogram(0);
        PrintWriter writer = new PrintWriter(new FileWriter("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\histogram_lena_2.txt"));
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);
        writer.flush();
        writer.close();
    }

    public static void equalizationTest() throws IOException {
        Image lena = new Raw(256, 256, Encoding.GS, "C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\LENA.RAW");

        PrintWriter writer = new PrintWriter(new FileWriter("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\c2Lena.txt"));

        double[] histogram =  lena.equalizatedHisto(0);
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);
        writer.flush();

        Image aux =  lena.equalization(0);
         histogram = aux.histogram(0);
        ImageIO.write(aux.toBufferedImage(), "bmp", new File("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\LENA_EQUALIZATED2.BMP"));

        writer = new PrintWriter(new FileWriter("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\c1Lena.txt"));
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);



        writer.flush();
        writer.close();

    }

    public static void histogram(boolean negative) throws IOException {
       // Image barco = new Raw(290, 207, Encoding.GS, "D:\\git\\antaim\\images\\BARCO.RAW");
        //double[] histogram = negative ? barco.negative(0).histogram(0) : barco.histogram(0);
        //PrintWriter writer = new PrintWriter(new FileWriter("D:\\git\\antaim\\images\\histogram_barco" + (negative ? "_negative.txt" : ".txt")));
        Image barco = new Raw(290, 207, Encoding.GS, "C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\BARCO.RAW");
        double[] histogram = negative ? barco.negative(0).histogram(0) : barco.histogram(0);
        PrintWriter writer = new PrintWriter(new FileWriter("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\histogram_barco" + (negative ? "_negative.txt" : ".txt")));
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);
        writer.flush();
        writer.close();
    }

    public static void histogramLena() throws IOException {
        Image lena = new Raw(290, 207, Encoding.GS, "C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\LENA.RAW");
        double[] histogram =  lena.histogram(0);
        PrintWriter writer = new PrintWriter(new FileWriter("C:\\Users\\Nicolas\\Documents\\GitHub\\antaim\\images\\histogram_lena.txt"));
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
