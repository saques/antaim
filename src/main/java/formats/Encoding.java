package formats;

import lombok.Getter;

import java.awt.image.BufferedImage;

public enum Encoding {

    RGB(3, BufferedImage.TYPE_INT_RGB), GS(1, BufferedImage.TYPE_BYTE_GRAY);

    Encoding(int bands, int bufferedImageType){
        this.bands = bands;
        this.bufferedImageType = bufferedImageType;
    }

    @Getter
    private int bands, bufferedImageType;

}
