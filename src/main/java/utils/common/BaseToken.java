package utils.common;


/**
 * Token wrapper for entities.xmi-naf
 */
public class BaseToken {
    String text;
    String id;
    Span span;

    public BaseToken(String token, String id, Span span) {
        this.text = token;
        this.id = id;
        this.span = span;
    }

    public static BaseToken create(String token, String id, String offset, String length) {
        return new BaseToken(token, id, Span.fromCharPosition(offset, length));
    }

    public int getFirstIndex() {
        return span.getFirstIndex();
    }

    public int getLastIndex() {
        return span.getLastIndex();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }
}
