package xmiIn2naf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.NafCreator;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import utils.xmi.CasDoc;
import xjc.naf.*;

import java.nio.file.Path;
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
    private static final String IN = "." + IO.XMI_SFX;
    private static final String OUT = "." + IO.NAF_SFX;
    private final static String NAME = "xmi-in2naf";
    private final static String VERSION = "0.1.2";
    public static final Logger logger = LogManager.getLogger(NafXmiReader.class);
    AlignedEntities alignedEntities;

    public NafXmiReader(String refNaf, String inputXmi) throws AbnormalProcessException {
        logger.info(inputXmi);
        this.xmi = CasDoc.create(inputXmi);
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
        alignedEntities.duplicateRemaining();

    }

    protected void resolveUnalignedEntities() {
        for (int i = 0; i < alignedEntities.size(); i++) {
            if (alignedEntities.get(i).hasNoMatch()) {
                AlignedEntity previous = i > 0 ? alignedEntities.get(i - 1) : null;
                int index = previous == null ? 0 : previous.getReferenceMatches().get(0).getFirstIndex();
                AlignedEntity current = alignedEntities.get(i);
                current.findHyphenatedMatch(refNaf.getRawText(), index);
                if (current.hasNoMatch())
                    current.findWhiteSpaceFreeMatch(refNaf.getRawText(), index);
                if (current.hasNoMatch()) {
                    AlignedEntity next = i < alignedEntities.size() - 1 ? alignedEntities.get(i + 1) : null;
                    int nextIndex = next == null ? refNaf.getRawText().length() : next.getReferenceMatches().get(0).getFirstIndex();
                    current.tryAgainSimpleMatch(refNaf.getRawText(), index, nextIndex);
                }
                if (current.hasNoMatch()) {
                    logger.warn("no match found for entity " + current.toString());
                }
            }
        }
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
            nafEntities.add(NafUnits.createEntity("e" + i, e.getEntity().getValue(), tokenSpan));
        }

        createEntitiesLayer(refNaf, nafEntities);
    }

    public void transferEntitiesToNaf() {
        List<AlignedEntity> entities = getEntities();
        List<List<Wf>> tokenSpans = findOverlappingTokens(entities);
        createEntitiesLayer(tokenSpans, entities);
    }

    public static void run(Path file, List<String> dirs) throws AbnormalProcessException {
        String fileName = file.getFileName().toString();

        if (fileName.endsWith(IN)) {
            String refFile = IO.append(dirs.get(0), IO.replaceExtension(file, IN, OUT));
            String outFile = IO.append(dirs.get(1), IO.replaceExtension(file, IN, OUT));

            NafXmiReader nafXmiReader = new NafXmiReader(refFile, file.toString());
            nafXmiReader.transferEntitiesToNaf();
            nafXmiReader.write(outFile);
            nafXmiReader.logStats();
        }
    }

    private void logStats() {
        alignedEntities.logStats();
    }


    public void write(String outFile) throws AbnormalProcessException {
        refNaf.write(outFile);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}