package tei2xmi;

import utils.ATeiTree;
import utils.TeiBreak;
import utils.TeiLeaf;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextFormatter extends Formatter {

    final static String FILE_EXTENSION = "";
    public TextFormatter() {
        super(FILE_EXTENSION);
    }

    public List<TeiLeaf> format(ATeiTree tei) {
        List<ATeiTree> paragraphs = listParagraphs(tei);
        cleanStrings(paragraphs);
        filterNotesAndBreaks(paragraphs);
        List<TeiLeaf> paragraphsAsText = flatten(paragraphs);
        return paragraphsAsText;
    }

    protected void filterNotesAndBreaks(List<ATeiTree> paragraphs) {
        paragraphs.forEach(p -> p.remove(t -> isNoteOrBreak(t)));
    }

    private boolean isNoteOrBreak(ATeiTree t) {
        return t.getTeiType().equals(ATeiTree.TeiType.NOTE) || t instanceof TeiBreak;
    }
}
