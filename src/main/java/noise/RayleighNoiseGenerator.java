package noise;

import java.util.Random;

public class RayleighNoiseGenerator implements NoiseGenerator {

    private double fi;

    private RayleighGenerator rg;

    public RayleighNoiseGenerator(double fi){
        this.fi = fi;
        this.rg = new RayleighGenerator(fi,new Random());
    }

    @Override
    public double nextVal() {
        return rg.nextVal();
    }
}
