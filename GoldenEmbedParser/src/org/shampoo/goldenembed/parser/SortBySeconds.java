package org.shampoo.goldenembed.parser;

public class SortBySeconds implements java.util.Comparator<GoldenCheetah> {
    @Override
    public int compare(GoldenCheetah aGC, GoldenCheetah anotherGC) {
        long secDiff = aGC.getSecs() - anotherGC.getSecs();
        return (int) secDiff;
    }
}
