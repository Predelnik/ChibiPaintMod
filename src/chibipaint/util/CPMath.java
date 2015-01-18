package chibipaint.util;

/**
 * Created by Sergey on 19.01.2015.
 */
public class CPMath {
    public static int bound (int x, int min, int max)
    {
        return Math.min (Math.max (x, min), max);
    }
}
