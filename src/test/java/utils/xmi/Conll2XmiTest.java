package utils.xmi;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import manIn2naf.Conll2Xmi;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Conll2XmiTest {

    private List<String[]> conllTokens() {
        List<String[]> conllTokens = new LinkedList<>();
        conllTokens.add(new String[]{"a", "B-ORG"});
        conllTokens.add(new String[]{"b", "B-PER"});
        conllTokens.add(new String[]{"c", "I-PER"});
        conllTokens.add(new String[]{"d", "O"});
        conllTokens.add(new String[]{"e", "B-LOC"});
        return conllTokens;
    }

    @Test
    public void testConversionIndices() throws AbnormalProcessException {
        Conll2Xmi converter = Conll2Xmi.create(conllTokens());
        converter.convert();
        assertEquals(converter.getXmi().getRawText(), "a b c d e");
        List<NamedEntity> entities = converter.getXmi().getEntities();
        assertEquals(converter.getXmi().getEntities().size(), 3);
        assertEquals(entities.get(0).getBegin(), 0);
        assertEquals(entities.get(0).getEnd(), 1);
        assertEquals(entities.get(0).getCoveredText(), "a");
        assertEquals(entities.get(0).getValue(), "ORG");
        assertEquals(entities.get(1).getBegin(), 2);
        assertEquals(entities.get(1).getEnd(), 5);
        assertEquals(entities.get(1).getCoveredText(), "b c");
        assertEquals(entities.get(1).getValue(), "PER");
        assertEquals(entities.get(2).getBegin(), 8);
        assertEquals(entities.get(2).getEnd(), 9);
        assertEquals(entities.get(2).getCoveredText(), "e");
        assertEquals(entities.get(2).getValue(), "LOC");
    }
}