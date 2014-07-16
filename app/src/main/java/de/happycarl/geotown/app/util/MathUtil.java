package de.happycarl.geotown.app.util;

/**
 * Created by jhbruhn on 16.07.14.
 */
public class MathUtil {
    public static long intsToLong(int part1, int part2) {
        return (long) part1 << 32 | part2 & 0xFFFFFFFFL;
    }

    public static int[] longToInts(long num) {
        int[] res = new int[2];
        res[0] = (int) (num >> 32);
        res[1] = (int) num;
        return res;
    }
}
