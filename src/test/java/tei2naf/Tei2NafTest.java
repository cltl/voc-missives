package tei2naf;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.naf.Fragment;
import utils.naf.NafUnits;
import utils.tei.Metadata;
import utils.tei.TeiReader;
import xjc.naf.Tunit;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Tei2NafTest {
    @Test
    public void testConversionTextAndNotes() throws AbnormalProcessException {
        String textFile = "src/test/resources/tei2naf/text_and_notes.xml";
        Tei2Naf converter = new Tei2Naf();

        List<Fragment> sections = converter.getFragments(textFile);
        int lastOffset = 0;
        for (Fragment f: sections) {
            System.out.println(f);
            assertTrue(f.getOffset() >= lastOffset);
            lastOffset = f.getOffset();
        }
        String cr = "\n";
        assertEquals(cr.length(), 1);
    }

    @Test
    public void testXpathCreation() {
        Fragment f = new Fragment("INT_ca46b627-a2e3-35b9-84ed-5dd7134d1178.TEI.1.text.1.body.1.div.1.div.1.table.10.row.12.cell.1", 0, 10);
        Tunit t = NafUnits.asTunit(f);
        assertEquals(t.getXpath(), "/TEI/text[1]/body[1]/div[1]/div[1]/table[10]/row[12]/cell[1]");
    }

    @Test
    public void testMetadata() throws AbnormalProcessException {
        String teiFile = "src/test/resources/tei2naf/text_and_notes.xml";
        TeiReader teiReader = new TeiReader(teiFile, x -> TeiInputTreeFactory.create(x));
        Metadata metadata = teiReader.getMetadata();
        String baseid = "INT_0aff566f-8c02-332d-971d-eb572c33f86b";
        String volume = metadata.getCollectionId();

        StringBuilder sb = new StringBuilder();
        sb.append(volume.substring(volume.indexOf(":") + 1)).append("_");
        sb.append("p0583");
        sb.append("_").append(baseid);
        assertEquals(metadata.getDocumentId(), sb.toString());
    }

    @Test
    public void testConversion() throws AbnormalProcessException {
        String teiFile = "src/test/resources/tei2naf/text_and_notes.xml";
        Tei2Naf converter = new Tei2Naf();
        String outdir = "src/test/resources/tei2naf/";
        converter.process(teiFile, outdir);
        String id = "vol2_p0583_INT_0aff566f-8c02-332d-971d-eb572c33f86b";
        File out = Paths.get(outdir, id + ".naf").toFile();
        assertTrue(out.exists());
    }
}