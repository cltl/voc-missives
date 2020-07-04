package tei2naf;

public class CharPosition {
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
}
