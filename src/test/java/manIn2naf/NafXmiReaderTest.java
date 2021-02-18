package manIn2naf;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import xjc.naf.Entity;
import utils.naf.Wf;

import java.io.File;
import java.nio.file.Paths;
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

        List<Entity> entities = entityAlignerTei.createNafEntities(tokenSpans, aligned, "e");
        nafXmiReader.createEntitiesLayer(entities);
        String outFile = "src/test/resources/entity-integration/fromXmi.naf";
        nafXmiReader.write(outFile);
        File out = new File(outFile);
        assertTrue(out.exists());
    }

    @Test
    public void testTFentityMapping() throws AbnormalProcessException {
        int v = 9;
        int l = 9;
        String datadir = "src/test/resources/tf";
        String xmiFile = Paths.get(datadir, "missive_" + v + "_" + l + "_text.xmi").toString();
        String nafFile = Paths.get(datadir, "missive_" + v + "_" + l + "_text.naf").toString();
        NafXmiReader nafXmiReader = NafXmiReader.createTeiTextReader(nafFile, xmiFile);
        List<NamedEntity> xmiEntities = nafXmiReader.getXmi().getEntities();
        EntityAlignerTei entityAlignerTei = (EntityAlignerTei) nafXmiReader.getEntityAligner();
        List<AlignedEntity> aligned = entityAlignerTei.getEntities(nafXmiReader.getRawNafText());
        assertTrue(aligned.size() < xmiEntities.size() + 10
                && aligned.size() > xmiEntities.size() - 10);
        List<List<Wf>> tokenSpans = entityAlignerTei.findOverlappingTokens(aligned);
        assertEquals(tokenSpans.size(), aligned.size());
        assertTrue(tokenSpans.stream().noneMatch(s -> s.isEmpty()));

        List<Entity> entities = entityAlignerTei.createNafEntities(tokenSpans, aligned, "e_t_9_9_");
        nafXmiReader.createEntitiesLayer(entities);
        String outFile = "src/test/resources/tf/missive_" + v + "_" + l + "_text_man.naf";
        nafXmiReader.write(outFile);
        File out = new File(outFile);
        assertTrue(out.exists());
    }


}