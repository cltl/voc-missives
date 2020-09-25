package tei2naf;

import utils.common.Span;
import utils.naf.CharPosition;
import utils.tei.ATeiTree;

public class AnchoredNode {
    ATeiTree node;
    CharPosition position;
    String yield;

    private AnchoredNode(ATeiTree node, String yield, int offset) {
        this.node = node;
        this.yield = yield;
        this.position = new CharPosition(offset, yield.length());
    }

    public static AnchoredNode createRoot(ATeiTree tree) {
        return new AnchoredNode(tree, tree.yield(), 0);
    }

    public static AnchoredNode create(ATeiTree node, AnchoredNode parent, int relativeOffset) {
        String yield = node.yield();
//        int relativeOffset = 0;
//        if (! lastAddedFragment.getSpan().equals(parent.getSpan()))
//            relativeOffset = lastAddedFragment.getEndIndex() - parent.getOffset();   // will match from the end of the left sibling if present

        int offset = relativeOffset;
        if (yield.length() > 0) {
            // elements of 0 length (page breaks) take the *next* char offset after the left sibling, or the parent char offset
            offset = parent.indexOf(yield, relativeOffset);
            if (offset == -1 )
                offset = parent.indexOf(yield);
            if (offset == -1)
                throw new IllegalArgumentException("cannot find yield of element " + node.getId() + " in " + parent.getId());
        }
        return new AnchoredNode(node, yield, parent.getOffset() + offset);
    }

    private int indexOf(String y, int i) {
        return yield.indexOf(y, i);
    }


    public Span getSpan() { return new Span(position.getOffset(), position.getOffset() + position.getLength() - 1); }

    public int indexOf(String y) {
        return yield.indexOf(y);
    }

    public int length() {
        return yield.length();
    }

    public String getId() {
        return node.getId();
    }

    public int getOffset() {
        return position.getOffset();
    }

}
