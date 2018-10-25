package structures;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class Vector3D {

    public static final Vector3D X = new Vector3D(1, 0,0);
    public static final Vector3D Y = new Vector3D(0, 1,0);
    public static final Vector3D Z = new Vector3D(0, 0,1);

    @Getter
    public double x, y, z;

    public Vector3D(Vector3D vector3D){
        this.x = vector3D.x;
        this.y = vector3D.y;
        this.z = vector3D.z;
    }

    public Vector3D(){
        x = y = z = 0;
    }

    public double dot(Vector3D o){
        return x*o.x + y*o.y + z*o.z;
    }

    public Vector3D sub(Vector3D o){
        return new Vector3D(x-o.x, y-o.y, z-o.z);
    }

    public Vector3D add(Vector3D o) {
        return new Vector3D(x+o.x, y+o.y, z+o.z);
    }

    public Vector3D subP(Vector3D o){
        x -= o.x;
        y -= o.y;
        z -= o.z;
        return this;
    }

    public Vector3D addP(Vector3D o) {
        x += o.x;
        y += o.y;
        z += o.z;
        return this;
    }

    public Vector3D nor(){
        double mod = mod();
        return scl(1.0/mod);
    }

    public Vector3D scl(double scl){
        return new Vector3D(x*scl, y*scl, z*scl);
    }

    public Vector3D sclP(double scl){
        x *= scl;
        y *= scl;
        z *= scl;
        return this;
    }

    public double mod(){
        return Math.sqrt(mod2());
    }

    public double mod2(){
        return (Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

}
