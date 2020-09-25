package deprecated.tei2xmi;


import utils.tei.ATeiTree;
import utils.tei.TeiLeaf;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lists paragraph-like elements.
 */
@Deprecated
public abstract class Formatter {
    String paragraphSeparator;
    String fileExtension;

    public Formatter(String paragraphSeparator, String fileExtension) {
        this.paragraphSeparator = paragraphSeparator;
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public abstract List<TeiLeaf> format(ATeiTree tei);

    public String getParagraphSeparator() {
        return paragraphSeparator;
    }

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
            offset += p.getContent().length() + paragraphSeparator.length();
        }
        return paragraphs;
    }



}
