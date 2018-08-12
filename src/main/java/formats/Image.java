package formats;


import formats.exceptions.NoSuchComponentException;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Image {


    byte[] data;
    @Getter
    int width, height;
    @Getter
    Encoding encoding;

    public Image(int width, int height, Encoding encoding, boolean initData){
        this.width = width;
        this.height = height;
        this.encoding = encoding;
        if(initData)
            data = new byte[width*height*encoding.getBands()];
    }

    public double getComponent(int x, int y, int component) {
        checkConstraints(x, y, component);
        return byteToDouble(data[getIndex(x, y, component, width, encoding)]);
    }

    public void setComponent(int x, int y, int component, double value){
        checkConstraints(x, y, component);
        data[getIndex(x, y, component, width, encoding)] = doubleToByte(value);
    }

    public boolean setComponents(int x, int y, double[] rgb){
        if(isOutOfBounds(x, y))
            return false;
        for(int b = 0; b < encoding.getBands(); b++)
            setComponent(x, y, b, rgb[b]);
        return true;
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
                ans = ((0xFF&d(x,y,0)) << 16) | ((0xFF&d(x,y,1)) << 8) | (0xFF&d(x,y,2));
                break;
            case GS:
                ans = ((0xFF&d(x,y,0)) << 16) | ((0xFF&d(x,y,0)) << 8) | (0xFF&d(x,y,0));
                break;
        }
        return ans;
    }


    public boolean isOutOfBounds(int x, int y){
        return x < 0 || y < 0 || x >= width || y >= height;
    }

    private byte d(int x, int y, int component){
        return data[getIndex(x,y,component, width, encoding)];
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
