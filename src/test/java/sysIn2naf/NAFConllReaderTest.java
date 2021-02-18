package sysIn2naf;

import utils.common.AbnormalProcessException;
import org.junit.jupiter.api.Test;
import utils.naf.BaseEntity;
import xjc.naf.Entity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NAFConllReaderTest {
    static String refNAF = "src/test/resources/entity-integration/ref.naf";
    static String inConll = "src/test/resources/entity-integration/train.conll";
    static String fromConllNaf = "src/test/resources/entity-integration/fromConll.naf";
    static String bareConll = "src/test/resources/entity-integration/test.conll";

    @Test
    public void testConllIn2Naf() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(refNAF);
        List<String[]> tokens = ncr.conllTokens(inConll);
        List<Entity> entities = ncr.readEntities(tokens);
        ncr.write(entities, fromConllNaf);
        Path p = Paths.get(fromConllNaf);
        assertTrue(Files.exists(p));
    }

    @Test
    public void testDifferences() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(fromConllNaf);
        List<Entity> newEntities = ncr.readEntities(NAFConllReader.conllTokens(bareConll));
        assertTrue(newEntities.isEmpty());
        List<Entity> currentEntities = ncr.getNaf().getEntities();
        List<BaseEntity> diff = ncr.compareToExisting(newEntities);
        // entities differ by their label -> they are all removed
        assertEquals(diff.size(), currentEntities.size());
    }

}