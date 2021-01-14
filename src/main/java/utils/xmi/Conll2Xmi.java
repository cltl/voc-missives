package utils.xmi;

import conllin2naf.NAFConllReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility class to convert manual annotations in Conll format into Uima Cas Xmi.
 * The raw text for the xmi is extracted by assuming a single space between Conll tokens.
 * The Xmi further only contains entities, as it is intended to be processed for
 * the integration of these entities into a reference NAF file.
 */
public class Conll2Xmi {
    CasDoc xmi;
    List<String[]> conllTokens;
    public static final Logger logger = LogManager.getLogger(Conll2Xmi.class);

    public Conll2Xmi(String inputConll) throws AbnormalProcessException {
        this.xmi = CasDoc.create();
        this.conllTokens = NAFConllReader.conllTokens(inputConll);
    }

    public Conll2Xmi(List<String[]> conllTokens) throws AbnormalProcessException {
        this.conllTokens = conllTokens;
        this.xmi = CasDoc.create();
    }

    public static Conll2Xmi create(List<String[]> conllTokens) throws AbnormalProcessException {
        return new Conll2Xmi(conllTokens);
    }

    public void convert() {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        LinkedList<IndexedEntity> entityTokens = new LinkedList<>();
        for (String[] labelledToken: conllTokens) {
            if (startsEntity(labelledToken)) {
                entityTokens = flush(entityTokens);
                entityTokens.add(IndexedEntity.create(labelledToken, i));
            } else if (isInEntity(labelledToken)) {
                entityTokens.add(IndexedEntity.create(labelledToken, i));
            } else
                entityTokens = flush(entityTokens);
            sb.append(labelledToken[0]).append(' ');
            i += labelledToken[0].length() + 1;
        }
        flush(entityTokens);
        sb.deleteCharAt(sb.length() - 1);
        xmi.addRawText(sb.toString());
    }

    public CasDoc getXmi() {
        return xmi;
    }

    private boolean isInEntity(String[] labelledToken) {
        return labelledToken[1].startsWith("I");
    }

    private boolean startsEntity(String[] labelledToken) {
        return labelledToken[1].startsWith("B");
    }

    private LinkedList<IndexedEntity> flush(LinkedList<IndexedEntity> entityTokens) {
        if (! entityTokens.isEmpty()) {
            xmi.addEntity(
                    entityTokens.getFirst().getBeginIndex(),
                    entityTokens.getLast().getEndIndex(),
                    entityTokens.getFirst().getType());
            entityTokens = new LinkedList<>();
        }
        return entityTokens;
    }

}
