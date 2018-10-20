import formats.*;
import noise.*;
import utils.ImageDrawingUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public final class PlayGround {

    private PlayGround() {}

    public static void main(String[] args) throws IOException {
        susanNewImage("images\\TEST.PNG");
    }

    /**
     * Try me
     */

    public static void anisotropicDiffusion(String file, int t, double sigma, Image.DiffusionBorderDetector detector) throws IOException{
        Image img = new Image(file);
        ImageIO.write(img.diffusion(t, sigma, detector).toBufferedImage(), "bmp", new File("images\\anisotropic.BMP"));
    }

    public static void prewitt(String file) throws IOException{
        Image img = new Image(file);
        ImageIO.write(img.toGS().prewitt().toBufferedImage(), "bmp", new File("images\\prewitt.BMP"));
    }

    public static void sobel(String file) throws IOException{
        Image img = new Image(file);
        ImageIO.write(img.toGS().sobel().toBufferedImage(), "bmp", new File("images\\sobel.BMP"));
    }

    public static void laplace(String file) throws IOException{
        Image img = new Image(file);
        ImageIO.write(img.toGS().laplace().toBufferedImage(), "bmp", new File("images\\laplace.BMP"));
    }

    public static void loGFilter(String file) throws IOException{
        Image img = new Image(file);
        ImageIO.write(img.toGS().loGFilter(7,1,0.00005).toBufferedImage(), "bmp", new File("images\\loGFilter.BMP"));
    }

    public static void bilateralFilter(String file) throws IOException{
        Image img = new Image(file);
        ImageIO.write(img.toGS().bilateralFilter(7,2,30).toBufferedImage(), "png", new File("images\\bilateralFilter.png"));
        ImageIO.write(img.toGS().bilateralFilter(7,20,30).toBufferedImage(), "png", new File("images\\bilateralFilter2.png"));

    }

    public static void otsu(String file) throws IOException{
        Image img = new Image(file);
        ImageIO.write(img.otsu().toBufferedImage(), "png", new File("images\\otsu.png"));

    }

    public static void susan(String file) throws IOException{
        //Image img = new Pgm("images/TEST.PGM");
        Image img = new Image(file);
        ImageIO.write(img.susanDetector(27.0/255.0,true).toBufferedImage(), "png", new File("images\\susanita.png"));

    }
    public static void susanNewImage(String file) throws IOException{
        //Image img = new Pgm("images/TEST.PGM");
        Image img = new Image(file);
        ImageIO.write(img.susanDetector(27.0/255.0,false).toBufferedImage(), "png", new File("images\\susanita.png"));

    }
    public static void weightedMedianFilter() throws IOException{
        Image img = new Image("images\\lena.jpg");
        img.contaminate(1, new SaltAndPepperGenerator(0.05), NoiseApplyMode.DESTRUCTIVE);
        ImageIO.write(img.toBufferedImage(), "bmp", new File("images\\lenaContaminated.BMP"));
        ImageIO.write(img.weightedMedianFilter().toBufferedImage(), "bmp", new File("images\\lenaTreated.BMP"));
    }

    public static void medianFilter() throws IOException{
        Image img = new Image("images\\lena.jpg");
        img.contaminate(1, new SaltAndPepperGenerator(0.05), NoiseApplyMode.DESTRUCTIVE);
        ImageIO.write(img.toBufferedImage(), "bmp", new File("images\\lenaContaminated.BMP"));
        ImageIO.write(img.medianFilter(3).toBufferedImage(), "bmp", new File("images\\lenaTreated.BMP"));
    }

    public static void saltAndPepper() throws IOException{
        Image img = new Image("images\\lena.jpg");
        img.contaminate(1, new SaltAndPepperGenerator(0.01), NoiseApplyMode.DESTRUCTIVE);
        ImageIO.write(img.toBufferedImage(), "bmp", new File("images\\lenaContaminated.BMP"));
    }

    public static void meanFilter(int n) throws IOException{
        Image img = new Image("images\\BARCO.BMP");
        ImageIO.write(img.meanFilter(n).toBufferedImage(), "bmp", new File("images\\barcoTreatedMean.BMP"));
    }


    public static void gaussFilter(int n , double sigma) throws IOException{
        Image img = new Image("images\\lena.jpg");
        img.contaminate(0.1, new GaussianNoiseGenerator(sigma,0), NoiseApplyMode.ADDITIVE);
        ImageIO.write(img.toBufferedImage(), "bmp", new File("images\\lenaContaminated.BMP"));
        ImageIO.write(img.gaussFilter(n,sigma).toBufferedImage(), "bmp", new File("images\\lenaTreated.BMP"));
    }


    public static void gaussianNoise(double sigma) throws IOException{
        Image img = new Image("images\\lena.jpg");
        img.contaminate(1, new GaussianNoiseGenerator(sigma,0), NoiseApplyMode.ADDITIVE);
        ImageIO.write(img.toBufferedImage(), "bmp", new File("images\\lenaContaminated.BMP"));
    }

    public static void exponentialNoise(double lambda) throws IOException{
        Image img = new Image("images\\lena.jpg");
        img.contaminate(1, new ExponentialNoiseGenerator(lambda), NoiseApplyMode.MULTIPLICATIVE);
        ImageIO.write(img.toBufferedImage(), "bmp", new File("images\\lenaContaminated.BMP"));
    }

    public static void rayleighNoise(double fi) throws IOException{
        Image img = new Image("images\\lena.jpg");
        img.contaminate(1, new RayleighNoiseGenerator(fi), NoiseApplyMode.MULTIPLICATIVE);
        ImageIO.write(img.toBufferedImage(), "bmp", new File("images\\lenaContaminated.BMP"));
    }


    public static void testPutin() throws IOException{
        Image putin = new Image("images\\putin.jpg").negative();
        ImageIO.write(putin.toBufferedImage(), "bmp", new File("images\\putinpromjpg.BMP"));
    }

    public static void testLena() throws IOException{
        Image lena = new Image("images\\lena.jpg").equalize();
        ImageIO.write(lena.toBufferedImage(), "bmp", new File("images\\lenafromjpg.BMP"));
    }

    public static void lenaSubGS() throws IOException{
        Image GS = ImageDrawingUtils.scale(Encoding.GS, ImageDrawingUtils.grayScale);
        Image lena = new Raw(256, 256, Encoding.GS, "images\\LENA.RAW");
        ImageIO.write(lena.subtract(GS).toBufferedImage(), "bmp", new File("images\\LENA_subGS.BMP"));
    }

    public static void lenaAddGS() throws IOException{
        Image GS = ImageDrawingUtils.scale(Encoding.GS, ImageDrawingUtils.grayScale);
        Image lena = new Raw(256, 256, Encoding.GS, "images\\LENA.RAW");
        ImageIO.write(lena.add(GS).toBufferedImage(), "bmp", new File("images\\LENA_addGS.BMP"));
    }

    public static void imageOps(String op) throws IOException{
        Image lena = new Raw(256, 256, Encoding.GS, "images\\LENA.RAW");
        Image o = null;
        switch (op){
            case "sum":
                o = lena.add(lena);
                break;
            case "sub":
                o = lena.subtract(lena);
                break;
            case "mul":
                o = lena.product(lena);
                break;
            default:
                throw new IllegalArgumentException();
        }

        ImageIO.write(o.toBufferedImage(), "bmp", new File("images\\LENA_" + op + ".BMP"));


    }

    public static void contrastEnhancement() throws IOException {
        Image lena = new Raw(256, 256, Encoding.GS, "images\\LENA.RAW");
        ImageIO.write(lena.automaticContrastEnhancement().toBufferedImage(), "bmp", new File("images\\LENA_ENHANCED.BMP"));
    }

    public static void negative() throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "images\\BARCO.RAW");
        ImageIO.write(barco.negative().toBufferedImage(), "bmp", new File("images\\BARCO_NEGATIVE.BMP"));
    }

    public static void thresholding(double u) throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "images\\BARCO.RAW");
        ImageIO.write(barco.thresholding(u).toBufferedImage(), "bmp", new File("images\\BARCO_THRESHOLDED.BMP"));
    }

    public static void productWithScalar(double scalar) throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "images\\BARCO.RAW");
        ImageIO.write(barco.scalarProduct(scalar).toBufferedImage(), "bmp", new File("images\\BARCO_PRODUCT_SCALAR.BMP"));
    }

    public static void equalization() throws IOException {
        Image lena = new Raw(256, 256, Encoding.GS, "images\\LENA.RAW");
        ImageIO.write(lena.equalize().toBufferedImage(), "bmp", new File("images\\LENA_EQUALIZATED2.BMP"));
        double[] histogram =  lena.equalize().histogram(0);
        PrintWriter writer = new PrintWriter(new FileWriter("images\\histogram_lena_2.txt"));
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);
        writer.flush();
        writer.close();
    }

    public static void equalizationTest() throws IOException {
        Image lena = new Raw(256, 256, Encoding.GS, "images\\LENA.RAW");

        PrintWriter writer = new PrintWriter(new FileWriter("images\\c2Lena.txt"));

        double[] histogram =  lena.equalizedHistogram(0);
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);
        writer.flush();

        Image aux =  lena.equalize(0);
        histogram = aux.histogram(0);
        ImageIO.write(aux.toBufferedImage(), "bmp", new File("images\\LENA_EQUALIZATED2.BMP"));

        writer = new PrintWriter(new FileWriter("images\\c1Lena.txt"));
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);



        writer.flush();
        writer.close();

    }

    public static void histogram(boolean negative) throws IOException {
        Image barco = new Raw(290, 207, Encoding.GS, "images\\BARCO.RAW");
        double[] histogram = negative ? barco.negative(0).histogram(0) : barco.histogram(0);
        PrintWriter writer = new PrintWriter(new FileWriter("images\\histogram_barco" + (negative ? "_negative.txt" : ".txt")));
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);
        writer.flush();
        writer.close();
    }

    public static void histogramLena() throws IOException {
        Image lena = new Raw(290, 207, Encoding.GS, "images\\LENA.RAW");
        double[] histogram =  lena.histogram(0);
        PrintWriter writer = new PrintWriter(new FileWriter("images\\histogram_lena.txt"));
        for(int i = 0; i < histogram.length; i++)
            writer.println(histogram[i]);
        writer.flush();
        writer.close();
    }



    public static void drawMonochromeCircle() throws IOException {
        Image image = new Image(200,200, Encoding.GS, true);
        ImageDrawingUtils.drawCircle(image, 99,99,50, (x, y) -> new double[]{1,1,1});
        ImageIO.write(image.toBufferedImage(), "bmp", new File("images\\CIRCLE.BMP"));
    }

    public static void drawMonochromeSquare() throws IOException{
        Image image = new Image(200,200, Encoding.GS, true);
        ImageDrawingUtils.drawRectangle(image, 50,50, 150, 150, (x, y) -> new double[]{1,1,1});
        ImageIO.write(image.toBufferedImage(), "bmp", new File("images\\SQUARE.BMP"));
    }

    public static void drawGrayScale() throws IOException{
        ImageIO.write(ImageDrawingUtils.scale(Encoding.GS, ImageDrawingUtils.grayScale).toBufferedImage(),
                "bmp", new File("images\\GRAYSCALE.BMP"));
    }

    public static void selectTEST() throws IOException{
        Image image = new Pgm("images\\TEST.PGM");
        ImageIO.write(image.copy(9,69,53,96).toBufferedImage(),
                "bmp", new File("images\\FASFASFAS.PGM"));
    }

    public static void drawFlashCircle() throws IOException{
        Image image = ImageDrawingUtils.drawCircle(
                ImageDrawingUtils.scale(Encoding.HSV,ImageDrawingUtils.fixedRGB(2, 1)),127,127, 50, ImageDrawingUtils.fixedRGB(1, 1));
        ImageIO.write(image.toBufferedImage(), "bmp", new File("images\\FLASH.BMP"));
    }

    public static void drawCircleWithImage() throws IOException{
        Image barco = new Raw(290, 207, Encoding.GS, "images\\BARCO.RAW");
        ImageDrawingUtils.GradientColour colour = barco::getComponents;
        Image image = ImageDrawingUtils.drawCircle(
                ImageDrawingUtils.scale(Encoding.HSV,ImageDrawingUtils.fixedRGB(2, 1)),127,127, 50, colour);
        ImageIO.write(image.toBufferedImage(), "bmp", new File("images\\USAIHDKSA.BMP"));
    }



}
