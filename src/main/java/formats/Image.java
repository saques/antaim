package formats;


import com.sun.imageio.plugins.common.BitFile;
import formats.exceptions.NoSuchComponentException;
import interfaces.FigureMode;
import interfaces.TriFunction;
import lombok.Getter;
import noise.NoiseApplyMode;
import noise.NoiseGenerator;
import structures.Vector3D;
import utils.ImageDrawingUtils;
import utils.MathUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class Image implements Cloneable{

    @FunctionalInterface
    public interface PixelFunction{
        double apply(double pixel);
    }


    public static final double MAX_D = 1.0;
    public static final int M = 0xFF;
    public static final double U = 1.0/M;
    public static final double SQRT_3 = Math.sqrt(3);

    private static final TriFunction<Double, Double, Double, Double> linearAdjust = (c, min, max) -> (c - min)/(max-min);
    private static final BiFunction<Double, Double, Double> modulus = (x, y) -> Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    private static final BiFunction<Double, Double, Double> sumOfModulus = (x, y) -> Math.abs(x) + Math.abs(y);
    private static final BiFunction<Double, Double, Double> sum = (x, y) -> x + y ;

    private static int IDs = 0;

    private int id;

    double[] data;
    @Getter
    int width, height;
    @Getter
    Encoding encoding;

    public Image(int width, int height, Encoding encoding, boolean initData){
        id = IDs ++;
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

    public int id(){
        return id;
    }

    public double getComponent(int x, int y, int component) {
        checkConstraints(x, y, component);
        return data[getIndex(x, y, component, width, encoding)];
    }

    public void setComponent(int x, int y, int component, double value){
        checkConstraints(x, y, component);
        data[getIndex(x, y, component, width, encoding)] = round(value);
    }

    public void setComponentNoRound(int x, int y, int component, double value){
        checkConstraints(x, y, component);
        data[getIndex(x, y, component, width, encoding)] = value;
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

    public Image dynamicRangeCompression(int component){
        checkConstraints(component, Encoding.HSV);

        double max = Double.MIN_VALUE;

        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                max = Math.max(max, getComponent(i, j, component));

        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                setComponent(i, j, component, dynamicRangeCompression(getComponent(i, j, component), max));

        return this;
    }

    public Image dynamicRangeCompression(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();

        for(int c = 0; c < encoding.getBands(); c++)
            ans.dynamicRangeCompression(c);

        return ans;
    }

    public static void apply(Image i1, Image i2, Image ans, BiFunction<Double, Double, Double> f, boolean round){
        for(int i = 0; i < i1.width; i++) {
            for (int j = 0; j < i1.height; j++) {
                for(int c = 0; c < i1.encoding.getBands(); c++){
                    double val = f.apply(i1.getComponent(i, j, c), i2.getComponent(i, j, c));
                    if(round)
                        ans.setComponent(i, j, c, val);
                    else
                        ans.setComponentNoRound(i, j, c, val);
                }
            }
        }

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
                    ans.setComponentNoRound(i, j, c, val);
                }
            }
        }

        if(adjust == null)
            return;

        for(int i = 0; i < i1.width; i++) {
            for (int j = 0; j < i1.height; j++) {
                for(int c = 0; c < i1.encoding.getBands(); c++){
                    ans.setComponent(i, j, c, adjust.apply(ans.getComponent(i, j, c), min[c], max[c]));
                }
            }
        }

    }

    private Image adjust(TriFunction<Double, Double, Double, Double> adjust, double[] max, double[] min){
        for(int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for(int c = 0; c < encoding.getBands(); c++){
                    setComponent(i, j, c, adjust.apply(getComponent(i, j, c), min[c], max[c]));
                }
            }
        }
        return this;
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

        applyAndAdjust(this, image, ans, (x, y) -> x+y, linearAdjust);

        return ans;
    }

    public Image subtract(Image image){
        if(!isBinaryOperandCompatible(image))
            throw new IllegalArgumentException();
        Image ans = clone();

        applyAndAdjust(this, image, ans, (x, y) -> x-y, linearAdjust);

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


    public double[] maxAndMin(int component){
        checkConstraints(component, Encoding.HSV);
        double max = Double.MIN_VALUE, min = Double.MAX_VALUE;

        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++){
                max = Math.max(max, getComponent(i, j, component));
                min = Math.min(min, getComponent(i, j, component));
            }

        return new double[]{max, min};
    }


    private double automaticThreshold(int component) {
        checkConstraints(component, Encoding.HSV);

        double[] maxMin = maxAndMin(component);
        double t = round((maxMin[0]+maxMin[1])/2);

        boolean exit = false;
        while(!exit) {
            exit = true;
            double m1 = 0, m2 = 0;
            int p1 = 0, p2 = 0;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    double pixel = getComponent(x, y, component);
                    if(pixel < t){
                        m1 += pixel;
                        p1++;
                    } else {
                        m2 += pixel;
                        p2 ++;
                    }
                }
            }
            m1 /= p1;
            m2 /= p2;
            double nT = (m1+m2)/2;
            if(Math.abs(t-nT) > U)
                exit = false;
            t = nT;
        }

        return t;
    }

    public Image automaticThresholding(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();
        for(int i = 0; i < encoding.getBands(); i++)
            ans.thresholding(i, automaticThreshold(i));
        return ans;
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


    public Image gammaCorrection(double g){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();
        for(int i = 0; i < encoding.getBands(); i++)
            ans.gammaCorrection(i, g);
        return ans;
    }


    public Image gammaCorrection(int component, double g){
        checkConstraints(component, Encoding.HSV);

        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                        double val = (Math.pow(M,1-g) * Math.pow(getComponent(i, j, component) * M,g))/255;
                        min = Math.min(min, val);
                        max = Math.max(max, val);
                        setComponentNoRound(i, j, component, val);
            }
        }

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++) {
                    double val = getComponent(i, j, component);
                    val = (val - min) / (max - min);
                    setComponent(i, j, component, val);
            }
        }

        return this;

    }

    public double[] equalizedHistogram(int component){
        checkConstraints(component, Encoding.HSV);
        double[] histogram = histogram(component);

        System.out.println(histogram[0]);
        double[] relativeHisto = cummulativeRelativeHistogram(histogram, Arrays.stream(histogram).sum());
        return equalizedHistogram(histogram,relativeHisto);
    }

    public Image equalize(int component){
        checkConstraints(component, Encoding.HSV);

        double[] histogram = this.histogram(component);

        double[] relativeHisto = cummulativeRelativeHistogram(histogram, Arrays.stream(histogram).sum());

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

    public double[] cummulativeRelativeHistogram(double [] h , double n){
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


    private double[] cummulativeMean(double[] relativeHistogram) {
        if (relativeHistogram == null)
            throw new IllegalStateException("Histogram can't be null");

        double [] ans = new double[relativeHistogram.length];

        ans[0] = 0;

        for (int i = 1 ; i < relativeHistogram.length; i++){
            ans[i] = ans[i-1] + i * relativeHistogram[i];
        }
        return ans;
    }


    private double[] classVariance(double[] cumMean, double[] cumRelHisto) {
        if (cumMean == null || cumRelHisto == null)
            throw new IllegalStateException("Histogram can't be null");

        double [] ans = new double[cumMean.length];

        for (int i = 0 ; i < cumMean.length; i++){
            if (cumRelHisto[i] == 1)
                ans[i] = 0;
            else
                ans[i] = Math.pow((cumMean[cumMean.length - 1] * cumRelHisto[i]) - cumMean[i],2) / (cumRelHisto[i] * ( 1 - cumRelHisto[i]));
            //System.out.println(ans[i] + " "+i);
        }
        return ans;
    }

    private double getOtsuThreshold(double[] classVariance) {
         List<Integer> maximums = new ArrayList<Integer>();
         double maxVal = Double.MIN_VALUE;
         for (int i = 0; i < classVariance.length ; i++){
             if (classVariance[i] > maxVal) {
                 maxVal = classVariance[i];
                 maximums.clear();
                 maximums.add(i);
             } else if (classVariance[i] == maxVal){
                 maximums.add(i);
             }

         }
         return maximums.stream().mapToInt(a -> a).average().getAsDouble()/(double)M;
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

    public Image toGS(){
        if(encoding.equals(Encoding.GS))
            return clone();
        Image ans = new Image(width, height, Encoding.GS, true);
        switch (encoding){
            case RGB:
                for(int x = 0; x < width; x++){
                    for(int y = 0; y < height; y++){

                        double r = getComponent(x, y, 0);
                        double g = getComponent(x, y, 1);
                        double b = getComponent(x, y, 2);

                        double csrgb = 0.2126*r + 0.7152*g + 0.0722*b;

                        if(csrgb <= 0.0031308){
                            csrgb *= 12.92;
                        } else {
                            csrgb = 1.055 * Math.pow(csrgb, 1.0/2.4) - 0.055;
                        }

                        ans.setComponent(x, y, 0, csrgb);
                    }
                }
                break;
            case GS:
                break;
            case HSV:
                for(int x = 0; x < width; x++){
                    for(int y = 0; y < height; y++){
                        //V
                        ans.setComponent(x, y, 0, getComponent(x, y, 2));
                    }
                }
                break;
        }
        return ans;
    }

    public Image contaminate(double density, NoiseGenerator generator, NoiseApplyMode mode){
        if(encoding.equals(Encoding.HSV) || density < 0 || density > 1)
            throw new IllegalArgumentException();

        double[] min = new double[encoding.getBands()], max = new double[encoding.getBands()];

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                if(Math.random() < density){
                    double noise = generator.nextVal();
                    for(int c = 0 ; c < encoding.getBands(); c++) {
                        double val = getComponent(i, j, c);
                        switch (mode) {
                            case ADDITIVE:
                                val += noise;
                                break;
                            case MULTIPLICATIVE:
                                val *= noise;
                                break;
                            case DESTRUCTIVE:
                                val = noise == -1 ? val : noise;
                                break;
                        }
                        min[c] = Math.min(min[c], val);
                        max[c] = Math.max(max[c], val);
                        setComponentNoRound(i, j, c, val);
                    }
                }
            }
        }

        if(!mode.equals(NoiseApplyMode.DESTRUCTIVE)){
            for(int i = 0; i < width; i++){
                for(int j = 0; j < height; j++) {
                    for(int c = 0; c < encoding.getBands(); c++) {
                        double val = getComponent(i, j, c);
                        if(val < 0 || val > 1)
                            val = (val - min[c]) / (max[c] - min[c]);
                        setComponent(i, j, c, val);
                    }
                }
            }
        }

        return this;
    }

    public Image contaminateO(double density, NoiseGenerator generator, NoiseApplyMode mode){
        Image ans = this.clone();
        ans.contaminate(density, generator, mode);
        return ans;
    }


    private class ImageMaxMin {
        Image image;
        double[] max;
        double[] min;
        ImageMaxMin(Image image, double max[], double min[]){
            this.image = image;
            this.max = max;
            this.min = min;
        }
    }

    private static class ConvolutionParameters {
        double[][] mask;
        boolean round;
        double divisor;
        public ConvolutionParameters(double[][] mask, boolean round, double divisor){
            this.mask = mask;
            this.round = round;
            this.divisor = divisor;
        }
    }


    private List<ImageMaxMin> convolution(ConvolutionParameters... params){
        if (encoding.equals(Encoding.HSV)) {
            throw new IllegalArgumentException();
        }

        List<ImageMaxMin> ans = new ArrayList<>(params.length);

        for(int i = 0; i < params.length; i++){
            double[] max = new double[encoding.getBands()], min = new double[encoding.getBands()];
            for (int c = 0; c < encoding.getBands(); c++){
                max[c] = Double.MIN_VALUE;
                min[c] = Double.MAX_VALUE;
            }
            ans.add(new ImageMaxMin(clone(), max, min));
        }

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int c = 0; c < encoding.getBands(); c++){
                    for(int op = 0; op < params.length; op++) {

                        ConvolutionParameters param = params[op];
                        ImageMaxMin im = ans.get(op);
                        int d = param.mask.length/2;

                        double accum = 0;
                        for (int x = i - d; x <= i + d; x++) {
                            for (int y = j - d; y <= j + d; y++) {
                                accum += param.mask[x - i + d][y - j + d] * getComponent(Math.floorMod(x, width), Math.floorMod(y, height), c);
                            }
                        }
                        accum /= param.divisor;
                        im.max[c] = Math.max(im.max[c], accum);
                        im.min[c] = Math.min(im.min[c], accum);
                        if (param.round)
                            im.image.setComponent(i, j, c, accum);
                        else
                            im.image.setComponentNoRound(i, j, c, accum);
                    }
                }
            }
        }
        return ans;
    }

    public Image genericConvolution(double[][] MASK){
        if (encoding.equals(Encoding.HSV) || MASK.length == 0 || MASK.length != MASK[0].length)
            throw new IllegalArgumentException();

        List<ImageMaxMin> ans = convolution(new ConvolutionParameters(MASK, false, 1));

        ImageMaxMin imageMaxMin = ans.get(0);

        return imageMaxMin.image.adjust(linearAdjust, imageMaxMin.max, imageMaxMin.min);
    }


    private Image medianMask(int[][] MASK){
        if((MASK.length % 2) == 0 || MASK[0].length != MASK.length|| encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        Image ans = clone();
        int d = MASK.length/2;

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int c = 0; c < encoding.getBands(); c++){
                    List<Double> list = new LinkedList<>();
                    for(int x = i - d; x <= i + d; x ++){
                        for(int y = j - d; y <= j + d; y++){
                            if(!isOutOfBounds(x, y))
                                for (int t = 0; t < MASK[x + d - i][y + d - j]; t++)
                                    list.add(getComponent(x, y, c));
                        }
                    }
                    ans.setComponent(i, j, c, MathUtils.median(list));
                }
            }
        }

        return ans;
    }


    public Image medianFilter(int n){
        if((n % 2) == 0 || encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        int[][] MASK = new int[n][n];
        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++)
                MASK[i][j] = 1;

        return medianMask(MASK);
    }

    public Image weightedMedianFilter(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        int[][] WEIGHTED_MEDIAN_MATRIX = {{1, 2, 1},
                                          {2, 4, 2},
                                          {1, 2, 1}};

        return medianMask(WEIGHTED_MEDIAN_MATRIX);
    }

    public Image contourEnhancement(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        double[][] MASK = {{-1.0, -1.0, -1.0},
                           {-1.0,  8.0, -1.0},
                           {-1.0, -1.0, -1.0}};

        return convolution(new ConvolutionParameters(MASK, true, 9.0)).get(0).image;
    }


    public Image meanFilter(int n){
        if((n % 2) == 0 || encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        double[][] MASK = new double[n][n];
        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++)
                MASK[i][j] = 1.0;

        return convolution(new ConvolutionParameters(MASK, true, (double)(n*n))).get(0).image;

    }

    private static ConvolutionParameters gaussMask(int n, double sigma, boolean round){
        int d = n/2;
        BiFunction <Integer, Integer ,Double > gauss = ( x , y ) -> ( 1 / (2 * Math.PI * Math.pow(sigma,2))) * Math.exp( (- (Math.pow(x,2) + Math.pow(y,2)) ) / ( 2 * Math.pow(sigma,2)) );
        double [][] MASK = new double [n][n];
        double divisor = 0;
        for ( int j = 0  ; j < n ; j++){
            for (int k = 0 ; k < n ; k++ ){
                MASK[j][k] = gauss.apply(j - d, k - d);
                divisor += MASK[j][k];
            }
        }
        return new ConvolutionParameters(MASK, round, divisor);
    }

    public Image gaussFilter(int n , double sigma){
        if((n % 2) == 0 || encoding.equals(Encoding.HSV) || sigma < 0)
            throw new IllegalArgumentException();

        return convolution(gaussMask(n, sigma, true)).get(0).image;

    }

    public Image canny(double t1, double t2){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        Image gs = toGS();

        /**
         * GAUSS FILTERS
         */
        List<ImageMaxMin> gauss = gs.convolution(gaussMask(3, 1, true));

        Image gauss1 = gauss.get(0).image;

        /**
         * SOBEL EDGE DETECTOR
         */

        double[][] MASK_DX = {{-1.0, 0.0, 1.0},
                              {-2.0, 0.0, 2.0},
                              {-1.0, 0.0, 1.0}};

        double[][] MASK_DY = {{-1.0, -2.0, -1.0},
                              { 0.0,  0.0,  0.0},
                              { 1.0,  2.0,  1.0}};

        ConvolutionParameters dx = new ConvolutionParameters(MASK_DX, false, 1),
                              dy = new ConvolutionParameters(MASK_DY, false, 1);

        List<ImageMaxMin> gauss1sobel = gauss1.convolution(dx, dy);

        Image gauss1sobelNoMaxSuppr = cannySobelModulusAngleNoMaxSuppr(gauss1sobel, width, height);

        return histeresisThreshold(gauss1sobelNoMaxSuppr, t1, t2);
    }

    private static Image cannySobelModulusAngleNoMaxSuppr(List<ImageMaxMin> dxdy, int width, int height){
        ImageMaxMin dx = dxdy.get(0), dy = dxdy.get(1);

        Image mod = new Image(width, height, Encoding.GS, true);

        applyAndAdjust(dx.image, dy.image, mod, modulus, linearAdjust);

        mod = mod.dynamicRangeCompression();

        Image ans = new Image(width, height, Encoding.GS, true);

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int c = 0; c < Encoding.GS.getBands(); c++){

                    double x = dx.image.getComponent(i, j, c);
                    double y = dy.image.getComponent(i, j, c);


                    double ang =  (Math.toDegrees(y != 0 ? Math.atan(x/y) : Math.PI/2) + 180) % 180;

                    double pix = mod.getComponent(i, j, c);

                    double l, r;

                    if(ang >= 67.5 && ang < 112.5){
                        l = mod.getComponent(i, Math.floorMod(j-1, height), c);
                        r = mod.getComponent(i, Math.floorMod(j+1, height), c);
                    } else if ((ang>= 0 && ang < 22.5) || (ang >= 157.5 && ang <= 180)){
                        l = mod.getComponent(Math.floorMod(i-1, width), j, c);
                        r = mod.getComponent(Math.floorMod(i+1, width), j, c);
                    } else if (ang >= 22.5 && ang < 67.5) {
                        l = mod.getComponent(Math.floorMod(i-1, width), Math.floorMod(j-1, height), c);
                        r = mod.getComponent(Math.floorMod(i+1, width), Math.floorMod(j+1, height), c);
                    } else {
                        l = mod.getComponent(Math.floorMod(i+1, width), Math.floorMod(j-1, height), c);
                        r = mod.getComponent(Math.floorMod(i-1, width), Math.floorMod(j+1, height), c);
                    }

                    double npix = 0;

                    if(pix > 0 && pix >= l && pix >= r)
                        npix = pix;

                    ans.setComponent(i, j, c, npix);
                }
            }
        }

        return ans;
    }

    private static Image histeresisThreshold(Image img, double t1, double t2){
        Image ans = new Image(img.width, img.height, img.encoding, true);

        for(int i = 0; i < img.width; i++){
            for(int j = 0; j < img.height; j++){
                for(int c = 0; c < img.encoding.getBands(); c++){
                    double val = img.getComponent(i, j, c);

                    double res = 0;

                    if(val >= t2){
                        //val is strong border
                        res = 1;
                    }
                    ans.setComponent(i, j, c, res);

                }
            }
        }


        for(int i = 0; i < img.width; i++){
            for(int j = 0; j < img.height; j++){
                for(int c = 0; c < img.encoding.getBands(); c++){
                    double val = img.getComponent(i, j, c);
                    if(val >= t1 && val < t2) {

                        for (int x = i - 1; x <= i + 1; x++) {
                            for (int y = j - 1; y <= j + 1; y++) {

                                if (x != i && y != j && !ans.isOutOfBounds(x, y) && ans.getComponent(x, y, c) >= t2) {
                                    ans.setComponent(i, j, c, 1);
                                    break;
                                }

                            }
                        }
                    }
                }
            }
        }

        return ans;
    }

    private Image firstDerivativeContourEnhancement(double[][] MASK_DX, double[][] MASK_DY){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        List<ImageMaxMin> convoluted = convolution(new ConvolutionParameters(MASK_DX, false, 1),
                                                   new ConvolutionParameters(MASK_DY, false, 1));

        ImageMaxMin dx = convoluted.get(0);
        ImageMaxMin dy = convoluted.get(1);

        Image ans = new Image(width, height, encoding, true);

        applyAndAdjust(dx.image, dy.image, ans, modulus, linearAdjust);

        return ans;
    }

    public Image prewitt(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        double[][] MASK_DX = {{-1.0, 0.0, 1.0},
                              {-1.0, 0.0, 1.0},
                              {-1.0, 0.0, 1.0}};

        double[][] MASK_DY = {{-1.0, -1.0, -1.0},
                              { 0.0,  0.0,  0.0},
                              { 1.0,  1.0,  1.0}};

        return firstDerivativeContourEnhancement(MASK_DX, MASK_DY);
    }

    public Image sobel(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        double[][] MASK_DX = {{-1.0, 0.0, 1.0},
                              {-2.0, 0.0, 2.0},
                              {-1.0, 0.0, 1.0}};

        double[][] MASK_DY = {{-1.0, -2.0, -1.0},
                              { 0.0,  0.0,  0.0},
                              { 1.0,  2.0,  1.0}};


        return firstDerivativeContourEnhancement(MASK_DX, MASK_DY);
    }

    public enum DiffusionBorderDetector {
        LECLERC((s, d) -> Math.exp(-1.0*Math.pow(d/s, 2))),
        LORENTZ((s, d) -> 1.0/(1+(Math.pow(d/s, 2)))),
        ISOTROPIC((s, d) -> 1.0);

        private BiFunction<Double, Double, Double> function;
        DiffusionBorderDetector(BiFunction<Double, Double, Double> function){
            this.function = function;
        }
    }

    public Image diffusion(int t, double sigma, DiffusionBorderDetector detector){
        if(t <= 0 || encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        Image ans = clone();

        while (t > 0){

            Image next = new Image(width, height, encoding, true);

            for(int i = 0; i < width; i++){
                for(int j = 0; j < height; j++){
                    for(int c = 0; c < encoding.getBands(); c++){

                        double v = ans.getComponent(i, j, c);

                        double vn = ans.getComponent(Math.floorMod(i+1, width), j, c) - v;
                        double vs = ans.getComponent(Math.floorMod(i-1, width), j, c) - v;
                        double ve = ans.getComponent(i, Math.floorMod(j-1, height), c) - v;
                        double vw = ans.getComponent(i, Math.floorMod(j+1, height), c) - v;

                        vn *= detector.function.apply(sigma, vn);
                        vs *= detector.function.apply(sigma, vs);
                        ve *= detector.function.apply(sigma, ve);
                        vw *= detector.function.apply(sigma, vw);

                        double vdef = v + 0.25*(vn + vs + ve + vw);

                        if(t == 1)
                            next.setComponent(i, j, c, vdef);
                        else
                            next.setComponentNoRound(i, j, c, vdef);
                    }
                }
            }

            ans = next;

            t--;
        }

        return ans;
    }




    public Image loGFilter(int n, int sigma, double threshold){
        if((n % 2) == 0 || encoding.equals(Encoding.HSV) || sigma < 0 || threshold < 0)
            throw new IllegalArgumentException();

        int d = n/2;
        BiFunction <Integer, Integer ,Double > loG = ( x , y ) -> (-1.0 / ((Math.sqrt(2.0 * Math.PI) * Math.pow(sigma,3)))  ) * ( 2 - ( (Math.pow(x,2)+ Math.pow(y,2) )  / Math.pow(sigma,2) ) ) * Math.exp( (-1.0/2.0) * ( (Math.pow(x,2)+ Math.pow(y,2) )  / Math.pow(sigma,2) ) ) ;
        double [][] MASK = new double [n][n];
        double divisor = 0;
        for ( int j = 0  ; j < n ; j++){
            for (int k = 0 ; k < n ; k++ ){
                MASK[j][k] = loG.apply(j - d, k - d);
                //divisor += MASK[j][k];
            }
        }

        return convolution(new ConvolutionParameters(MASK, false, 1)).get(0).image.zeroCrossing(moreThanThreshold(threshold));

    }


    public Image laplace(){
        if( encoding.equals(Encoding.HSV) )
            throw new IllegalArgumentException();

        double[][] LAPLACE_MASK = {{0.0, -1.0, 0.0},
                            {-1.0, 4.0, -1.0},
                                {0.0, -1.0, 0.0}};

        return convolution(new ConvolutionParameters(LAPLACE_MASK, false, 1.0)).get(0).image.zeroCrossing((x,y) -> 0.0);

    }



    private Image zeroCrossing(BiFunction<Double,Double,Double> function){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();

        Image ans = clone();
        for(int c = 0; c < encoding.getBands(); c++){
            for ( int x = 0  ; x < width ; x++){
                for (int  y= 0 ; y < height ; y++ ){
                    ans.setComponent(x,y,c,hasChangedSign(x,y,c,function));
                }
            }
        }
        return ans;
    }


    private double hasChangedSign(int x, int y, int component, BiFunction<Double,Double,Double> function) {
        if (getComponent(x,y,component) == 0){
            if (!isOutOfBounds(x-1,y) && !isOutOfBounds(x+1,y)){
                return getComponent(x-1,y,component) * getComponent(x+1,y,component) < 0.0 ? function.apply(getComponent(x-1,y,component) , getComponent(x+1,y,component)) : MAX_D;
            }
            if (!isOutOfBounds(x,y-1) && !isOutOfBounds(x,y+1)){
                return getComponent(x,y-1,component) * getComponent(x,y+1,component) < 0.0 ? function.apply(getComponent(x,y-1,component) , getComponent(x,y+1,component) ): MAX_D;
            }
            return MAX_D;
        } else{
            if (!isOutOfBounds(x+1,y)){
                return getComponent(x,y,component) * getComponent(x+1,y,component) < 0.0 ? function.apply(getComponent(x,y,component) , getComponent(x+1,y,component) ): MAX_D;
            }
            if (!isOutOfBounds(x,y+1)){
                return getComponent(x,y,component) * getComponent(x,y+1,component) < 0.0 ? function.apply(getComponent(x,y,component) , getComponent(x,y+1,component)) : MAX_D;
            }
            return MAX_D;

        }

    }

    private BiFunction<Double,Double,Double> moreThanThreshold( double threshold) {
        return (component,component1) ->(Math.abs(component) + Math.abs(component1)) > threshold ?  0.0 : MAX_D;
    }


    public Image bilateralFilter(int n , int sigmaS, int sigmaR){
        if((n % 2) == 0 || encoding.equals(Encoding.HSV) || sigmaS < 0 || sigmaR < 0)
            throw new IllegalArgumentException();

        int d = n/2;
        BiFunction <Integer, Integer ,Function<Double,Double >> bilateral = ( x , y ) -> ( module ) -> ( Math.exp( (((-1) * (Math.pow(x,2) + Math.pow(y,2))) / (2 * Math.pow(sigmaS,2))) - ( module / (2* Math.pow(sigmaR,2)) )  ));
        ArrayList<ArrayList<Function<Double,Double >>>  MASK = new ArrayList<>();

        for ( int j = 0  ; j < n ; j++){
            MASK.add(j,new ArrayList<>());
        }
        for ( int j = 0  ; j < n ; j++){
            for (int k = 0 ; k < n ; k++ ){
                MASK.get(j).add(k,bilateral.apply(j - d, k - d));
            }
        }

        Image ans = clone();



        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                double module[][] = new double[n][n];
                for (int x = i - d; x <= i + d; x++) {
                    for (int y = j - d; y <= j + d; y++) {
                        for(int c = 0; c < encoding.getBands(); c++) {
                            if(!isOutOfBounds(x,y))
                                module[x - i + d][y - j + d] += Math.pow((getComponent(i,j,c) - getComponent(Math.floorMod(x, width), Math.floorMod(y, height), c)) , 2);
                        }
                    }
                }

                for(int c = 0; c < encoding.getBands(); c++) {
                    double accum = 0;
                    double aux = 0;
                    double divisor = 0;
                    for (int x = i - d; x <= i + d; x++) {
                        for (int y = j - d; y <= j + d; y++) {
                            aux = MASK.get(x - i + d).get(y - j + d).apply(module[x - i + d][y - j + d]);
                            accum += aux * getComponent(Math.floorMod(x, width), Math.floorMod(y, height), c);
                            divisor += aux;
                        }
                    }
                    accum /= divisor;
                    ans.setComponent(i, j, c, accum);
                }

            }
        }
        return ans;
    }

    public Image otsu(){
        if(encoding.equals(Encoding.HSV))
            throw new IllegalArgumentException();
        Image ans = clone();
        for(int i = 0; i < encoding.getBands(); i++)
            ans.otsu(i);
        return ans;
    }

    public void otsu(int component){
        checkConstraints(component, Encoding.HSV);

        double[] histogram = this.histogram(component);

        double[] cummulativeRelativeHisto = cummulativeRelativeHistogram(histogram, Arrays.stream(histogram).sum());

        double N = Arrays.stream(histogram).sum();

        double[] relativeHistogram = Arrays.stream(histogram).map(x -> x / N).toArray();

        double[] cummulativeMean = cummulativeMean(relativeHistogram);

        double[] classVariance = classVariance(cummulativeMean,cummulativeRelativeHisto);

        thresholding(component, getOtsuThreshold(classVariance));

    }


    public Image susanDetector( double t , Boolean overThisImage ){
            if( encoding.equals(Encoding.HSV) || t < 0 || t > 1)
                throw new IllegalArgumentException();

            double[][] MASK = {{0.0,0.0,1.0, 1.0, 1.0,0.0,0.0},
                    {0.0,1.0,1.0, 1.0, 1.0,1.0,0.0},
                    {1.0,1.0,1.0, 1.0, 1.0,1.0,1.0},
                    {1.0,1.0,1.0, 1.0, 1.0,1.0,1.0},
                    {1.0,1.0,1.0, 1.0, 1.0,1.0,1.0},
                    {0.0,1.0,1.0, 1.0, 1.0,1.0,0.0},
                    {0.0,0.0,1.0, 1.0, 1.0,0.0,0.0}};

            Image aux = this.clone();

            double[] n = new double[width*height];
            int d = 3;
            double pixels = 0.0;
            for(int i = 0; i < aux.width; i++){
                for(int j = 0; j < aux.height; j++){
                    pixels = 0.0;
                        for (int x = i - 3; x <= i + 3; x++) {
                            for (int y = j - 3; y <= j + 3; y++) {
                                if (MASK[x - i + d][y - j + d] != 0.0 && !aux.isOutOfBounds(x,y)  ){
                                    pixels++;
                                    if ( Math.abs(aux.getModule(x,y) - aux.getModule(i,j)) < t)
                                        n[i + j*width]++;

                                }
                            }
                        }
                    n[i + j*width] /= pixels;
                }
            }
            if (!overThisImage){
                Image ans = new Image(width, height, Encoding.GS, true);
                for (int i = 0; i < n.length; i++) {
                    if (Math.abs(0.65 - (1 - n[i])) < 0.1 ) {
                        ans.setComponent(i % ans.width, i / ans.width, 0, MAX_D);
                    }

                    if (Math.abs(0.5 - (1 - n[i])) < 0.1 ) {
                        ans.setComponent(i % ans.width, i / ans.width, 0, 0.3);
                    }
                }
                aux = ans;
            } else {
                for (int i = 0; i < n.length; i++) {
                    if (Math.abs(0.65 - (1 - n[i])) < 0.1 ) {
                        aux.setComponent(i % aux.width, i / aux.width, 0, MAX_D);
                        if (encoding.getBands() > 1) {
                            aux.setComponent(i % aux.width, i / aux.width, 1, 0);
                            aux.setComponent(i % aux.width, i / aux.width, 2, 0);
                        }

                    }

                    if (Math.abs(0.5 - (1 - n[i])) < 0.1 ) {
                        aux.setComponent(i % aux.width, i / aux.width, 0, 0.3);
                        if (encoding.getBands() > 1) {
                            aux.setComponent(i % aux.width, i / aux.width, 1, 0.8);
                            aux.setComponent(i % aux.width, i / aux.width, 2, 0.8);
                        }
                    }
                }
            }


            return aux;
    }

    private double getModule(int x, int y) {
        double ans = 0;
        for (int c = 0; c < encoding.getBands(); c++) {
            ans += Math.pow(getComponent(x,y,c),2);
        }
        return Math.sqrt(ans);
    }

    /*
    BEGIN ACTIVE CONTOURS
     */

    public void activeContours(RegionFeatures features){
        activeContours(features, (x, y) -> FdBACK_IMG(x, y, features.getObjAvg(), features.getBackAvg()));
    }

    public void activeContours(RegionFeatures features, double threshold){
        activeContours(features, (x, y) -> FdIMG(x, y, features.getObjAvg(), threshold));
    }

    private void activeContours(RegionFeatures features, BiFunction<Integer, Integer, Double> f){

        final int n = 5;
        final int d = n/2;

        ConvolutionParameters gauss = gaussMask(n, 1, false);
        Predicate<Double> p1 = x -> x > 0;
        Predicate<Double> p2 = x -> x < 0;


        activeContours(features, Math.max(width, height), p1, p2, f);

        activeContours(features,  Math.max(width, height), p2, p1, (x, y) -> {
            double accum = 0;
            for(int i = x-d; i <= x+d; i++){
                for(int j = y-d; j <= y+d; j++){
                    accum += gauss.mask[i - x + d][j - y + d] * features.phi(Math.floorMod(i, width), Math.floorMod(j, height));
                }
            }
            accum /= gauss.divisor;
            return accum;
        });

    }

    private void activeContours(RegionFeatures features, int maxIters, Predicate<Double> pLout, Predicate<Double> pLin, BiFunction<Integer, Integer, Double> F){
        if(encoding != Encoding.RGB)
            throw new IllegalArgumentException();

        boolean allLoutOk = false, allLinOk = false;
        while (maxIters > 0 && !(allLinOk && allLoutOk)){
            allLoutOk = allLinOk = true;

            int x, y;

            /**
             * SECOND STEP
             */

            Set<int[]> addToLout = new HashSet<>();
            for (Iterator<int[]> it = features.getLout().iterator(); it.hasNext();) {
                int[] ints = it.next();
                x = ints[0]; y = ints[1];

                assert (features.phi(x, y) == 1);

                if(pLout.test(F.apply(x, y))){
                    allLoutOk = false;

                    it.remove();
                    features.getLin().add(ints);
                    features.setPhi(x, y, -1);

                    for(int i = x-1; i <= x+1; i++){
                        for(int j = y-1; j <= y+1; j++){
                            if(i != x && j != y && !features.isOutOfBounds(i, j) && features.phi(i, j) == 3){
                                addToLout.add(new int[]{i, j});

                                //TODO: Check if need to update separatedly
                                features.setPhi(i, j, 1);
                            }
                        }
                    }

                }

            }
            features.getLout().addAll(addToLout);


            /**
             * THIRD STEP
             */
            for (Iterator<int[]> it = features.getLin().iterator(); it.hasNext();) {
                int[] ints = it.next();
                x = ints[0]; y = ints[1];


                assert (features.phi(x, y) == -1);

                boolean validLin = false;
                for(int i = x-1; i <= x+1 && !validLin; i++){
                    for(int j = y-1; j <= y+1; j++){
                        if(i != x && j != y && !features.isOutOfBounds(i, j) && features.phi(i, j) > 0){
                            validLin = true;
                            break;
                        }
                    }
                }
                if(!validLin) {
                    //All negative, x is interior now
                    it.remove();
                    features.setPhi(x, y, -3);
                }

            }

            /**
             * FOURTH STEP
             */

            Set<int[]> addToLin = new HashSet<>();
            for (Iterator<int[]> it = features.getLin().iterator(); it.hasNext();) {
                int[] ints = it.next();
                x = ints[0]; y = ints[1];

                assert (features.phi(x, y) == -1);

                if(pLin.test(F.apply(x, y))){
                    allLinOk = false;

                    it.remove();
                    features.getLout().add(ints);
                    features.setPhi(x, y, 1);

                    for(int i = x-1; i <= x+1; i++){
                        for(int j = y-1; j <= y+1; j++){
                            if(i != x && j != y && !features.isOutOfBounds(i, j) && features.phi(i, j) == -3){
                                addToLin.add(new int[]{i, j});

                                //TODO: Check if need to update separatedly
                                features.setPhi(i, j, -1);

                            }
                        }
                    }

                }
            }
            features.getLin().addAll(addToLin);

            /**
             *  FIFTH STEP
             */


            for (Iterator<int[]> it = features.getLout().iterator(); it.hasNext();) {
                int[] ints = it.next();
                x = ints[0]; y = ints[1];

                assert (features.phi(x, y) == 1);

                boolean validLout = false;
                for(int i = x-1; i <= x+1 && !validLout; i++){
                    for(int j = y-1; j <= y+1; j++){
                        if(i != x && j != y && !features.isOutOfBounds(i, j) && features.phi(i, j) < 0){
                            validLout = true;
                            break;
                        }
                    }
                }
                if(!validLout) {
                    //All negative, x is exterior now
                    it.remove();
                    features.setPhi(x, y, 3);
                }

            }

            maxIters--;
        }

    }

    private double FdBACK_IMG(int x, int y, Vector3D thetaObj, Vector3D thetaBack){
        Vector3D thetaPix = new Vector3D(getComponent(x, y, 0), getComponent(x, y, 1), getComponent(x, y, 2));

        if(Double.compare(0, thetaObj.sub(thetaPix).mod()) == 0)
            return 0;

        double div = thetaBack.sub(thetaPix).mod()/thetaObj.sub(thetaPix).mod();

        return div < 1 ? -1 : div > 1 ? 1 : 0;
    }

    private double FdIMG(int x, int y, Vector3D thetaObj, double threshold){
        Vector3D thetaPix = new Vector3D(getComponent(x, y, 0), getComponent(x, y, 1), getComponent(x, y, 2));

        double div = thetaObj.sub(thetaPix).mod()/SQRT_3;

        return Double.compare(threshold, div);
    }

    /*
    END ACTIVE CONTOURS
     */

    public Image harris(double k){
        if(encoding == Encoding.HSV)
            throw new IllegalArgumentException();

        Image gs = toGS();


        ConvolutionParameters gauss = gaussMask(3, 1, true);

        /**
         * SOBEL EDGE DETECTOR
         */

        double[][] MASK_DX = {{-1.0, 0.0, 1.0},
                              {-2.0, 0.0, 2.0},
                              {-1.0, 0.0, 1.0}};

        double[][] MASK_DY = {{-1.0, -2.0, -1.0},
                              { 0.0,  0.0,  0.0},
                              { 1.0,  2.0,  1.0}};

        ConvolutionParameters dx = new ConvolutionParameters(MASK_DX, false, 1),
                              dy = new ConvolutionParameters(MASK_DY, false, 1);

        /**
         * Ix Iy, Ix2, Iy2, Ixy
         */

        List<ImageMaxMin> IxIy = gs.convolution(dx, dy);

        Image Ix = IxIy.get(0).image, Iy = IxIy.get(1).image;
        Image Ix2 = new Image(width, height, Encoding.GS, true),
              Iy2 = new Image(width, height, Encoding.GS, true),
              Ixy = new Image(width, height, Encoding.GS, true);

        applyAndAdjust(Ix, Ix, Ix2, (x, y)-> x*y, null);
        applyAndAdjust(Iy, Iy, Iy2, (x, y)-> x*y, null);
        applyAndAdjust(Ix, Iy, Ixy, (x, y)-> x*y, null);


        Ix2 = Ix2.convolution(gauss).get(0).image;
        Iy2 = Iy2.convolution(gauss).get(0).image;
        Ixy = Ixy.convolution(gauss).get(0).image;


        Image cim1 = new Image(width, height, Encoding.GS, true);

        double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                double ix2 = Ix2.getComponent(i, j, 0);
                double iy2 = Iy2.getComponent(i, j, 0);
                double ixy = Ixy.getComponent(i, j, 0);
                double val = (ix2*iy2 - ixy*ixy) - k*(ix2 + iy2)*(ix2 + iy2);
                max = Math.max(max, val); min = Math.min(min, val);
                cim1.setComponent(i, j, 0, val);
            }
        }
        cim1 = cim1.adjust(linearAdjust, new double[]{max, max, max}, new double[]{min, min, min});

        return cim1;
    }


    public Image houghTransform(FigureMode mode ,Double pStep,Double thetaStep, Integer aStep, Integer bStep, Integer rStep,Double epsilon,Double threshold){
        double t0 = System.currentTimeMillis();
        Image aux = toGS().sobel().otsu();
        List<Pixel> whitePixels = aux.getWhitePixels();
        Image ans = new Image(width, height, Encoding.GS, true);
        if (mode.equals(FigureMode.STRAIGHT)){
            Integer D = Math.max(aux.width,aux.height);
            Map<Double,Map<Double,Integer>>  accumulator = new HashMap<>();
            Integer maxAccum = 0;
            for (double p = (-1) * Math.sqrt(2) * D ; p <= Math.sqrt(2) * D ; p+=pStep){
                accumulator.put(p,new HashMap<>());
                for (double theta = -90 ; theta <= 90 ; theta+= thetaStep){
                    Integer currAccum = 0;
                    for (Pixel whitePixel : whitePixels){
                        if (Math.abs(p - whitePixel.x * Math.cos(Math.toRadians(theta)) - whitePixel.y * Math.sin(Math.toRadians(theta))) < epsilon){
                            currAccum++;
                        }
                    }
                    accumulator.get(p).put(theta,currAccum);
                    if (currAccum > maxAccum){
                        maxAccum = currAccum;
                    }
                }
            }
            for (double p = (-1) * Math.sqrt(2) * D ; p <= Math.sqrt(2) * D ; p+=pStep){
                for (double theta = -90 ; theta <= 90 ; theta+= thetaStep){
                    if (accumulator.get(p).get(theta) >=   (threshold*maxAccum)){
                        //System.out.println(p+" "+theta);
                        aux.printStraight(p,theta);
                    }
                }
            }
            System.out.println(System.currentTimeMillis() - t0);

        } else{
            Double D = Math.sqrt(Math.pow(aux.width,2) + Math.pow(aux.height,2));
            Map<Integer,Map<Integer,Map<Integer,Integer>>>  accumulator = new HashMap<>();
            Integer maxAccum = 0;
            for (int a = 0 ; a < width ; a+=aStep){
                accumulator.put(a,new HashMap<>());
                for (int b = 0 ; b < height ; b+= bStep){
                    accumulator.get(a).put(b,new HashMap<>());
                    for (int r = 1 ; r <= D/2 ; r+=rStep) {
                        Integer currAccum = 0;
                        for (Pixel whitePixel : whitePixels) {
                            if (Math.abs(Math.pow(r,2) - Math.pow(whitePixel.x - a,2) - Math.pow(whitePixel.y - b,2)) < epsilon) {
                                currAccum++;
                            }
                        }
                        accumulator.get(a).get(b).put(r, currAccum);
                        if (currAccum > maxAccum) {
                            maxAccum = currAccum;
                        }
                    }
                }
            }
            for (int a = 0 ; a < width ; a+=aStep) {
                for (int b = 0; b < height; b += bStep) {
                    for (int r = 1; r <= D/2 ; r += rStep) {
                        if (accumulator.get(a).get(b).get(r) >= threshold*maxAccum) {
                            aux = ans;
                            ImageDrawingUtils.drawCircleBorder(aux, a, b, r, (j, k) -> new double[]{1.0, 0.0, 0.0});
                        }
                    }
                }
            }
            System.out.println(System.currentTimeMillis() - t0);
        }
        return aux;

    }

    private static class Pixel{
        int x,y;

        public Pixel(int x,int y){
            this.x = x;
            this.y=y;
        }
    }

    public List<Pixel> getWhitePixels(){
        if (getEncoding() != encoding.GS){
            throw new IllegalArgumentException("La imagen tiene que ser en escala de grises");
        }
        List<Pixel> ans = new LinkedList<>();
        for (int i = 0 ; i < width ; i++){
            for (int j = 0 ; j < height ; j++){
                if (getComponent(i,j,0) == MAX_D){
                    ans.add(new Pixel(i,j));
                }
            }
        }
        return ans;
    }

    public void printStraight(double p,double theta){
        if (getEncoding() != encoding.GS){
            throw new IllegalArgumentException("La imagen tiene que ser en escala de grises");
        }
        int y;
        if (Math.abs(theta) > 5) {
            for (int x = 0; x < width; x++) {
                y = (int) ((p - (x * Math.cos(Math.toRadians(theta)))) / Math.sin(Math.toRadians(theta)));
                if (!isOutOfBounds(x, y)) {
                    // System.out.println("x "+x +" y "+y);
                    if (getComponent(x,y,0) == 0)
                        setComponent(x, y, 0, MAX_D/2);
                }
            }
        }
        else {
            int x = (int)(p / Math.cos(Math.toRadians(theta)));
            for ( y = 0; y < width; y++) {
                if (!isOutOfBounds(x, y)) {
                    setComponent(x, y, 0, MAX_D);
                }
            }
        }
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
        return byteToDouble(doubleToByte(d));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return id == image.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
