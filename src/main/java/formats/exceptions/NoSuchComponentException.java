package formats.exceptions;

import formats.Encoding;

public class NoSuchComponentException extends RuntimeException {
    public NoSuchComponentException(int band, Encoding encoding){
        super(band + " exceeds the limit for encoding " + encoding.name());
    }
}
