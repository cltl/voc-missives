package xmiIn2naf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.Span;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;

public class AlignedEntities {
    List<AlignedEntity> alignedEntities;
    int manyAlignedCount;
    int unalignedCount;
    int alignedCount;
    AlignedEntities added;
    public static final Logger logger = LogManager.getLogger(AlignedEntities.class);

    public AlignedEntities(){
        this.alignedEntities = new LinkedList<>();
        this.added = null;  // AlignedEntities recursion limited to depth 1
    }

    public AlignedEntities(List<AlignedEntity> entities) {
        this.alignedEntities = entities;
        this.added = null;
    }

    public AlignedEntities(List<AlignedEntity> alignedEntities, AlignedEntities added) {
        this.alignedEntities = alignedEntities;
        this.added = added; // not null only for call from NafXmiReader
    }

    public static AlignedEntities create(List<AlignedEntity> entities) {
        return new AlignedEntities(entities, new AlignedEntities());
    }

    /**
     * Marks entities with a single match as anchors, and filters out multiple matches
     * that fall out of anchor boundaries
     * @param nafTextLength
     */
    protected void reduceMultipleMatchesByAnchoring(int nafTextLength) {
        AlignedEntity anchor = null;
        AlignedEntity rightAnchor;
        for (int i = 0; i < alignedEntities.size(); i++) {
            if (alignedEntities.get(i).hasSingleMatch())
                anchor = alignedEntities.get(i);
            else if (alignedEntities.get(i).hasMultipleMatches()) {
                int j = i + 1;
                while (j < alignedEntities.size() && ! alignedEntities.get(j).hasSingleMatch())
                    j++;
                if (j < alignedEntities.size())
                    rightAnchor = alignedEntities.get(j);
                else
                    rightAnchor = null;
                int startIndex = anchor != null ? anchor.getReferenceMatches().get(0).getFirstIndex(): 0;
                int endIndex = rightAnchor != null ? rightAnchor.getReferenceMatches().get(0).getLastIndex(): nafTextLength;
                List<Span> filtered = alignedEntities.get(i).referencesBetween(startIndex, endIndex);
                if (! filtered.isEmpty())
                    alignedEntities.get(i).setReferenceMatches(filtered);

                if (alignedEntities.get(i).hasSingleMatch())
                    anchor = alignedEntities.get(i);
            }
        }
    }
    protected void removeAssignedMatches() {
        AlignedEntities mult = select(x -> x.hasMultipleMatches());
        mult.filterReferenceMatches((e, s) ->
                alignedEntities.stream().noneMatch(x -> x.hasSingleMatchCoveringWithSameType(s, e)));
    }


    protected void fillMultiLabelEntitiesBetweenAnchorsAndNormalize() {
        int anchorLeft = indexOfAnchor(0);
        int anchorRight = -1;
        if (anchorLeft != -1)
            anchorRight = indexOfAnchor(anchorLeft + 1);
        int first = anchorLeft + 1;
        int last = anchorRight != -1 ? anchorRight : alignedEntities.size(); // excludes index
        while (first < alignedEntities.size()) {
            AlignedEntities sub = select(first, last);
            sub.duplicateMultiLabelEntitiesAndNormalize();
            first = last + 1;
            anchorRight = indexOfAnchor(first);
            last = anchorRight != -1 ? anchorRight : alignedEntities.size();
        }
    }

    List<String> mentions() {
        return alignedEntities.stream().map(x -> x.getEntity().getCoveredText()).distinct().collect(Collectors.toList());
    }

    private void resolveEntityGroups() {
        if (haveLessMatchesThanEntities()) {
            duplicateMatchesOfMultilabelEntities();
        }
        if (areEquicardinal())
            normalizeMatches();
    }

    private void duplicateMatchesOfMultilabelEntities() {
        // look for distinct entities with same span, to duplicate corresponding mention
        List<Span> mentionSpans = alignedEntities.get(0).getReferenceMatches();
        boolean flag = false;
        for (int i = 0; i < alignedEntities.size() - 1; i++) {
            if (alignedEntities.get(i).getEntity().getBegin() == alignedEntities.get(i + 1).getEntity().getBegin()
                    && alignedEntities.get(i).getEntity().getEnd() == alignedEntities.get(i + 1).getEntity().getEnd()) {
                mentionSpans.add(i, mentionSpans.get(i));
                flag = true;
            }
        }
        if (flag) {
            for (AlignedEntity e: alignedEntities) {
                e.setReferenceMatches(mentionSpans);
            }
        }
    }

    private void normalizeMatches() {
        for (int i = 0; i < alignedEntities.size(); i++)
            alignedEntities.get(i).setReferenceMatches(Collections.singletonList(alignedEntities.get(i).getReferenceMatches().get(i)));
    }

    /**
     * removes spans that are unlikely to correspond to an entity.
     * These are spans:
     * - that are embedded in another entity of the same type,
     * - or for which the entity is of type LOC and the candidate span is embedded in an entity of type LOCderiv
     * - or for which the entity is of type SHP but another entity of type LOC has the same span
     * - or that are embedded in a larger non-entity token
     * @param xmiRawText
     */
    protected void removeUnlikelySpans(String xmiRawText) {
        AlignedEntities mult = select(x -> x.hasMultipleMatches());
        BiPredicate<AlignedEntity,Span> customSpanPredicate = (e, s) ->
                alignedEntities.stream().noneMatch(x -> x.overrules(s, e))
                && ! e.isEmbeddedInLargerNonEntityToken(s, xmiRawText);
        mult.filterDistinctReferenceMatches(customSpanPredicate);
    }

    private void filterReferenceMatches(BiPredicate<AlignedEntity, Span> biPredicate) {
        for (AlignedEntity e: alignedEntities)
            e.setReferenceMatches(e.getReferenceMatches().stream()
                    .filter(m -> biPredicate.test(e, m))
                    .collect(Collectors.toList()));
    }
    private void filterDistinctReferenceMatches(BiPredicate<AlignedEntity, Span> biPredicate) {
        for (AlignedEntity e: alignedEntities)
            e.setReferenceMatches(e.getReferenceMatches().stream()
                    .filter(m -> biPredicate.test(e, m))
                    .distinct().collect(Collectors.toList()));
    }

    /**
     * Covers a case of many-aligned entities with different types.
     * We regroup the entities per type before resolving them.
     */
    protected void reduceMultipleMatchesWithSameNumberOfMentionsPerType() {
        groupByMention(g -> g.regroupEntitiesPerTypeAndReduce());
    }

    protected void duplicateMultiLabelEntitiesAndNormalize() {
        groupByMention(g -> g.resolveEntityGroups());
    }

    void groupByMention(Consumer<AlignedEntities> consumer) {
        AlignedEntities mult = select(x -> x.hasMultipleMatches());
        for (String m: mult.mentions()) {
            AlignedEntities withSameMention = mult.select(x -> x.getEntity().getCoveredText().equals(m));
            consumer.accept(withSameMention);
        }
    }

    private void regroupEntitiesPerTypeAndReduce() {
        for (String t: types()) {
            AlignedEntities withSameType = select(e -> e.getEntity().getValue().equals(t));
            withSameType.resolveEntityGroups();
        }
    }

    private List<String> types() {
        return alignedEntities.stream().map(e -> e.getEntity().getValue()).distinct().collect(Collectors.toList());
    }

    /**
     * For cases where entities were omitted during annotations, and have more
     * matches in the ref NAF than occurrences in the input XMI.
     */
    protected void duplicateRemaining() {
        List<AlignedEntity> addedEntities = new LinkedList<>();
        AlignedEntities mult = select(x -> x.hasMultipleMatches());
        for (String m: mult.mentions()) {
            AlignedEntities withSameMention = mult.select(x -> x.getEntity().getCoveredText().equals(m));
            addedEntities.addAll(withSameMention.addEntitiesToMatchMentionCount());
        }
        added.addAll(addedEntities);
    }

    private void addAll(List<AlignedEntity> addedEntities) {
        alignedEntities.addAll(addedEntities);
    }

    private List<AlignedEntity> addEntitiesToMatchMentionCount() {
        List<AlignedEntity> addedEntities = new LinkedList<>();
        while (haveLessEntitiesThanMentions()) {
            AlignedEntity copy = duplicateFirst();
            addedEntities.add(copy);
        }
        normalizeMatches();
        return addedEntities;
    }

    boolean haveLessEntitiesThanMentions() {
        return alignedEntities.size() < alignedEntities.get(0).getReferenceMatches().size();
    }

    private boolean haveLessMatchesThanEntities() {
        return alignedEntities.get(0).getReferenceMatches().size() < alignedEntities.size();
    }

    private boolean areEquicardinal() {
        return alignedEntities.get(0).getReferenceMatches().size() == alignedEntities.size();
    }

    AlignedEntity duplicateFirst() {
        AlignedEntity copy = AlignedEntity.copy(alignedEntities.get(0));
        alignedEntities.add(copy);
        return copy;
    }

    public List<AlignedEntity> singleAlignments() {
        return alignedEntities.stream().filter(x -> x.hasSingleMatch()).collect(Collectors.toList());
    }

    protected int size() {
        return alignedEntities.size();
    }

    protected long multipleAlignmentsCount() {
        return count(x -> x.hasMultipleMatches());
    }

    private List<AlignedEntity> filter(Predicate<AlignedEntity> entityPredicate) {
        return alignedEntities.stream().filter(entityPredicate).collect(Collectors.toList());
    }

    private AlignedEntities select(Predicate<AlignedEntity> entityPredicate) {
        return new AlignedEntities(filter(entityPredicate));
    }

    private int count(Predicate<AlignedEntity> filter) {
        return (int) alignedEntities.stream().filter(filter).count();
    }

    private int indexOfAnchor(int from) {
        for (int j = from; j < alignedEntities.size(); j++) {
            if (alignedEntities.get(j).hasSingleMatch())
                return j;
        }
        return -1;
    }

    AlignedEntities select(int i, int j) {
        List<AlignedEntity> sublist = new LinkedList<>();
        for (int k = i; k < j; k++)
            sublist.add(alignedEntities.get(k));

        return new AlignedEntities(sublist);
    }

    public void logStats() {
        StringBuilder sb = new StringBuilder();
        sb.append(alignedCount);
        sb.append(" aligned;\t");
        sb.append(manyAlignedCount);
        sb.append(" many-aligned;\t");
        sb.append(unalignedCount);
        sb.append(" unaligned;\t");
        sb.append(added.getAlignedCount());
        sb.append(" added");
        logger.info(sb.toString());
    }

    public void finalizeAlignment() {
        updateCounts();
        added.updateCounts();

        alignedEntities.addAll(added.getAlignedEntities());
        alignedEntities = filter(e -> e.hasSingleMatch());
        Collections.sort(alignedEntities);
    }

    public List<AlignedEntity> getAlignedEntities() {
        return alignedEntities;
    }

    public int getAlignedCount() {
        return alignedCount;
    }

    private void updateCounts() {
        this.alignedCount = count(x -> x.hasSingleMatch());
        this.manyAlignedCount = count(x -> x.hasMultipleMatches());
        this.unalignedCount = count(x -> x.hasNoMatch());
    }

    public AlignedEntity get(int i) {
        return alignedEntities.get(i);
    }
}