package conllIn2naf;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


class WfAlignerTest {
    static String fromConllNaf = "src/test/resources/entity-integration/fromConll.naf";
    static String modConll = "src/test/resources/entity-integration/mod.conll";

    @Test
    public void testAlignment() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(fromConllNaf, false, false);
        List<String[]> conllLines = NAFConllReader.conllLines(modConll);
        WfAligner wfAligner = new WfAligner(ncr.getNaf().getWfs(), NAFConllReader.conllTokens(conllLines));
        assertTrue(wfAligner.getAlignments().stream().noneMatch(a -> a.wfCount() == 0 || a.tokenCount() == 0));

        assertTrue(wfAligner.indexMap().stream().noneMatch(i -> i.isEmpty()));
    }

}