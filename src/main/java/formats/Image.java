package formats;


import formats.exceptions.NoSuchComponentException;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class Image {


    byte[] data;
    @Getter
    int width, height;
    @Getter
    Encoding encoding;

    public Image(int width, int height, Encoding encoding){
        this.width = width;
        this.height = height;
        this.encoding = encoding;
    }

    public double getComponent(int x, int y, int component) {
        checkConstraints(x, y, component);
        return byteToDouble(data[getIndex(x, y, component)]);
    }

    public void setComponent(int x, int y, int component, double value){
        checkConstraints(x, y, component);
        data[getIndex(x, y, component)] = doubleToByte(value);
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


    private int getIndex(int x, int y, int component){
        return (x + y*width)*encoding.getBands() + component;
    }

    private byte d(int x, int y, int component){
        return data[getIndex(x,y,component)];
    }

    private double byteToDouble(byte b){
        return (0xFF & b)/((double)0xFF);
    }

    private byte doubleToByte(double b){
        return (byte)(0xFF & (int)Math.ceil(b*0xFF));
    }

    private void checkConstraints(int x, int y, int component){
        if(component >= encoding.getBands())
            throw new NoSuchComponentException(component, encoding);
        if(x < 0 || y < 0 || x >= height || y >= width)
            throw new IndexOutOfBoundsException();
    }

}
