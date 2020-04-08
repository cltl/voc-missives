package tei2xmi;


import utils.ATeiTree;
import utils.Paragraph;
import utils.TeiLeaf;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lists paragraph-like elements.
 */
public abstract class Formatter {
    public static final String PARA_SEP = "\n";
    String fileExtension;

    public Formatter(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public abstract List<TeiLeaf> format(ATeiTree tei);


    protected String getString(ATeiTree tree, Function<String, String> leafFct, String sep) {
        return tree.getAllNodes(t -> t.getTeiType().equals(ATeiTree.TeiType.STR)).stream()
                .map(n -> leafFct.apply(((TeiLeaf) n).getContent())).collect(Collectors.joining(sep));
    }

    protected List<TeiLeaf> getContent(List<ATeiTree> paragraphs) {
        return paragraphs.stream().map(p -> toLeaf(p)).filter(p -> ! p.getContent().equals("")).collect(Collectors.toList());
    }

    protected abstract TeiLeaf toLeaf(ATeiTree p);

    public List<Paragraph> formatParagraphs(ATeiTree tree) {
        List<Paragraph> paragraphs = new LinkedList<>();
        int offset = 0;
        for (TeiLeaf p: format(tree)) {
            Paragraph paragraph = Paragraph.create(p, offset);
            paragraphs.add(paragraph);
            offset += p.getContent().length() + PARA_SEP.length();
        }
        return paragraphs;
    }

}
