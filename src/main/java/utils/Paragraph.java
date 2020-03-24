package utils;


public class Paragraph {
    Segments sentences;
    Segments tokens;
    String content;
    String teiId;
    int offset;

    public Paragraph(TeiLeaf paragraph, int offset) {
        this.content = paragraph.getContent();
        this.teiId = paragraph.getId();
        this.offset = offset;
        this.sentences = new Segments();
        this.tokens = new Segments();
    }


    public static Paragraph create(TeiLeaf paragraph, int offset) {
        return new Paragraph(paragraph, offset);
    }

    public String getContent() {
        return content;
    }

    public String getTeiId() {
        return teiId;
    }

    public Segments getTokens() {
        return tokens;
    }

    public Segments getSentences() {
        return sentences;
    }

    public int getOffset() {
        return offset;
    }


}
