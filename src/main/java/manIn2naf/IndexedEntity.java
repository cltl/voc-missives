package manIn2naf;

import utils.common.Span;

import java.util.Objects;

public class IndexedEntity {
    String token;
    String type;
    int beginIndex;
    int endIndex;

    private IndexedEntity(String token, String type, int beginIndex, int endIndex) {
        this.token = token;
        this.type = type;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    /**
     * creates an indexed entity from Conll input
     * @param labelledToken token/NE-token-label pair, where the NE token label is
     *                      of the form <code>(BI)-LABEL</code>
     * @param offset
     * @return
     */
    protected static IndexedEntity create(String[] labelledToken, int offset) {
        return new IndexedEntity(labelledToken[0],
                labelledToken[1].substring(labelledToken[1].indexOf("-") + 1),
                offset,
                offset + labelledToken[0].length());
    }

    public static IndexedEntity create(String mention, String type, int begin, int end) {
        return new IndexedEntity(mention, type, begin, end);
    }

    public static IndexedEntity create(String mention, String type, Span span) {
        return new IndexedEntity(mention, type, span.getFirstIndex(), span.getEnd());
    }

    public String getLabelledMention() {
        return token + " [" + type + "]";
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public boolean hasSameSpan(IndexedEntity o) {
        return this.beginIndex == o.getBeginIndex() && this.endIndex == o.getEndIndex();
    }

    public boolean isEmbeddedIn(IndexedEntity o) {
        return ! hasSameSpan(o) && this.beginIndex >= o.getBeginIndex() && this.endIndex <= o.getEndIndex();
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, type, beginIndex, endIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        IndexedEntity x = (IndexedEntity) o;
        return token.equals(x.getToken())
                && type.equals(x.getType())
                && beginIndex == x.getBeginIndex()
                && endIndex == x.getEndIndex();
    }

    public boolean overlapsWith(IndexedEntity e) {
        return isEmbeddedIn(e) || e.isEmbeddedIn(this);
    }
}
