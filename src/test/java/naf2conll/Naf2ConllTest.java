package naf2conll;

import missives.AbnormalProcessException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tei2naf.NafConverter;
import xjc.naf.Wf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Naf2ConllTest {
    static String indexNAF = "src/test/resources/naf-tok/index_persons.naf";
    static String tableNAF = "src/test/resources/naf-tok/table.naf";
    static String notesNAF = "src/test/resources/naf-tok/text_and_notes.naf";

    @BeforeAll
    public static void verifyOrGenerateNAFfiles() throws IOException, AbnormalProcessException {
        String[] files = new String[]{indexNAF, tableNAF, notesNAF};
        String nafDir = "src/test/resources/naf-tok/";
        if (! Files.exists(Paths.get(nafDir)))
            Files.createDirectories(Paths.get(nafDir));
        for (String f: files) {
            Path p = Paths.get(f);
            if (!Files.exists(p))
                NafConverter.convertFile(p, nafDir, true);
        }
    }

    @Test
    public void testSelectionInTextAndNotes() {
        Naf2Conll converter = new Naf2Conll(" ", "all", notesNAF);
        List<Wf> allTokens = converter.selectedTokens();

        converter = new Naf2Conll(" ", "mixed", notesNAF);
        List<Wf> textAndNotesTokens = converter.selectedTokens();

        converter = new Naf2Conll(" ", "text", notesNAF);
        List<Wf> textTokens = converter.selectedTokens();

        converter = new Naf2Conll(" ", "notes", notesNAF);
        List<Wf> notesTokens = converter.selectedTokens();

        assertTrue(allTokens.size() > textAndNotesTokens.size());
        assertEquals(textAndNotesTokens.size(), textTokens.size() + notesTokens.size());
    }

    @Test
    public void testEmptySelection() {
        Naf2Conll converter = new Naf2Conll(" ", "all", tableNAF);
        List<Wf> allTokens = converter.selectedTokens();

        converter = new Naf2Conll(" ", "mixed", tableNAF);
        List<Wf> textAndNotesTokens = converter.selectedTokens();

        converter = new Naf2Conll(" ", "text", tableNAF);
        List<Wf> textTokens = converter.selectedTokens();

        converter = new Naf2Conll(" ", "notes", tableNAF);
        List<Wf> notesTokens = converter.selectedTokens();

        assertEquals(allTokens.size(), textTokens.size());
        assertEquals(textAndNotesTokens.size(), textTokens.size());
        assertEquals(notesTokens.size(), 0);
    }
}