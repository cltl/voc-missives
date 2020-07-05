package text.tei2xmi;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.tei.TeiLeaf;
import utils.tei.TeiReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteFormatterTest {
    @Test
    public void testNotes() throws AbnormalProcessException {
        String input = "src/test/resources/tei-xml/text_and_notes.xml";
        String note1 = "(Afsluiting contract met den Koning van Ternate, af gedrukt Corpus II, nr. CGIV; zending van 3 fluitschepen met cargasoen van f. 73809.15.5 naar Makassar;";
        String note2 = "1) NI. dat van Verstegen.";
        String note3 = "2) Priserende, schattende, de waarde bepalende.";
        NoteFormatter formatter = new NoteFormatter();
        TeiReader teiReader = new TeiReader(input, x -> TeiTreeFactory.create(x));
        List<TeiLeaf> notes = formatter.format(teiReader.getTeiTree());
        assertEquals(notes.size(), 3);
        assertEquals(notes.get(0).getContent(), note1);
        assertEquals(notes.get(1).getContent(), note2);
        assertEquals(notes.get(2).getContent(), note3);
    }

}