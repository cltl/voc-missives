package manIn2naf;

import missives.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.NafCreator;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import utils.xmi.CasDoc;
import xjc.naf.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Integrates manual entity annotations from an XMI file into
 * a reference NAF file.
 * Entities from the XMI file are mapped against the raw text of the
 * reference NAF, and then matched to overlapping tokens. The reference
 * NAF is accordingly enriched with an entities layer.
 */
public class NafXmiReader implements NafCreator {
    NafDoc refNaf;
    CasDoc xmi;
    private final static String NAME = "man-in2naf";
    public static final Logger logger = LogManager.getLogger(NafXmiReader.class);
    AlignedEntities alignedEntities;

    public NafXmiReader(String refNaf, String inputXmi) throws AbnormalProcessException {
        logger.info(inputXmi);
        this.xmi = CasDoc.create(inputXmi);
        this.refNaf = NafDoc.create(refNaf);
    }

    public NafXmiReader(String refNaf, CasDoc inputXmi) throws AbnormalProcessException {
        logger.info(refNaf);
        this.xmi = inputXmi;
        this.refNaf = NafDoc.create(refNaf);
    }

    public CasDoc getXmi() {
        return xmi;
    }

    protected List<AlignedEntity> getEntities() {
        if (xmi.getRawText().equals(refNaf.getRawText()))
            return xmi.getEntities().stream().map(e -> AlignedEntity.create(e)).collect(Collectors.toList());
        else
            return findEntitiesInRawText();
    }

    private List<AlignedEntity> findEntitiesInRawText() {
        this.alignedEntities = AlignedEntities.create(xmi.getEntities().stream()
                .map(e -> AlignedEntity.create(e, refNaf.getRawText())).collect(Collectors.toList()));
        reduceMultipleMatches();
        resolveUnalignedEntities();
        alignedEntities.finalizeAlignment();

        return alignedEntities.singleAlignments();
    }

    private void reduceMultipleMatches() {
        long reference = alignedEntities.multipleAlignmentsCount() + 1;
        while (alignedEntities.multipleAlignmentsCount() < reference) {
            reference = alignedEntities.multipleAlignmentsCount();
            alignedEntities.reduceMultipleMatchesByAnchoring(refNaf.getRawText().length());
        }
        alignedEntities.removeAssignedMatches();
        alignedEntities.fillMultiLabelEntitiesBetweenAnchorsAndNormalize();

        alignedEntities.removeAssignedMatches();
        alignedEntities.duplicateMultiLabelEntitiesAndNormalize();

        alignedEntities.removeUnlikelySpans(xmi.getRawText());
        alignedEntities.reduceMultipleMatchesWithSameNumberOfMentionsPerType();

        alignedEntities.duplicateRemaining(refNaf.getRawText());
    }

    protected void resolveUnalignedEntities() {
        for (int i = 0; i < alignedEntities.size(); i++) {
            if (alignedEntities.get(i).hasNoMatch()) {
                AlignedEntity previous = getPreviousAlignedEntity(i);
                int index = previous == null ? 0 : previous.getReferenceMatches().get(0).getFirstIndex();
                AlignedEntity current = alignedEntities.get(i);
                current.findHyphenatedMatch(refNaf.getRawText(), index);
                if (current.hasNoMatch())
                    current.findWhiteSpaceFreeMatch(refNaf.getRawText(), index);
                if (current.hasNoMatch()) {
                    AlignedEntity next = getNextAlignedEntity(i);
                    int nextIndex = next == null ? refNaf.getRawText().length() : next.getReferenceMatches().get(0).getFirstIndex();
                    current.tryAgainSimpleMatch(refNaf.getRawText(), index, nextIndex);
                }
                if (current.hasNoMatch()) {
                    logger.warn("no match found for entity " + current.toString());
                }
            }
        }
    }

    private AlignedEntity getPreviousEntity(int i) {
        return i > 0 ? alignedEntities.get(i - 1) : null;
    }

    private AlignedEntity getPreviousAlignedEntity(int from) {
        int i = from;
        AlignedEntity next = getPreviousEntity(i);
        while (next != null && next.hasNoMatch()) {
            i--;
            next = getPreviousEntity(i);
        }
        return next;
    }

    private AlignedEntity getNextEntity(int i) {
        return i < alignedEntities.size() - 1 ? alignedEntities.get(i + 1) : null;
    }

    private AlignedEntity getNextAlignedEntity(int from) {
        int i = from;
        AlignedEntity next = getNextEntity(i);
        while (next != null && next.hasNoMatch()) {
            i++;
            next = getNextEntity(i);
        }
        return next;
    }

    /**
     * lists the tokens overlapping with input entities.
     * Entities may themselves overlap.
     * @param entities
     */
    protected List<List<Wf>> findOverlappingTokens(List<AlignedEntity> entities) {
        List<Wf> wfs = refNaf.getWfs();
        return entities.stream().map(e -> e.overlapping(wfs)).collect(Collectors.toList());
    }


    protected void createEntitiesLayer(List<List<Wf>> tokenSpans, List<AlignedEntity> entities) {
        List<Entity> nafEntities = new LinkedList<>();
        for (int i = 0; i < tokenSpans.size(); i++) {
            List<Wf> tokenSpan = tokenSpans.get(i);
            AlignedEntity e = entities.get(i);
            e.fillMissingTypes();
            nafEntities.add(NafUnits.createEntity("e" + i, e.getEntity().getValue(), tokenSpan));
        }

        createEntitiesLayer(refNaf, nafEntities);
    }

    public void transferEntitiesToNaf() {
        List<AlignedEntity> entities = getEntities();
        List<List<Wf>> tokenSpans = findOverlappingTokens(entities);
        createEntitiesLayer(tokenSpans, entities);
    }

    private void convertTo(File outFile) throws AbnormalProcessException {
        transferEntitiesToNaf();
        write(outFile.toString());
        logStats();
    }

    public static void run(Path file, List<String> dirs) throws AbnormalProcessException {
        File refFile = IO.findFileWithSameId(file, new File(dirs.get(0)));
        File outFile = Paths.get(dirs.get(1), refFile.getName()).toFile();
        NafXmiReader nafXmiReader = new NafXmiReader(refFile.getPath(), file.toString());
        nafXmiReader.convertTo(outFile);
    }

    public static void runWithConnl2Xmi(Path file, List<String> dirs) throws AbnormalProcessException {
        File refFile = IO.findFileWithSameId(file, new File(dirs.get(0)));
        File outFile = Paths.get(dirs.get(1), refFile.getName()).toFile();
        Conll2Xmi conll2Xmi = new Conll2Xmi(file.toString());
        conll2Xmi.convert();
        NafXmiReader nafXmiReader = new NafXmiReader(refFile.getPath(), conll2Xmi.getXmi());
        nafXmiReader.convertTo(outFile);
    }

    private void logStats() {
        alignedEntities.logStats();
    }


    public void write(String outFile) throws AbnormalProcessException {
        refNaf.write(outFile);
    }

    @Override
    public String getName() {
        return Handler.NAME + "-" + NAME;
    }

}
