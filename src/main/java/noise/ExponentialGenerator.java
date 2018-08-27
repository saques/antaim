package noise;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Random;

@AllArgsConstructor
public class ExponentialGenerator implements NoiseGenerator {

    @Getter
    private double lambda;

    @Getter
    private Random rand ;

    @Override
    public double nextVal() {
        return (-1/lambda) * Math.log(rand.nextDouble());
    }

}
