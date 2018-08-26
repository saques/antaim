package noise;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Random;

@AllArgsConstructor
public class RayleighGenerator implements NoiseGenerator {

    @Getter
    private double fi;

    @Getter
    private Random rand ;

    @Override
    public double nextVal() {
        return fi * Math.sqrt( (-2) * Math.log( 1 - rand.nextDouble() ) );
    }


}
