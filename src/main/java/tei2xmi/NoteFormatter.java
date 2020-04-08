package tei2xmi;

import utils.ATeiTree;
import utils.TeiLeaf;

import java.util.List;

public class NoteFormatter extends Formatter {

    final static String FILE_EXTENSION = "_notes";

    public NoteFormatter() {
        super(FILE_EXTENSION);
    }

    @Override
    public List<TeiLeaf> format(ATeiTree tei) {
        List<ATeiTree> paragraphs = listNotes(tei);
        return getContent(paragraphs);
    }

    private List<ATeiTree> listNotes(ATeiTree tree) {
        return tree.getAllNodes(t -> t.getTeiType().equals(ATeiTree.TeiType.NOTE));
    }

    protected TeiLeaf toLeaf(ATeiTree p) {
        String str = getString(p, x -> x.replaceAll("\n", ""), "");
        return TeiLeaf.create(p.getTeiType(), p.getId(), str);
    }
}
