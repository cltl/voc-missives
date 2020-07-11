package entities.nafReference;

import utils.common.AbnormalProcessException;
import org.junit.jupiter.api.Test;
import utils.common.BaseEntity;
import utils.common.BaseToken;
import utils.naf.NafUnits;
import xjc.naf.Entity;
import xjc.naf.References;
import xjc.naf.Target;
import xjc.naf.Wf;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NAFConllReaderTest {
    static String notesNAF = "src/test/resources/entities/text_and_notes.naf";
    String notesConll = "src/test/resources/entities/text_and_notes.notes.conll";

    @Test
    public void testNotesIntegrationDetailed() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(notesNAF, "notes", " ", "manual annotations");


        List<BaseToken> conllTokens = ncr.read(notesNAF);
        List<Wf> nafTokens = ncr.selectedTokens(ncr.getNaf(), "notes");
        assertEquals(conllTokens.size(), nafTokens.size());

        List<BaseEntity> baseEntities = ncr.readEntities(notesNAF);
        assertEquals(baseEntities.get(0).getTokenSpan().getLength(), 1);

        // naf entity conversion
        Entity e = NafUnits.asNafEntity(baseEntities.get(0).withId(0), ncr.getNaf());
        List<Target> ts = ((References) e.getReferencesAndExternalReferences().get(0)).getSpen().get(0).getTargets();
        assertEquals(ts.size(), 1);
    }

    @Test
    public void testNotesIntegration() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(notesNAF, "notes", " ", "manual annotations");
        List<BaseEntity> entities = ncr.readEntities(notesConll);
        assertEquals(entities.size(), 3);
        assertEquals(entities.get(0).getType(), "LOC");
        assertEquals(entities.get(0).getTokenSpan().getLength(), 1);
        assertEquals(entities.get(0).getId(), "");
        BaseEntity entity0 = entities.get(0).withId(0);
        assertEquals(entity0.getId(), "e0");

    }





}