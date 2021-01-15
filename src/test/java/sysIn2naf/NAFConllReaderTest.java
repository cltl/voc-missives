package sysIn2naf;

import utils.common.AbnormalProcessException;
import org.junit.jupiter.api.Test;
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

    @Test
    public void testConllIn2Naf() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(refNAF);
        List<String[]> tokens = ncr.conllTokens(inConll);
        List<Entity> entities = ncr.readEntities(tokens);
        ncr.write(entities, fromConllNaf);
        Path p = Paths.get(fromConllNaf);
        assertTrue(Files.exists(p));
    }

}