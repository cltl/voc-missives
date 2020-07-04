package tei2naf;

import tei2xmi.Formatter;
import utils.ATeiTree;
import utils.TeiLeaf;

import java.util.List;

public class BaseFormatter extends Formatter {
    final static String FILE_EXTENSION = "";
    final static String PARAGRAPH_SEPARATOR = "";
    public BaseFormatter() {
        super(PARAGRAPH_SEPARATOR, FILE_EXTENSION);
    }

    List<ATeiTree> selectSectionSubtrees(ATeiTree tei) {
        return tei.getTopNodes(t ->  t.getTeiType().equals(ATeiTree.TeiType.P)
                || t.getTeiType().equals(ATeiTree.TeiType.HEAD)
                || t.getTeiType().equals(ATeiTree.TeiType.NOTE)
                || t.getTeiType().equals(ATeiTree.TeiType.FW));
    }

    @Override
    public List<TeiLeaf> format(ATeiTree tei) {
        List<ATeiTree> headAndParagraphTrees = selectSectionSubtrees(tei);
        return getContent(headAndParagraphTrees);
    }

    @Override
    protected TeiLeaf toLeaf(ATeiTree p) {
        String str = getString(p, x -> x, "");
        return TeiLeaf.create(p.getTeiType(), p.getId(), str);
    }
}
