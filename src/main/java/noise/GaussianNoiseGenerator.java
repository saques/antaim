package noise;

import java.util.Random;

public class GaussianNoiseGenerator implements NoiseGenerator {

    private double sigma, mu;

    private GaussianGenerator gg;

    public GaussianNoiseGenerator(double sigma, double mu){
        if( sigma < 0)
            throw new IllegalArgumentException();
        this.sigma = sigma;
        this.mu = mu;
        this.gg = new GaussianGenerator(sigma,mu,new Random());
    }

    @Override
    public double nextVal() {
        return gg.nextVal();
    }
}
