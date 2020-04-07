package tei2xmi;


import utils.ATeiTree;
import utils.Paragraph;
import utils.TeiLeaf;
import utils.TeiTree;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
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

    protected void cleanStrings(List<ATeiTree> paragraphs) {
        Consumer<ATeiTree> cleanLeaf = s -> {
            if (s instanceof TeiLeaf)
                ((TeiLeaf) s).getContent().replace("\n", "");
        };
        paragraphs.forEach(p -> p.accept(cleanLeaf));
    }

    protected boolean isParagraph(ATeiTree t) {
        return t.getTeiType().equals(ATeiTree.TeiType.HEAD) || t.getTeiType().equals(ATeiTree.TeiType.P);
    }

    protected List<TeiLeaf> flatten(List<ATeiTree> paragraphs) {
        return paragraphs.stream().map(p -> createParagraph(p)).collect(Collectors.toList());
    }

    protected TeiLeaf createParagraph(ATeiTree p) {
        Function<ATeiTree, String> getCellContent = t -> {
            if (t instanceof TeiTree)
                return ((TeiTree) t).getContent();
            if (t instanceof TeiLeaf)
                return ((TeiLeaf) t).getContent();
            return "";
        };
        String str = p.collect(t -> t instanceof TeiLeaf || t instanceof TeiTree).stream()
                .map(getCellContent).collect(Collectors.joining());
        return TeiLeaf.create(p.getTeiType(), p.getId(), str);
    }

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
