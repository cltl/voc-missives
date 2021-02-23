package naf2conll;

import manIn2naf.NafXmiReader;
import utils.common.AbnormalProcessException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import utils.naf.NafHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Naf2ConllTest {
    static String entitiesNaf = "src/test/resources/entity-integration/ref-entities.naf";
    static String refNaf = "src/test/resources/entity-integration/ref.naf";
    static String testConll = "src/test/resources/entity-integration/test.conll";
    static String trainConll = "src/test/resources/entity-integration/train.conll";

    @BeforeAll
    public static void verifyOrGenerateNAFfiles() throws AbnormalProcessException {
        if (! Files.exists(Paths.get(entitiesNaf))) {
            String xmiFile = "src/test/resources/entity-integration/in.xmi";
            NafXmiReader nafXmiReader = new NafXmiReader(refNaf, xmiFile);
            nafXmiReader.transferEntities();
            nafXmiReader.write(entitiesNaf);
        }
    }


    @Test
    public void testEmptySelection() throws AbnormalProcessException {
        Naf2Conll converter = new Naf2Conll(refNaf);

        converter.filterEntities();
        assertTrue(converter.getEntities().isEmpty());
        converter.write(testConll);
        Path p = Paths.get(testConll);
        assertTrue(Files.exists(p));

    }

    @Test
    public void testEntitiesNaf2Conlll() throws AbnormalProcessException {
        Naf2Conll converter = new Naf2Conll(entitiesNaf);

        converter.filterEntities();
        assertFalse(converter.getEntities().isEmpty());
        int nafEntities = NafHandler.create(entitiesNaf).getEntities().size();
        assertEquals(nafEntities, converter.getGpeCount() + converter.getEmbeddedEntityCount() + converter.getEntityCount());

        converter.write(trainConll);
        Path p = Paths.get(trainConll);
        assertTrue(Files.exists(p));
    }
}