package utils;

public class Segment {
    int begin;
    int end;
    int index;

    public Segment(int begin, int end, int index) {
        this.begin = begin;
        this.end = end;
        this.index = index;
    }

    public int getEnd() {
        return end;
    }

    public int getBegin() {
        return begin;
    }

    public int getIndex() {
        return index;
    }
}
