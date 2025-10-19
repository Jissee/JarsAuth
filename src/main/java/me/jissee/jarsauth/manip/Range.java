package me.jissee.jarsauth.manip;

import java.util.ArrayList;
import java.util.LinkedList;

public class Range {
    public static ArrayList<Integer> getArrayRange(int from, int to){
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = from; i < to; i++) {
            result.add(i);
        }
        return result;
    }
    public static LinkedList<Integer> getLinkedRange(int from, int to){
        LinkedList<Integer> result = new LinkedList<>();
        for (int i = from; i < to; i++) {
            result.add(i);
        }
        return result;
    }
}
