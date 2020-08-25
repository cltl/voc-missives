package text.tei2inputNaf;

import utils.common.CharPosition;
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

    public static AnchoredNode create(ATeiTree node, AnchoredNode parent) {
        String yield = node.yield();
        int offset = parent.indexOf(yield);
        if (offset == -1)
            throw new IllegalArgumentException("cannot find yield of element " + node.getId() + " in " + parent.getId());

        return new AnchoredNode(node, yield, parent.getOffset() + offset);
    }

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
