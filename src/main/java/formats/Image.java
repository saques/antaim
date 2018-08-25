package formats;


import formats.exceptions.NoSuchComponentException;
import interfaces.TriFunction;
import lombok.Getter;
import utils.MathUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiFunction;

public class Image implements Cloneable{

    @FunctionalInterface
    public interface PixelFunction{
        double apply(double pixel);
    }


    public static final double MAX_D = 1.0;
    public static final int M = 0xFF;
    public static final double U = 1.0/M;


    double[] data;
    @Getter
    int width, height;
    @Getter
    Encoding encoding;

    public Image(int width, int height, Encoding encoding, boolean initData){
        this.width = width;
        this.height = height;
        this.encoding = encoding;
        if(initData)
            data = new double[width*height*encoding.getBands()];
    }

    public Image(String path) throws IOException {
        this(0, 0, Encoding.RGB, false);
        BufferedImage bufferedImage = ImageIO.read(new File(path));
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
        data = new double[width*height*encoding.getBands()];
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int pixel = bufferedImage.getRGB(x, y);
                for(int c = 0; c < encoding.getBands(); c++){
                    setComponent(x, y, c, byteToDouble((byte)(0xFF&pixel>>(8*(encoding.getBands()-c-1)))));
                }
            }
        }
    }

    @Override
    public Image clone(){
        Image image = new Image(width, height, encoding, false);
        image.data = data.clone();
        return image;
    }

    public double getComponent(int x, int y, int component) {
        checkConstraints(x, y, component);
        return data[getIndex(x, y, component, width, encoding)];
    }

    public void setComponent(int x, int y, int component, double value){
        checkConstraints(x, y, component);
        data[getIndex(x, y, component, width, encoding)] = round(value);
    }

    public void setComponents(int x, int y, double[] cmp){
        if(isOutOfBounds(x, y))
            throw new IndexOutOfBoundsException();
        for(int b = 0; b < encoding.getBands(); b++)
            setComponent(x, y, b, cmp[b]);
    }

    public void setComponentsRGB(int x, int y, double[] rgb){
        if(isOutOfBounds(x, y))
            throw new IndexOutOfBoundsException();
        if(rgb.length != 3)
            throw new IllegalArgumentException();
        switch (encoding){
            case RGB:
                setComponents(x, y, rgb);
                break;
            case GS:
                double avg = (rgb[0] + rgb[1] + rgb[2])/3;
                setComponents(x, y, new double[]{avg});
                break;
            case HSV:
                setComponents(x, y, toHSV(rgb[0], rgb[1], rgb[2]));
                break;
        }
    }

    public double[] getComponents(int x, int y){
        if(isOutOfBounds(x, y))
            throw new IndexOutOfBoundsException();
        double[] ans = new double[3];
        switch (encoding){
            case RGB:
            case HSV:
                for(int b = 0; b < encoding.getBands(); b++)
                    ans[b] = getComponent(x, y, b);
                break;
            case GS:
                ans[0] = ans[1] = ans[2] = getComponent(x, y, 0);
            break;
        }
        return ans;
    }

    public double[] avg(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalStateException();
        double[] ans = new double[]{0,0,0};
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                double[] cmp = getComponents(x, y);
                ans[0] += cmp[0]; ans[1] += cmp[1]; ans[2] += cmp[2];
            }
        }
        int tot = width*height;
        ans[0] /= tot; ans[1] /= tot; ans[2] /= tot;
        return ans;
    }

    public double[] histogram(int component){
        checkConstraints(component, Encoding.HSV);
        double[] ans = new double[256];
        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                ans[M & doubleToByte(getComponent(i, j, component))]++;
        return ans;
    }

    public static double dynamicRangeCompression(double r, double R){
        double c = M /Math.log((1+R)*M);
        return (c*Math.log(1+r*M))/M;
    }

    public Image negative(int component){
        checkConstraints(component, Encoding.HSV);
        for(int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                setComponent(i, j, component, negative(getComponent(i, j, component)));
        return this;
    }

    public Image negative(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();
        for(int i = 0; i < encoding.getBands(); i++)
            ans.negative(i);
        return ans;
    }

    public static double negative(double r){
        return byteToDouble((byte)(M - doubleToByte(r)));
    }

    public static void applyAndAdjust(Image i1, Image i2, Image ans, BiFunction<Double, Double, Double> f,
                                      TriFunction<Double, Double, Double, Double> adjust){


        double min[] = new double[i1.encoding.getBands()];
        double max[] = new double[i1.encoding.getBands()];


        for(int i = 0; i < i1.width; i++) {
            for (int j = 0; j < i1.height; j++) {
                for(int c = 0; c < i1.encoding.getBands(); c++){
                    double val = f.apply(i1.getComponent(i, j, c), i2.getComponent(i, j, c));
                    min[c] = Math.min(min[c], val); max[c] = Math.max(max[c], val);
                    ans.setComponent(i, j, c, val);
                }
            }
        }

        for(int i = 0; i < i1.width; i++) {
            for (int j = 0; j < i2.height; j++) {
                for(int c = 0; c < i1.encoding.getBands(); c++){
                    ans.setComponent(i, j, c, adjust.apply(ans.getComponent(i, j, c), min[c], max[c]));
                }
            }
        }

    }

    public boolean isBinaryOperandCompatible(Image o){
        if(!this.encoding.equals(o.encoding) || this.width != o.width || this.height != o.height)
            return false;
        return true;
    }

    public Image add(Image image){
        if(!isBinaryOperandCompatible(image))
            throw new IllegalArgumentException();
        Image ans = clone();

        applyAndAdjust(this, image, ans, (x, y) -> x+y, (c, min, max) -> (c - min)/(max-min));

        return ans;
    }

    public Image subtract(Image image){
        if(!isBinaryOperandCompatible(image))
            throw new IllegalArgumentException();
        Image ans = clone();

        applyAndAdjust(this, image, ans, (x, y) -> x-y, (c, min, max) -> (c - min)/(max-min));

        return ans;
    }

    public Image product(Image image){
        if(!isBinaryOperandCompatible(image))
            throw new IllegalArgumentException();
        Image ans = clone();

        applyAndAdjust(this, image, ans, (x, y) -> x*y, (c, min, max) -> dynamicRangeCompression(c, max));

        return ans;
    }

    public Image thresholding(int component, double u){
        checkConstraints(component, Encoding.HSV);
        for(int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                setComponent(i, j, component, thresholding(getComponent(i, j, component),u));
        return this;
    }

    public Image thresholding(double u){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();
        for(int i = 0; i < encoding.getBands(); i++)
            ans.thresholding(i, u);
        return ans;
    }

    public static double thresholding(double r, double u){
        return r <= u ? 0 : 1 ;
    }

    public Image scalarProduct(int component, double num){
        checkConstraints(component, Encoding.HSV);

        double[] arr = componentsArray(component);

        double max = Arrays.stream(arr).max().getAsDouble() * num;

        for(int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                setComponent(i, j, component, dynamicRangeCompression(getComponent(i, j, component) * num ,max));
        return this;
    }

    public Image scalarProduct(double n){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();
        for(int i = 0; i < encoding.getBands(); i++)
            ans.scalarProduct(i, n);
        return ans;
    }

    public double[] equalizedHistogram(int component){
        checkConstraints(component, Encoding.HSV);
        double[] histogram = histogram(component);

        System.out.println(histogram[0]);
        double[] relativeHisto = relativeHistogram(histogram, Arrays.stream(histogram).sum());
        return equalizedHistogram(histogram,relativeHisto);
    }

    public Image equalize(int component){
        checkConstraints(component, Encoding.HSV);

        double[] histogram = this.histogram(component);

        double[] relativeHisto = relativeHistogram(histogram, Arrays.stream(histogram).sum());

        double[] transf = equalizedTransformation(histogram,relativeHisto);


        for(int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                setComponent(i, j, component, transf[M & doubleToByte(getComponent(i, j, component))] / 255);
        return this;
    }

    public Image equalize(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();
        for(int i = 0; i < encoding.getBands(); i++)
            ans.equalize(i);
        return ans;
    }

    public static double[] equalizedHistogram(double [] h , double [] s ){
        double [] ans = new double[s.length];

        for (int i = 0 ; i < s.length ; i++){
            ans[(int)Math.floor( ( ((s[i] - s[0]) / (1 - s[0]) ) ) * M)] +=  h[i];
        }
        return ans;
    }

    public static double[] equalizedTransformation(double [] h , double [] s ){
        double [] ans = new double[s.length];
        for (int i = 0 ; i < s.length ; i++){
            ans[i] =  (int)Math.floor( ( ((s[i] - s[0]) / (1 - s[0]) ) ) * M);
        }
        return ans;
    }

    public double[] relativeHistogram(double [] h , double n){
        if (h == null)
            throw new IllegalStateException("Histogram can't be null");

        double [] ans = new double[h.length];
        ans[0] = h[0] ;

        for (int i = 1 ; i < h.length; i++){
            ans[i] = ans[i-1] + h[i];
        }

        ans = Arrays.stream(ans).map( x -> x / n).toArray();
        return ans;
    }


    public Image automaticContrastEnhancement(int component){
        checkConstraints(component, Encoding.HSV);
        PixelFunction function = constrastEnhancementFunction(component);

        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                setComponent(i, j, component, function.apply(getComponent(i, j, component)));
        return this;
    }

    public Image automaticContrastEnhancement(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();
        for(int i = 0; i < encoding.getBands(); i++)
            ans.automaticContrastEnhancement(i);
        return ans;
    }

    private double[] componentsArray(int component){
        double[] ans = new double[data.length/encoding.getBands()];
        int count = 0;
        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                ans[count++] = getComponent(i, j, component);
        return ans;
    }

    private PixelFunction constrastEnhancementFunction(int component){
        double[] arr = componentsArray(component);

        double avg = MathUtils.avg(arr);
        double std = MathUtils.std(arr, avg);

        double r1 = computeR1(avg, std), r2 = computeR2(avg, std);
        double s1 = r1/2, s2 = 1 - r2/2;
        double p1 = s1/r1, p2 = (s2-s1)/(r2-r1), p3 = (1-s2)/(1-r2);

        return (x) -> x < r1 ? x*p1 : (x < r2 ? x*p2 + (s1-p2*r1) : x*p3 + (s2-p3*r2));
    }

    private static double computeR1(double avg, double std){
        double r1;
        double div = 2;
        do {
            r1 = avg - std/div;
            div ++;
        } while (r1 <= 0);
        return r1;
    }

    private static double computeR2(double avg, double std){
        double r2;
        double div = 2;
        do {
            r2 = avg + std/div;
            div ++;
        } while (r2 >= 1);
        return r2;
    }


    public static double[] toHSV(double r, double g, double b){
        double h = 0, s , v ;

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        v = max;

        double delta = max - min;

        s = (max!=0) ? (delta/max) : 0.0;

        if(Double.compare(s, 0.0) == 0)
            h = Double.NaN;
        else {
            if(r == max)
                h = (g-b)/delta;
            else if (g == max)
                h = 2.0 + (b-r)/delta;
            else if (b == max)
                h = 4.0 + (r-g)/delta;
            h = h * 60.0;
            if(h < 0)
                h = h + 360.0;
        }

        return new double[]{h, s, v};
    }

    public static double[] toRGB(double h, double s, double v){
        double r = 0, g = 0, b = 0;

        double f,p,q,t;
        int i;

        if(s == 0){ //Achromatic case
            if(Double.isNaN(h)){
                r = g = b = v;
            } else {
                throw new IllegalStateException();
            }
        } else { //Chromatic case
            if(Double.compare(h, 360.0) == 0)
                h = 0;
            h /= 60.0;
            i = (int)Math.floor(h);
            f = h - i;
            p = v * (1-s);
            q = v * (1 - (s * f));
            t = v * (1 - (s * (1 - f)));

            switch (i){
                case 0:
                    r=v; g=t; b=p;
                    break;
                case 1:
                    r=q; g=v; b=p;
                    break;
                case 2:
                    r=p; g=v; b=t;
                    break;
                case 3:
                    r=p; g=q; b=v;
                    break;
                case 4:
                    r=t; g=p; b=v;
                    break;
                case 5:
                    r=v; g=p; b=q;
                    break;
            }

        }

        return new double[]{r, g, b};
    }

    public Image toHSV(){
        switch (encoding){
            case RGB:
                for(int x = 0; x < width; x++){
                    for(int y = 0; y < height; y++){
                        double[] rgb = getComponents(x, y);
                        double[] hsv = toHSV(rgb[0], rgb[1], rgb[2]);
                        setComponents(x, y, hsv);
                    }
                }
                encoding = Encoding.HSV;
                break;
            case GS:
                double[] ans = new double[width*height*Encoding.HSV.getBands()];
                for(int x = 0; x < width; x++){
                    for(int y = 0; y < height; y++){
                        double[] rgb = getComponents(x, y);
                        double[] hsv = toHSV(rgb[0], rgb[0], rgb[0]);
                        for(int b = 0; b < Encoding.HSV.getBands(); b++){
                            ans[getIndex(x, y, b, width, Encoding.HSV)] = hsv[b];
                        }
                    }
                }
                data = ans;
                encoding = Encoding.HSV;
                break;
            case HSV:
                break;
        }
        return this;
    }

    public Image toRGB(){
        switch (encoding){
            case RGB:
                break;
            case GS:
                double[] ans = new double[width*height*Encoding.RGB.getBands()];
                for(int x = 0; x < width; x++){
                    for(int y = 0; y < height; y++){
                        double[] gs = getComponents(x, y);
                        double[] rgb = {gs[0], gs[0], gs[0]};
                        for(int b = 0; b < Encoding.HSV.getBands(); b++){
                            ans[getIndex(x, y, b, width, Encoding.RGB)] = rgb[b];
                        }
                    }
                }
                data = ans;
                encoding = Encoding.RGB;
                break;
            case HSV:
                for(int x = 0; x < width; x++){
                    for(int y = 0; y < width; y++){
                        double[] hsv = getComponents(x, y);
                        double[] rgb = toRGB(hsv[0], hsv[1], hsv[2]);
                        setComponents(x, y, rgb);
                    }
                }
                encoding = Encoding.RGB;
                break;
        }
        return this;
    }

    public Image copy(int x1, int y1, int x2, int y2){
        if(isOutOfBounds(x1, y1) || isOutOfBounds(x2, y2))
            throw new IndexOutOfBoundsException();
        if(x1 >= x2 || y1 >= y2)
            throw new IllegalArgumentException("Point 1 must be lower than point 2");

        int nWidth = (x2-x1), nHeight = (y2-y1);
        Image image = new Image(nWidth, nHeight, encoding, true);
        for(int x = x1; x < x2; x++)
            for(int y = y1; y < y2; y++)
                for(int b = 0; b < encoding.getBands(); b++)
                    image.data[getIndex(x-x1, y-y1, b, nWidth, encoding)] = d(x, y, b);
        return image;
    }

    public BufferedImage toBufferedImage(){
        BufferedImage image = new BufferedImage(width, height, encoding.getBufferedImageType());
        for(int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, getIntPixel(x, y));
            }
        }
        return image;
    }

    private int getIntPixel(int x, int y){
        int ans = 0;
        switch (encoding){
            case RGB:
                ans = ((M& b(x,y,0)) << 16) | ((M& b(x,y,1)) << 8) | (M& b(x,y,2));
                break;
            case GS:
                ans = ((M& b(x,y,0)) << 16) | ((M& b(x,y,0)) << 8) | (M& b(x,y,0));
                break;
            case HSV:
                double[] rgb = toRGB(d(x, y, 0), d(x, y, 1), d(x, y, 2));
                ans = ((M & doubleToByte(rgb[0])) << 16) | ((M& doubleToByte(rgb[1])) << 8) | (M& doubleToByte(rgb[2]));
                break;
        }
        return ans;
    }


    public boolean isOutOfBounds(int x, int y){
        return x < 0 || y < 0 || x >= width || y >= height;
    }

    private double d(int x, int y, int component){
        return data[getIndex(x,y,component, width, encoding)];
    }

    private byte b(int x, int y, int component){
        return doubleToByte(d(x, y, component));
    }

    private void checkConstraints(int x, int y, int component){
        if(component >= encoding.getBands())
            throw new NoSuchComponentException(component, encoding);
        if(isOutOfBounds(x, y))
            throw new IndexOutOfBoundsException();
    }

    private void checkConstraints(int component, Encoding... encodings){
        if(Arrays.stream(encodings).anyMatch(x -> x == encoding))
            throw new IllegalStateException();
        if(component >= encoding.getBands())
            throw new IllegalArgumentException();
    }

    private static int getIndex(int x, int y, int component, int width, Encoding encoding){
        return (x + y*width)*encoding.getBands() + component;
    }

    public static double byteToDouble(byte b){
        return (0xFF&b)*U;
    }

    public static byte doubleToByte(double b){
        return (byte)(b/U);
    }

    public static double round(double d){
        return Math.floor(d/U)*U;
    }

}
