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
        cleanStrings(paragraphs);
        return flatten(paragraphs);
    }

    private List<ATeiTree> listNotes(ATeiTree tree) {
        return tree.collect(t -> t.getTeiType().equals(ATeiTree.TeiType.NOTE) || isFoliaReference(t));
    }

    private boolean isFoliaReference(ATeiTree t) {
        return t.getTeiType().equals(ATeiTree.TeiType.P) && FOLIA.matcher(createParagraph(t).getContent()).matches();
    }
}
