package tei2xmi;

import utils.*;

import java.util.List;

public class TextFormatter extends Formatter {

    final static String FILE_EXTENSION = "";
    final static String PARAGRAPH_SEPARATOR = "\n";
    public TextFormatter() {
        super(PARAGRAPH_SEPARATOR, FILE_EXTENSION);
    }

    public List<TeiLeaf> format(ATeiTree tei) {
        List<ATeiTree> candidateTrees = listParagraphsHeadsAndRows(tei);
        removeNotesAndBreaks(candidateTrees);
        List<TeiLeaf> leaves = getContent(candidateTrees);
        return leaves;
    }

    protected void removeNotesAndBreaks(List<ATeiTree> paragraphs) {
        paragraphs.forEach(p -> p.remove(t -> isNoteOrBreak(t)));
    }

    private boolean isNoteOrBreak(ATeiTree t) {
        return t.getTeiType().equals(ATeiTree.TeiType.NOTE) || t instanceof TeiBreak;
    }

    List<ATeiTree> listParagraphsHeadsAndRows(ATeiTree tei) {
        return tei.getTopNodes(t ->  t.getTeiType().equals(ATeiTree.TeiType.P)
                || t.getTeiType().equals(ATeiTree.TeiType.HEAD)
                || t.getTeiType().equals(ATeiTree.TeiType.ROW));
    }

    protected TeiLeaf toLeaf(ATeiTree p) {
        String str = getString(p, x -> x.trim(), " ");
        return TeiLeaf.create(p.getTeiType(), p.getId(), str);
    }

}
