package formats;

import lombok.Getter;

public enum Encoding {

    RGB(3), GS(1);

    Encoding(int bands){
        this.bands = bands;
    }

    @Getter
    private int bands;

}
