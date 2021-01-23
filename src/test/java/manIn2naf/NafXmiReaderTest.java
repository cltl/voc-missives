package manIn2naf;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import xjc.naf.Entity;
import utils.naf.Wf;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NafXmiReaderTest {
    @Test
    public void testIntegration() throws AbnormalProcessException {
        String xmiFile = "src/test/resources/entity-integration/in.xmi";
        String nafFile = "src/test/resources/entity-integration/ref.naf";

        NafXmiReader nafXmiReader = NafXmiReader.createTeiTextReader(nafFile, xmiFile);
        List<NamedEntity> xmiEntities = nafXmiReader.getXmi().getEntities();
        EntityAlignerTei entityAlignerTei = (EntityAlignerTei) nafXmiReader.getEntityAligner();
        List<AlignedEntity> aligned = entityAlignerTei.getEntities(nafXmiReader.getRawNafText());
        assertEquals(xmiEntities.size(), aligned.size());

        List<List<Wf>> tokenSpans = entityAlignerTei.findOverlappingTokens(aligned);
        assertEquals(tokenSpans.size(), aligned.size());
        assertTrue(tokenSpans.stream().noneMatch(s -> s.isEmpty()));

        List<Entity> entities = entityAlignerTei.createNafEntities(tokenSpans, aligned);
        nafXmiReader.createEntitiesLayer(entities);
        String outFile = "src/test/resources/entity-integration/fromXmi.naf";
        nafXmiReader.write(outFile);
        File out = new File(outFile);
        assertTrue(out.exists());
    }

    @Test
    public void testTFentityMapping() throws AbnormalProcessException {
        String xmiFile = "src/test/resources/tf/missive_9_9_text.xmi";
        String nafFile = "src/test/resources/tf/missive_9_9_text.naf";
        NafXmiReader nafXmiReader = NafXmiReader.createTeiTextReader(nafFile, xmiFile);
        List<NamedEntity> xmiEntities = nafXmiReader.getXmi().getEntities();
        EntityAlignerTei entityAlignerTei = (EntityAlignerTei) nafXmiReader.getEntityAligner();
        List<AlignedEntity> aligned = entityAlignerTei.getEntities(nafXmiReader.getRawNafText());
        assertTrue(aligned.size() < xmiEntities.size() + 10
                && aligned.size() > xmiEntities.size() - 10);
        List<List<Wf>> tokenSpans = entityAlignerTei.findOverlappingTokens(aligned);
        assertEquals(tokenSpans.size(), aligned.size());
        assertTrue(tokenSpans.stream().noneMatch(s -> s.isEmpty()));

        List<Entity> entities = entityAlignerTei.createNafEntities(tokenSpans, aligned);
        nafXmiReader.createEntitiesLayer(entities);
        String outFile = "src/test/resources/out/missive_9_9_text_man.naf";
        nafXmiReader.write(outFile);
        File out = new File(outFile);
        assertTrue(out.exists());
    }


}