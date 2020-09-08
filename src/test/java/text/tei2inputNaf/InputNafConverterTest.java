package text.tei2inputNaf;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.common.Fragment;
import utils.naf.NafUnits;
import utils.tei.Metadata;
import utils.tei.TeiReader;
import xjc.naf.Tunit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InputNafConverterTest {
    @Test
    public void testConversionTextAndNotes() throws AbnormalProcessException {
        String textFile = "src/test/resources/tei-xml/text_and_notes.xml";
        InputNafConverter converter = new InputNafConverter();

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
        String teiFile = "src/test/resources/tei-xml/text_and_notes.xml";
        TeiReader teiReader = new TeiReader(teiFile, x -> TeiInputTreeFactory.create(x));
        Metadata metadata = teiReader.getMetadata();
        String baseid = "INT_0aff566f-8c02-332d-971d-eb572c33f86b";
        StringBuilder sb = new StringBuilder();
        sb.append(baseid).append("_");
        sb.append(metadata.getCollectionId()).append("_");
        sb.append("p583");
        assertEquals(metadata.getDocumentId(), sb.toString());
    }
}