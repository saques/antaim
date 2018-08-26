package noise;

import java.util.Random;

public class ExponentialNoiseGenerator implements NoiseGenerator {

    private double lambda;

    private ExponentialGenerator eg;

    public ExponentialNoiseGenerator(double lambda){
        this.lambda = lambda;
        this.eg = new ExponentialGenerator(lambda,new Random());
    }

    @Override
    public double nextVal() {
        return eg.nextVal();
    }
}
