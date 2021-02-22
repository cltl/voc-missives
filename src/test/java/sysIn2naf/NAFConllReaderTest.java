package sysIn2naf;

import utils.common.AbnormalProcessException;
import org.junit.jupiter.api.Test;
import utils.naf.BaseEntity;
import utils.naf.NafHandler;
import utils.naf.NafUnits;
import utils.naf.Wf;
import xjc.naf.Entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sysIn2naf.NAFConllReader.entitiesWithIdSpans;

class NAFConllReaderTest {
    static String refNAF = "src/test/resources/entity-integration/ref.naf";
    static String inConll = "src/test/resources/entity-integration/train.conll";
    static String fromConllNaf = "src/test/resources/entity-integration/fromConll.naf";
    static String bareConll = "src/test/resources/entity-integration/test.conll";
    static String modConll = "src/test/resources/entity-integration/mod.conll";

    @Test
    public void testConllIn2Naf() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(refNAF, false, false);
        List<String[]> lines = ncr.conllLines(inConll);
        List<BaseEntity> entitiesWithIdSpans = entitiesWithIdSpans(NAFConllReader.conllLabels(lines));
        ncr.writeEntitiesToNaf(ncr.asNafEntities(entitiesWithIdSpans, ncr.getNaf().getWfs(), false), fromConllNaf);
        Path p = Paths.get(fromConllNaf);
        assertTrue(Files.exists(p));
    }

    @Test
    public void testDifferences() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(fromConllNaf, false, true);
        List<String[]> newlines = ncr.conllLines(bareConll);
        List<BaseEntity> newentitiesWithIdSpans = entitiesWithIdSpans(NAFConllReader.conllLabels(newlines));
        assertTrue(newentitiesWithIdSpans.isEmpty());

        List<Entity> currentEntities = ncr.getNaf().getEntities();
        List<BaseEntity> diff = ncr.compareToExisting(currentEntities, newentitiesWithIdSpans);
        // entities differ by their label -> they are all removed
        assertEquals(diff.size(), currentEntities.size());
    }

    @Test
    public void testDifferencesInTokensWithNewEntities() throws AbnormalProcessException, IOException {
        NAFConllReader ncr = new NAFConllReader(fromConllNaf, false, false);
        List<String[]> lines = ncr.conllLines(modConll);
        List<String> conllTokens = NAFConllReader.conllTokens(lines);
        List<BaseEntity> entitiesWithIdSpans = entitiesWithIdSpans(NAFConllReader.conllLabels(lines));
        assertEquals(entitiesWithIdSpans.size(), 1);
        assertEquals(entitiesWithIdSpans.get(0).begin(), 9);

        entitiesWithIdSpans = ncr.mapEntitiesToNafWfIds(entitiesWithIdSpans, ncr.mapConllTokensToNafWfs(conllTokens));
        assertEquals(entitiesWithIdSpans.size(), 1);
        assertEquals(entitiesWithIdSpans.get(0).begin(), 10);

        List<Wf> tokens = ncr.getNaf().getWfs();
        List<BaseEntity> conllEntities = ncr.createEntitiesWithOffsets(entitiesWithIdSpans, tokens);

        List<Entity> conllAsNafEntities = ncr.asNafEntities(conllEntities, tokens, true);
        List<Wf> span = NafUnits.wfSpan(conllAsNafEntities.get(0));
        assertEquals(span.get(0).getId(), "w10");

        int before = ncr.getNaf().getEntities().size();
        conllEntities.addAll(ncr.asBaseEntities(ncr.getNaf().getEntities()));
        Collections.sort(conllEntities);
        ncr.writeEntitiesToNaf(ncr.asNafEntities(conllEntities, tokens, true), "-add");
        assertEquals(ncr.getNaf().getEntities().size(), before + 1);
        Files.createDirectories(Paths.get("src/test/resources/out"));
        ncr.writeNaf("src/test/resources/out/conll-in2naf-keep-tokens-add-entities.naf");
    }

    @Test
    public void testReplaceTokensAddEntities() throws AbnormalProcessException, IOException {
        NAFConllReader ncr = new NAFConllReader(fromConllNaf, true, false);
        String outdir = "src/test/resources/out";
        String outNaf = Paths.get(outdir, "replace-tokens-add-entities.naf").toString();
        Files.createDirectories(Paths.get(outdir));
        ncr.process(modConll, outNaf);
        NafHandler naf = NafHandler.create(outNaf);
        Entity e0 = naf.getEntities().get(0);
        assertEquals(NafUnits.wfSpan(e0).get(0).getId(), "w9");
    }

}