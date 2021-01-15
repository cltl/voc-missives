package manIn2naf;

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

    public String getType() {
        return type;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }
}
