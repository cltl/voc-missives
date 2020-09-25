package entities.entityIntegration;

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
        String xmiFile = "src/test/resources/entityIntegration/INT_08c82040-752f-3fc2-ad50-0d3e7b37a945_notes.xmi";
        String nafFile = "src/test/resources/entityIntegration/vol1_p0106_INT_08c82040-752f-3fc2-ad50-0d3e7b37a945_notes.naf";
        NafXmiReader nafXmiReader = new NafXmiReader(nafFile, xmiFile);
        List<NamedEntity> xmiEntities = nafXmiReader.getXmi().getEntities();
        List<AlignedEntity> aligned = nafXmiReader.getEntities();
        assertEquals(xmiEntities.size(), aligned.size());

        List<List<Wf>> tokenSpans = nafXmiReader.findOverlappingTokens(aligned);
        assertEquals(tokenSpans.size(), aligned.size());
        assertTrue(tokenSpans.stream().noneMatch(s -> s.isEmpty()));

        nafXmiReader.createEntitiesLayer(tokenSpans, aligned);
        String outFile = "src/test/resources/entityIntegration/out.naf";
        nafXmiReader.write(outFile);
        File out = new File(outFile);
        assertTrue(out.exists());
    }

}