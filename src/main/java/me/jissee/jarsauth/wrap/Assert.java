package me.jissee.jarsauth.wrap;

public class Assert {
    public static boolean assertTrue(boolean condition) {
        return condition;
    }
    public static boolean assertFalse(boolean condition) {
        return !condition;
    }
    public static boolean assertEQ(int x, int y){
        return x == y;
    }
    public static boolean assertGE(int x, int y){
        return x >= y;
    }
    public static int assertInt(int value){
        return value;
    }
}
