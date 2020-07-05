package tei2xmi;

import org.junit.jupiter.api.Test;
import missives.AbnormalProcessException;
import utils.TeiLeaf;
import utils.TeiTreeFactory;
import xjc.teiAll.TEI;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteFormatterTest {
    @Test
    public void testNotes() {
        String input = "src/test/resources/tei-xml/text_and_notes.xml";
        String note1 = "(Afsluiting contract met den Koning van Ternate, af gedrukt Corpus II, nr. CGIV; zending van 3 fluitschepen met cargasoen van f. 73809.15.5 naar Makassar;";
        String note2 = "1) NI. dat van Verstegen.";
        String note3 = "2) Priserende, schattende, de waarde bepalende.";
        NoteFormatter formatter = new NoteFormatter();
        TEI tei = null;
        try {
            tei = Converter.load(input);
        } catch (AbnormalProcessException e) {
            e.printStackTrace();
        }
        List<TeiLeaf> notes = formatter.format(TeiTreeFactory.create(tei));
        assertEquals(notes.size(), 3);
        assertEquals(notes.get(0).getContent(), note1);
        assertEquals(notes.get(1).getContent(), note2);
        assertEquals(notes.get(2).getContent(), note3);
    }

}