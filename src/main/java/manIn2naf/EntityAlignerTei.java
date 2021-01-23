package manIn2naf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.naf.NafHandler;
import utils.naf.NafUnits;
import utils.xmi.CasDoc;
import xjc.naf.Entity;
import utils.naf.Wf;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class EntityAlignerTei implements EntityAligner {
    AlignedEntities alignedEntities;
    NafHandler refNaf;
    //String rawNafText;
    CasDoc xmi;
    public static final Logger logger = LogManager.getLogger(EntityAlignerTei.class);

    public EntityAlignerTei(NafHandler naf, CasDoc xmi) {
        this.refNaf = naf;
        this.xmi = xmi;
    }

    public List<Entity> align() {
        List<AlignedEntity> entities = getEntities(refNaf.getRawText());
        List<List<Wf>> tokenSpans = findOverlappingTokens(entities);
        return createNafEntities(tokenSpans, entities);
    }

    public List<AlignedEntity> getEntities(String rawNafText) {
        if (xmi.getRawText().equals(rawNafText))
            return xmi.getEntities().stream().map(e -> AlignedEntity.create(e)).collect(Collectors.toList());
        else
            return findEntitiesInRawText(rawNafText);
    }

    /**
     * lists the tokens overlapping with input entities.
     * Entities may themselves overlap.
     * @param entities
     */
    public List<List<Wf>> findOverlappingTokens(List<AlignedEntity> entities) {
        List<Wf> wfs = refNaf.getWfs();
        return entities.stream().map(e -> e.overlapping(wfs)).collect(Collectors.toList());
    }


    public List<Entity> createNafEntities(List<List<Wf>> tokenSpans, List<AlignedEntity> entities) {
        List<Entity> nafEntities = new LinkedList<>();
        for (int i = 0; i < tokenSpans.size(); i++) {
            List<Wf> tokenSpan = tokenSpans.get(i);
            AlignedEntity e = entities.get(i);
            e.fillMissingTypes();
            nafEntities.add(NafUnits.createEntity("e" + i, e.getEntity().getValue(), tokenSpan));
        }
        return nafEntities;
    }


    List<AlignedEntity> findEntitiesInRawText(String rawNafText) {
        this.alignedEntities = AlignedEntities.create(xmi.getEntities().stream()
                .map(e -> AlignedEntity.create(e, rawNafText)).collect(Collectors.toList()));
        reduceMultipleMatches(rawNafText);
        resolveUnalignedEntities(rawNafText);
        alignedEntities.finalizeAlignment();

        return alignedEntities.singleAlignments();
    }


    void reduceMultipleMatches(String rawNafText) {
        long reference = alignedEntities.multipleAlignmentsCount() + 1;
        while (alignedEntities.multipleAlignmentsCount() < reference) {
            reference = alignedEntities.multipleAlignmentsCount();
            alignedEntities.reduceMultipleMatchesByAnchoring(rawNafText.length());
        }
        alignedEntities.removeAssignedMatches();
        alignedEntities.fillMultiLabelEntitiesBetweenAnchorsAndNormalize();

        alignedEntities.removeAssignedMatches();
        alignedEntities.duplicateMultiLabelEntitiesAndNormalize();

        alignedEntities.removeUnlikelySpans(xmi.getRawText());
        alignedEntities.reduceMultipleMatchesWithSameNumberOfMentionsPerType();

        alignedEntities.duplicateRemaining(rawNafText);
    }

    void resolveUnalignedEntities(String rawNafText) {
        for (int i = 0; i < alignedEntities.size(); i++) {
            if (alignedEntities.get(i).hasNoMatch()) {
                AlignedEntity previous = getPreviousAlignedEntity(i);
                int index = previous == null ? 0 : previous.getReferenceMatches().get(0).getFirstIndex();
                AlignedEntity current = alignedEntities.get(i);
                current.findHyphenatedMatch(rawNafText, index);
                if (current.hasNoMatch())
                    current.findWhiteSpaceFreeMatch(rawNafText, index);
                if (current.hasNoMatch()) {
                    AlignedEntity next = getNextAlignedEntity(i);
                    int nextIndex = next == null ? rawNafText.length() : next.getReferenceMatches().get(0).getFirstIndex();
                    current.tryAgainSimpleMatch(rawNafText, index, nextIndex);
                    if (current.hasNoMatch())
                        current.findInRawTextWithLineBreaks(rawNafText, index, nextIndex);
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

    AlignedEntity getPreviousAlignedEntity(int from) {
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

    AlignedEntity getNextAlignedEntity(int from) {
        int i = from;
        AlignedEntity next = getNextEntity(i);
        while (next != null && next.hasNoMatch()) {
            i++;
            next = getNextEntity(i);
        }
        return next;
    }

    public void logStats() {
        alignedEntities.logStats();
    }
}
