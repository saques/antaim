package noise;


public class SaltAndPepperGenerator implements NoiseGenerator {

    private double v1, v2;

    public SaltAndPepperGenerator(double v1){
        if(v1 >= 0.5 || v1 <= 0)
            throw new IllegalArgumentException();
        this.v1 = v1;
        this.v2 = 1 - v1;
    }

    @Override
    public double nextVal() {
        double rand = Math.random();
        return rand <= v1 ? 0 : (rand >= v2 ? 1 : -1);
    }
}
