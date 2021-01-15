package nafSelector;

import javafx.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tei2naf.Tei2Naf;
import utils.common.AbnormalProcessException;
import utils.naf.NafDoc;
import xjc.naf.LinguisticProcessors;
import xjc.naf.Tunit;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NafUnitSelectorTest {
    static List<Tunit> elts;
    private final String testdirIn = "src/test/resources/tei2naf";
    private final String testdirOut = "src/test/resources/naf-selector";
    private final String filePfx = "vol2_p0583_INT_0aff566f-8c02-332d-971d-eb572c33f86b";

    public static Tunit createTunit(String id, String type, int offset, int length) {
        Tunit tunit = new Tunit();
        tunit.setLength(length + "");
        tunit.setOffset(offset + "");
        tunit.setType(type);
        tunit.setId(id);
        tunit.setXpath(id + type);
        return tunit;
    }

    @BeforeAll
    public static void createTunits() {
        elts = new LinkedList<>();
        elts.add(createTunit("A", "text", 0, 11));
        elts.add(createTunit("B", "div", 0, 11));
        elts.add(createTunit("C", "fw", 1, 1));
        elts.add(createTunit("D", "note", 2, 5));
        elts.add(createTunit("E", "p", 5, 1));
        elts.add(createTunit("F", "note", 8, 3));
        elts.add(createTunit("G", "p", 9, 2));
        elts.add(createTunit("H", "fw", 10, 1));
    }

    @Test
    public void testTreeStructure() {
        TunitTree root = TunitTree.create(Collections.singletonList(elts.get(0)));
        assertTrue(root.dominates(createTunit("F", "note", 8, 3)));

        TunitTree tree = TunitTree.create(elts);
        assertEquals(tree.toString(),
                "[A [B [C] [D [E]] [F [G [H]]]]]");
    }

    @Test
    public void testFwExcisionInNotes() {

        TunitTree tree = TunitTree.create(elts);

        List<Tunit> notes = NafUnitSelector.filterNotes(tree);
        assertEquals(notes.size(), 5);
        assertEquals(notes.get(0).getId(), "D.co2-3");
        assertEquals(notes.get(1).getId(), "E.co5-1");
        assertEquals(notes.get(2).getId(), "D.co6-1");
        assertEquals(notes.get(4).getId(), "G.co9-1");
    }

    @Test
    public void testNotesAndFwExcisionTextExtraction() {
        TunitTree tree = TunitTree.create(elts);
        assertEquals(tree.getChildren().size(), 1);
        List<Tunit> text = NafUnitSelector.filterText(tree);
        assertEquals(text.size(), 2);
        assertEquals(text.get(0).getId(), "B.co0-1");
        assertEquals(text.get(1).getId(), "B.co7-1");

    }

    @Test
    public void testSpanUpdate() {
        TunitTree tree = TunitTree.create(elts);
        List<Tunit> text = NafUnitSelector.filterText(tree);
        assertEquals(text.size(), 2);
        assertEquals(text.get(0).getOffset(), "0");
        assertEquals(text.get(0).getLength(), "1");
        assertEquals(text.get(1).getOffset(), "7");
        assertEquals(text.get(1).getLength(), "1");

        text = NafUnitSelector.updateSpans(text);
        assertEquals(text.get(0).getOffset(), "0");
        assertEquals(text.get(0).getLength(), "1");
        assertEquals(text.get(1).getOffset(), "1");
        assertEquals(text.get(1).getLength(), "1");

        tree = TunitTree.create(elts);

        List<Tunit> notes = NafUnitSelector.filterNotes(tree);
        assertEquals(notes.size(), 5);
        notes = NafUnitSelector.updateSpans(notes);
        assertEquals(notes.get(0).getOffset(), "0");
        assertEquals(notes.get(0).getLength(), "3");
        assertEquals(notes.get(1).getOffset(), "3");
        assertEquals(notes.get(1).getLength(), "1");
        assertEquals(notes.get(2).getOffset(), "4");
        assertEquals(notes.get(1).getLength(), "1");
        assertEquals(notes.get(3).getOffset(), "5");
        assertEquals(notes.get(1).getLength(), "1");
        assertEquals(notes.get(4).getOffset(), "6");
        assertEquals(notes.get(1).getLength(), "1");
    }

    @Test
    public void testRawTextDerivation() {
        String rawText = "dfnnnpndnpf";
        TunitTree tree = TunitTree.create(elts);
        List<Tunit> text = NafUnitSelector.filterText(tree);
        String derivedRawText = NafUnitSelector.deriveRawText(text, rawText);
        assertEquals(derivedRawText, "dd");

        tree = TunitTree.create(elts);
        List<Tunit> notes = NafUnitSelector.filterNotes(tree);
        derivedRawText = NafUnitSelector.deriveRawText(notes, rawText);
        assertEquals(derivedRawText, "nnnpnnp");
    }


    @Test
    public void testFullTextDerivation() {
        String rawText = "dfnnnpndnpf";
        TunitTree tree = TunitTree.create(elts);
        List<Tunit> allUnits = NafUnitSelector.filterAll(tree);
        assertEquals(allUnits.size(), 9);
        String unitsText = NafUnitSelector.deriveRawText(allUnits, rawText);
        assertEquals(unitsText, rawText);
    }



    @Test
    public void testDerivedNafCreationNotes() throws AbnormalProcessException {
        NafUnitSelector nus = new NafUnitSelector("notes");
        NafDoc outNaf = nus.getDerivedNaf(NafDoc.create(getTestInputNaf()));
        outNaf.write(Paths.get(testdirOut, outNaf.getFileName()).toString());
        File out = Paths.get(testdirOut, filePfx + "_notes.naf").toFile();
        assertTrue(out.exists());
    }

    @Test
    public void testDerivedNafCreationText() throws AbnormalProcessException {
        NafUnitSelector nus = new NafUnitSelector("text");
        NafDoc outNaf = nus.getDerivedNaf(NafDoc.create(getTestInputNaf()));
        outNaf.write(Paths.get(testdirOut, outNaf.getFileName()).toString());
        File out = Paths.get(testdirOut, filePfx + "_text.naf").toFile();
        assertTrue(out.exists());
    }

    @Test
    public void testDerivedNafCreationAll() throws AbnormalProcessException {
        NafDoc inputNaf = NafDoc.create(getTestInputNaf());
        NafUnitSelector nus = new NafUnitSelector("all");
        NafDoc outNaf = nus.getDerivedNaf(inputNaf);
        assertEquals(inputNaf.getRawText(), outNaf.getRawText());
        outNaf.write(Paths.get(testdirOut, outNaf.getFileName()).toString());
        File out = Paths.get(testdirOut, filePfx + "_all.naf").toFile();
        assertTrue(out.exists());
    }

    private String getTestInputNaf() throws AbnormalProcessException {
        String inNaf = "src/test/resources/tei2naf/vol2_p0583_INT_0aff566f-8c02-332d-971d-eb572c33f86b.naf";
        File inFile = new File(inNaf);
        if (! inFile.exists()) {
            String teiFile = "src/test/resources/tei2naf/text_and_notes.xml";
            Tei2Naf converter = new Tei2Naf();
            String outdir = "src/test/resources/tei2naf/";
            converter.process(teiFile, outdir);
        }
        assertTrue(inFile.exists());
        return inNaf;
    }
    @Test
    public void testCohesiveTextUnits() {
        String rawText = "dfnnnpndnpf";
        TunitTree tree = TunitTree.create(elts);
        assertEquals(tree.getChildren().size(), 1);
        List<Tunit> text = NafUnitSelector.filterText(tree);
        assertEquals(text.size(), 2);
        List<Pair<Integer, String>> unitOffsetAndText = NafUnitSelector.joinCohesive(text, rawText);
        assertEquals(unitOffsetAndText.size(), 1);
    }

    @Test
    public void testTransferHeader() throws AbnormalProcessException {
        NafDoc inputNaf = NafDoc.create(getTestInputNaf());
        List<LinguisticProcessors> inputLps = inputNaf.getLinguisticProcessorsList();
        assertEquals(inputLps.size(), 2);
        assertEquals(inputLps.get(0).getLps().size(), 1);

        NafUnitSelector nus = new NafUnitSelector("all");
        NafDoc outNaf = nus.transferHeader(inputNaf);
        List<LinguisticProcessors> lps = outNaf.getLinguisticProcessorsList();
        assertEquals(lps.size(), 2);
        assertEquals(lps.get(0).getLps().size(), 2);
    }
}