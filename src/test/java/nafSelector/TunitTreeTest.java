package nafSelector;

import org.junit.jupiter.api.Test;
import xjc.naf.Tunit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TunitTreeTest {

    private Tunit createTunit(String id, int firstIndex, int lastIndex) {
        Tunit t = new Tunit();
        t.setId(id);
        t.setOffset(firstIndex + "");
        t.setLength(lastIndex - firstIndex + 1 + "");
        return t;
    }

    @Test
    public void testTreeCreation() {
        List<Tunit> elts = new LinkedList<>();
        elts.add(createTunit("A", 0, 10));
        elts.add(createTunit("B", 0, 10));
        elts.add(createTunit("C", 1, 6));
        elts.add(createTunit("D", 2, 3));
        elts.add(createTunit("E", 5, 5));
        elts.add(createTunit("F", 8, 10));
        elts.add(createTunit("G", 9, 10));
        elts.add(createTunit("H", 10, 10));

        TunitTree root = TunitTree.create(Collections.singletonList(elts.get(0)));
        assertTrue(root.dominates(createTunit("F", 8, 10)));

        TunitTree tree = TunitTree.create(elts);
        assertEquals(tree.toString(), "[A [B [C [D] [E]] [F [G [H]]]]]");
    }

    @Test
    public void testSplitChildren() {
        List<Tunit> elts = new LinkedList<>();
        elts.add(createTunit("F", 8, 10));
        elts.add(createTunit("G", 9, 10));
        elts.add(createTunit("H", 10, 10));
        TunitTree tree = TunitTree.create(elts);
        List<Tunit> tunits = tree.extractTunits(x -> x.getNode().getId().equals("H"));
        assertEquals(tunits.size(), 2);
        assertEquals(tunits.get(0).getId(), "F.co8-1");
        assertEquals(tunits.get(1).getId(), "G.co9-1");

    }

}