package formats;

import lombok.Getter;
import structures.Vector3D;

import java.util.HashSet;
import java.util.Set;

@Getter
public class RegionFeatures {

    public static RegionFeatures buildRegionFeatures(Image img, int x0, int y0, int x1, int y1){
        if(img.getEncoding() != Encoding.RGB)
            throw new IllegalArgumentException();

        RegionFeatures ans = new RegionFeatures();

        ans.width = img.getWidth();
        ans.height = img.getHeight();
        ans.phi = new int[img.getWidth() * img.getHeight()];

        ans.objAvg = new Vector3D();
        ans.backAvg = new Vector3D();

        ans.lin = new HashSet<>();
        ans.lout = new HashSet<>();

        int objCount = 0, backCount = 0;

        for(int i = 0; i < ans.width; i++){
            for(int j = 0; j < ans.height; j++){

                if(i < x0-1 || i > x1+1 || j < y0-1 || j > y1+1){
                    //Outside of image
                    ans.setPhi(i, j, 3);
                    backCount ++;

                    ans.backAvg.addP(Vector3D.X.scl(img.getComponent(i, j,0)));
                    ans.backAvg.addP(Vector3D.Y.scl(img.getComponent(i, j,1)));
                    ans.backAvg.addP(Vector3D.Z.scl(img.getComponent(i, j,2)));

                } else if(i > x0 && i < x1 && j > y0 && j < y1){
                    //inside of object
                    ans.setPhi(i, j, -3);
                    objCount ++;

                    ans.objAvg.addP(Vector3D.X.scl(img.getComponent(i, j,0)));
                    ans.objAvg.addP(Vector3D.Y.scl(img.getComponent(i, j,1)));
                    ans.objAvg.addP(Vector3D.Z.scl(img.getComponent(i, j,2)));

                } else if((i == x0 && j >= y0 && j <= y1) || (i == x1 && j >= y0 && j <= y1) ||
                          (j == y0 && i >= x0 && i <= x1) || (j == y1 && i >= x0 && i <= x1 )){

                    //lin
                    ans.setPhi(i, j, -1);
                    ans.getLin().add(new int[]{i, j});

                } else {
                    //lout
                    ans.setPhi(i, j, 1);
                    ans.getLout().add(new int[]{i, j});
                }

            }
        }

        ans.objAvg.sclP(1.0/objCount);
        ans.backAvg.sclP(1.0/backCount);

        return ans;
    }


    private int[] phi;
    private Vector3D objAvg, backAvg;
    private int width, height;
    private Set<int[]> lin, lout;

    public int phi(int x, int y){
        if(isOutOfBounds(x, y))
            throw new ArrayIndexOutOfBoundsException();
        return phi[x + y*width];
    }

    void setPhi(int x, int y, int val){
        if(isOutOfBounds(x, y))
            throw new ArrayIndexOutOfBoundsException();
        phi[x + y*width] = val;
    }

    public boolean isOutOfBounds(int x, int y){
        return x < 0 || y < 0 || x >= width || y >= height;
    }

}
