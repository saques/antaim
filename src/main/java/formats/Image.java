package formats;


import lombok.Getter;

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

    private int getIndex(int x, int y, int component){
        return (x*width + y)*encoding.getBands() + component;
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
