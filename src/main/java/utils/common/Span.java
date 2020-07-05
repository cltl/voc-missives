package utils.common;

public class Span implements Comparable<Span> {
    int firstIndex;
    int lastIndex;

    public Span(int firstIndex, int lastIndex) {
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
    }

    public static int lastIndex(int offset, int length) {
        return offset + length - 1;
    }

    public static Span fromCharPosition(String offset, String length) {
        int o = Integer.parseInt(offset);
        return new Span(o, lastIndex(o, Integer.parseInt(length)));
    }

    public String toString() {
        return firstIndex + "-" + lastIndex;
    }
    public int getLength() {
        return lastIndex - firstIndex + 1;
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    public int getLastIndex() {
        return lastIndex;
    }

    @Override
    public int compareTo(Span o) {
        // prefix order
        if (firstIndex < o.getFirstIndex() || firstIndex == o.getFirstIndex() && lastIndex >= o.getLastIndex())
            return -1;
        else
            return 1;
    }
}
