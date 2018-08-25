package noise;

public interface NoiseGenerator {
    /**
     * This function should return -1 if the pixel mustn't be contaminated.
     * @return
     */
    double nextVal();
}
