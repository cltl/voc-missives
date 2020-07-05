package tei2naf;

public class CharPosition implements Comparable<CharPosition> {
    int offset;
    int length;

    public CharPosition(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public int compareTo(CharPosition o) {
        // prefix order
        if (offset < o.getOffset() || offset == o.getOffset() && length >= o.getLength())
            return -1;
        else
            return 1;
    }

    public int getEndIndex() {
        return offset + length;
    }

    public boolean precedes(CharPosition o) {
        return getEndIndex() <= o.getOffset();
    }

    public boolean contains(CharPosition o) {
        return offset <= o.getOffset() && getEndIndex() >= o.getEndIndex();
    }

    public boolean overlaps(CharPosition o) {
        boolean coveredDistanceLessThanSumLengths = Math.max(getEndIndex(), o.getEndIndex())
                - Math.min (offset, o.getOffset()) < length + o.getLength();
        return coveredDistanceLessThanSumLengths;
    }
}
