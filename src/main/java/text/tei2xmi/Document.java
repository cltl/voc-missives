package text.tei2xmi;

import tokens.Tokenizer;
import utils.tei.ATeiTree;
import utils.tei.Metadata;
import utils.xmi.Segments;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Document {
    Formatter formatter;
    List<Paragraph> paragraphs;
    Metadata metadata;

    public Document(Formatter formatter, Metadata metadata) {
        this.formatter = formatter;
        this.metadata = metadata;
        this.paragraphs = new LinkedList<>();
    }

    public static Document create(Formatter formatter, Metadata metadata) {
        return new Document(formatter, metadata);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public int sentenceCount() {
        return getSentences().getSegments().size();
    }

    public int tokenCount() {
        return getTokens().getSegments().size();
    }

    public boolean isEmpty() {
        return paragraphs.isEmpty();
    }

    public void segmentAndTokenize(Tokenizer tokenizer) {
        for (Paragraph p: paragraphs)
            p.tokenize(tokenizer, sentenceCount(), tokenCount());
    }

    public String typedFileName(String prefix) {
        return prefix + formatter.getFileExtension();
    }

    public String getRawText() {
        return paragraphs.stream().map(p -> p.getContent()).collect(Collectors.joining(formatter.getParagraphSeparator()));
    }

    public List<Paragraph> getParagraphs() {
        return paragraphs;
    }

    public Segments getSentences() {
        return Segments.join(paragraphs.stream().map(p -> p.getSentences()).collect(Collectors.toList()));
    }

    public Segments getTokens() {
        return Segments.join(paragraphs.stream().map(p -> p.getTokens()).collect(Collectors.toList()));
    }

    public void formatParagraphs(ATeiTree aTeiTree) {
        this.paragraphs = formatter.formatParagraphs(aTeiTree);
    }
}
