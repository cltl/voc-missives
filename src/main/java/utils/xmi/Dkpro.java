package utils.xmi;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import utils.common.BaseEntity;
import utils.common.BaseToken;
import utils.common.Span;

public class Dkpro {
    static public BaseToken asBaseToken(Token token) {
        return new BaseToken(token.getText(), token.getId(), new Span(token.getBegin(), token.getEnd()));
    }

    public static BaseEntity asBaseEntity(NamedEntity xmiEntity) {
        return new BaseEntity(xmiEntity.getValue(), xmiEntity.getIdentifier(),
                new Span(xmiEntity.getBegin(), xmiEntity.getEnd()));
    }

}
