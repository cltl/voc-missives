package utils.xmi;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import utils.common.BaseToken;
import utils.common.Span;


public class Dkpro {
    static public BaseToken asBaseToken(Token token) {
        return new BaseToken(token.getText(), token.getId(), new Span(token.getBegin(), token.getEnd()));
    }

}
