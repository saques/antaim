package utils;

import java.util.Collections;
import java.util.List;

public final class MathUtils {

    private MathUtils(){}

    public static double avg(double[] arr){
        double ans = 0;
        for (double anArr : arr) ans += anArr;
        return ans/arr.length;
    }

    public static double std(double[] arr){
        return Math.sqrt(var(arr));
    }

    public static double std(double[] arr, double avg){
        return Math.sqrt(var(arr, avg));
    }

    public static double var(double[] arr){
        return var(arr, avg(arr));
    }

    public static double var(double[] arr, double avg){
        if(arr.length <= 1)
            return 0;
        double var = 0;
        for (double anArr : arr) var += Math.pow(anArr - avg, 2);
        return var / (arr.length - 1);
    }

    public static double median(List<Double> list){
        Collections.sort(list);
        int middle = list.size() / 2;
        middle = middle > 0 && middle % 2 == 0 ? middle - 1 : middle;
        return list.get(middle);
    }


}
