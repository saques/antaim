package noise;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Random;

@AllArgsConstructor
public class GaussianGenerator implements NoiseGenerator {

    @Getter
    private double sigma, mu;

    @Getter
    private Random rand ;

    @Override
    public double nextVal() {
        return rand.nextGaussian() * sigma;
    }

}
