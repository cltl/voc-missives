package tei2xmi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TextFormatter extends Formatter {

    final static String FILE_EXTENSION = "";
    public TextFormatter() {
        super(FILE_EXTENSION);
    }

    protected static final Logger logger = LogManager.getLogger(Formatter.class);

    public List<TeiLeaf> format(ATeiTree tei) {
        List<ATeiTree> candidateTrees = listParagraphsHeadsAndRows(tei);
        filterNotesAndBreaks(candidateTrees);
        cleanStrings(candidateTrees);
        List<TeiLeaf> leaves = toLeaf(candidateTrees);
        for (TeiLeaf leaf: leaves) {
            if (leaf.getContent().equals(""))
                logger.warn("Found empty content for paragraph " + leaf.getId());
        }
        leaves = leaves.stream().filter(x -> ! x.getContent().equals("")).collect(Collectors.toList());
        return leaves;
    }


    private List<TeiLeaf> toLeaf(List<ATeiTree> candidateTrees) {
        List<TeiLeaf> leaves = new LinkedList<>();
        for (ATeiTree t: candidateTrees) {
            if (t instanceof TeiLeaf)
                leaves.add((TeiLeaf) t);
            else if (t instanceof TeiTree)
                leaves.add(TeiLeaf.create(t.getTeiType(), t.getId(), ((TeiTree) t).getContent()));
            else if (t instanceof TeiDiv) {
                String str = t.collect(x -> x.getTeiType() == ATeiTree.TeiType.STR).stream()
                        .map(p -> ((TeiLeaf) p).getContent().trim()).collect(Collectors.joining(" "));
                leaves.add(TeiLeaf.create(t.getTeiType(), t.getId(), str));
            }
            else
                throw new IllegalArgumentException("Trying to create leaf from tree of type " + t.getClass());

        }
        return leaves;
    }


    protected void filterNotesAndBreaks(List<ATeiTree> paragraphs) {
        paragraphs.forEach(p -> p.remove(t -> isNoteOrBreak(t)));
    }

    private boolean isNoteOrBreak(ATeiTree t) {
        return t.getTeiType().equals(ATeiTree.TeiType.NOTE) || t instanceof TeiBreak;
    }

    List<ATeiTree> listParagraphsHeadsAndRows(ATeiTree tei) {
        return tei.filterHighest(t -> isParagraph(t) || t.getTeiType().equals(ATeiTree.TeiType.ROW));
    }

}
