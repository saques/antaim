package formats;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

public class RawImageTest {

    private Image image;


    @Before
    public void init() throws IOException {
        image = new Raw(389,164, Encoding.GS, "D:\\git\\antaim\\images\\GIRL.RAW");
    }

    @Test
    public void testCorrectInsertion(){
        for(int i = 0; i < 389; i++) {
            for (int j = 0; j < 164; j++) {
                double d1 = image.getComponent(i, j ,0);
                image.setComponent(i,j,0,d1);
                assertFalse(Math.abs(d1-image.getComponent(i,j,0))>0.0001);
            }
        }

    }
}