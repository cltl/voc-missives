package naf2conll;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import tei2naf.CharPosition;
import xjc.naf.Wf;

/**
 * Token wrapper for xmi-naf
 */
public class BaseToken {
    String text;
    String id;
    CharPosition position;

    public BaseToken(String token, String id, CharPosition position) {
        this.text = token;
        this.id = id;
        this.position = position;
    }

    public int getBegin() {
        return position.getOffset();
    }

    static public BaseToken create(Token token) {
        // TODO check token.getEnd(). is that the end index or the last char index?
        return new BaseToken(token.getText(), token.getId(), new CharPosition(token.getBegin(), token.getEnd() - token.getBegin() + 1));
    }

    static public BaseToken create(Wf token) {
        return new BaseToken(token.getContent(), token.getId(), new CharPosition(token.getOffset(), token.getLength()));
    }

    /**
     * TODO last index?
     * @return
     */
    public int getEnd() {
        return position.getEndIndex() - 1;
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

    public CharPosition getPosition() {
        return position;
    }

    public void setPosition(CharPosition position) {
        this.position = position;
    }
}
