package manIn2naf;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import xjc.naf.Wf;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NafXmiReaderTest {
    @Test
    public void testIntegration() throws AbnormalProcessException {
        String xmiFile = "src/test/resources/entity-integration/in.xmi";
        String nafFile = "src/test/resources/entity-integration/ref.naf";

        NafXmiReader nafXmiReader = new NafXmiReader(nafFile, xmiFile);
        List<NamedEntity> xmiEntities = nafXmiReader.getXmi().getEntities();
        List<AlignedEntity> aligned = nafXmiReader.getEntities();
        assertEquals(xmiEntities.size(), aligned.size());

        List<List<Wf>> tokenSpans = nafXmiReader.findOverlappingTokens(aligned);
        assertEquals(tokenSpans.size(), aligned.size());
        assertTrue(tokenSpans.stream().noneMatch(s -> s.isEmpty()));

        nafXmiReader.createEntitiesLayer(tokenSpans, aligned);
        String outFile = "src/test/resources/entity-integration/fromXmi.naf";
        nafXmiReader.write(outFile);
        File out = new File(outFile);
        assertTrue(out.exists());
    }

}