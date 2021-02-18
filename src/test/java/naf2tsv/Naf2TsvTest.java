package naf2tsv;

import manIn2naf.NafXmiReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.common.IO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Naf2TsvTest {
    static String entitiesNaf = "src/test/resources/tf/missive_9_9_text_man.naf";
    static String refNaf = "src/test/resources/tf/missive_9_9_text.naf";
    static String xmiFile = "src/test/resources/tf/missive_9_9_text.xmi";
    static String outdir = "src/test/resources/tf";

    @BeforeAll
    public static void verifyOrGenerateNAFfiles() throws AbnormalProcessException {
        if (! Files.exists(Paths.get(entitiesNaf))) {
            NafXmiReader nafXmiReader = new NafXmiReader(refNaf, xmiFile);
            nafXmiReader.transferEntities();
            nafXmiReader.write(entitiesNaf);
        }
        if (!Files.exists(Paths.get(outdir))) {
            try {
                Files.createDirectories(Paths.get(outdir));
            } catch (IOException e) {
                throw new AbnormalProcessException("Error creating " + outdir, e);
            }
        }
    }

    @Test
    public void testTsv() throws AbnormalProcessException {
        Naf2Tsv converter = new Naf2Tsv(entitiesNaf, true);
        List<String> lines = converter.getLines();

        String e0Line = lines.get(1) ;
        assertEquals(e0Line, converter.getTFLine("0", "17", "e_t9_9_0", "PER"));
    }

    @Test
    public void testTsvContext() throws AbnormalProcessException {
        Naf2Tsv converter = new Naf2Tsv(entitiesNaf, false);
        List<String> lines = converter.getLines();

        String e0Line = lines.get(1) ;
        assertEquals(e0Line, converter.getContextLine("IX.DIDERIK DURVEN", "0", "17", "PER", "IX.DIDERIK DURVEN, CORNELIS HASSELAAR, WIJ"));

    }
}