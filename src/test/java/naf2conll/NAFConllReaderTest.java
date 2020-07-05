package naf2conll;

import missives.AbnormalProcessException;
import org.junit.jupiter.api.Test;
import xjc.naf.Entity;
import xjc.naf.References;
import xjc.naf.Target;
import xjc.naf.Wf;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NAFConllReaderTest {
    static String notesNAF = "src/test/resources/naf2conll/text_and_notes.naf";
    String notesConll = "src/test/resources/naf2conll/text_and_notes.notes.conll";

    @Test
    public void testNotesIntegrationDetailed() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(notesNAF, "notes", " ", "manual annotations");

        List<String> conllTokens = ncr.conllTokens(notesConll);
        List<Wf> nafTokens = ncr.getNaf().selectTokens("notes");

        assertEquals(conllTokens.size(), nafTokens.size());
        List<BaseEntity> baseEntities = ncr.collectEntities(nafTokens, conllTokens);
        assertEquals(baseEntities.get(0).getPosition().getLength(), 1);

        // naf entity conversion
        Entity e = baseEntities.get(0).withId(0).asNaf();
        List<Target> ts = ((References) e.getReferencesAndExternalReferences().get(0)).getSpen().get(0).getTargets();
        assertEquals(ts.size(), 1);
    }

    @Test
    public void testNotesIntegration() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(notesNAF, "notes", " ", "manual annotations");
        ncr.read(notesConll);
        List<BaseEntity> entities = ncr.getBaseEntities();
        assertEquals(entities.size(), 3);
        assertEquals(entities.get(0).getType(), "LOC");
        assertEquals(entities.get(0).getId(), "e0");
        assertEquals(entities.get(0).getPosition().getLength(), 1);
    }



}