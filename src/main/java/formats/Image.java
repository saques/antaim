package formats;


import formats.exceptions.NoSuchComponentException;
import lombok.Getter;

import javax.naming.OperationNotSupportedException;
import java.awt.image.BufferedImage;

public class Image {


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

    public double getComponent(int x, int y, int component) {
        checkConstraints(x, y, component);
        return data[getIndex(x, y, component, width, encoding)];
    }

    public void setComponent(int x, int y, int component, double value){
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
        for(int b = 0; b < encoding.getBands(); b++)
            ans[b] = getComponent(x, y, b);
        return ans;
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
                    image.data[getIndex(x, y, b, nWidth, encoding)] = d(x, y, b);
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
                ans = ((0xFF& b(x,y,0)) << 16) | ((0xFF& b(x,y,1)) << 8) | (0xFF& b(x,y,2));
                break;
            case GS:
                ans = ((0xFF& b(x,y,0)) << 16) | ((0xFF& b(x,y,0)) << 8) | (0xFF& b(x,y,0));
                break;
            case HSV:
                double[] rgb = toRGB(d(x, y, 0), d(x, y, 1), d(x, y, 2));
                ans = ((0xFF & doubleToByte(rgb[0])) << 16) | ((0xFF& doubleToByte(rgb[1])) << 8) | (0xFF& doubleToByte(rgb[2]));
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

    private static int getIndex(int x, int y, int component, int width, Encoding encoding){
        return (x + y*width)*encoding.getBands() + component;
    }

    public static double byteToDouble(byte b){
        return (0xFF & b)/((double)0xFF);
    }

    public static byte doubleToByte(double b){
        return (byte)(0xFF & (int)Math.ceil(b*0xFF));
    }

}
