package xmiIn2naf;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import utils.common.Span;
import xjc.naf.Wf;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class AlignedEntity implements Comparable<AlignedEntity> {
    NamedEntity entity;
    List<Span> referenceMatches;

    public AlignedEntity(NamedEntity entity, List<Span> referenceMatches) {
        this.entity = entity;
        this.referenceMatches = referenceMatches;
    }

    public static AlignedEntity create(NamedEntity entity, String rawText) {
        return new AlignedEntity(entity, match(entity.getCoveredText(), rawText));
    }

    public static AlignedEntity create(NamedEntity entity) {
        return new AlignedEntity(entity, Collections.singletonList(new Span(entity.getBegin(), entity.getEnd() - 1)));
    }

    /**
     * copies spans to a new list, keeping other references
     * @param e
     * @return
     */
    public static AlignedEntity copy(AlignedEntity e) {
        List<Span> references = e.getReferenceMatches().stream().collect(Collectors.toList());
        return new AlignedEntity(e.getEntity(), references);
    }

    private static List<Span> match(String substring, String compactRawText) {
        List<Span> matches = new LinkedList<>();
        int index = compactRawText.indexOf(substring);
        while (index != -1) {
            matches.add(new Span(index, index + substring.length() - 1));
            index = compactRawText.indexOf(substring, index + 1);
        }
        return matches;
    }

    protected List<Span> referencesBetween(int startIndex, int endIndex) {
        return referenceMatches.stream()
                .filter(x -> x.getFirstIndex() >= startIndex && x.getFirstIndex() <= endIndex)
                .collect(Collectors.toList());
    }
    public boolean hasMultipleMatches() {
        return referenceMatches.size() > 1;
    }

    public boolean hasNoMatch() {
        return referenceMatches.isEmpty();
    }

    public boolean hasSingleMatch() {
        return referenceMatches.size() == 1;
    }

    public List<Span> getReferenceMatches() {
        return referenceMatches;
    }

    public void setReferenceMatches(List<Span> referenceMatches) {
        this.referenceMatches = referenceMatches;
    }

    public NamedEntity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return entity.getIdentifier() + " " + entity.getValue() + " " + entity.getCoveredText()
                + "\nMatches: " + referenceMatches.stream().map(x -> x.toString()).collect(Collectors.joining(", "));
    }

    public Span getSpan() {
        if (referenceMatches.size() != 1)
            throw new IllegalArgumentException("trying to call getSpan() on zero or multiple reference spans");
        return referenceMatches.get(0);
    }

    public boolean hasSingleMatchCoveringWithSameType(Span s, AlignedEntity e) {
        return referenceMatches.size() == 1 &&
                referenceMatches.get(0).getFirstIndex() <= s.getFirstIndex()
                && referenceMatches.get(0).getLastIndex() >= s.getLastIndex()
                && entity.getValue().startsWith(e.getEntity().getValue()); // this will also exclude LOC under LOCderiv
    }

    protected boolean hasType(String type) {
        return entity.getValue().equals(type);
    }

    /**
     * Identifies entities covering an unlikely match <code>s</code> for the input entity <code>e</code>
     * @param s
     * @param e
     * @return
     */
    public boolean overrules(Span s, AlignedEntity e) {
        return hasReferenceMatchCovering(s) &&
                (this.hasType("LOCderiv") && e.hasType("LOC")
                || this.hasType("LOC") && e.hasType("SHP"))
                || hasReferenceMatchStrictlyCovering(s) && this.hasType(e.getEntity().getValue());
    }

    private boolean hasReferenceMatchStrictlyCovering(Span s) {
        return referenceMatches.stream().anyMatch(x -> x.contains(s) && (x.getFirstIndex() < s.getFirstIndex() || s.getLastIndex() < x.getLastIndex()));
    }

    private boolean hasReferenceMatchCovering(Span s) {
        return referenceMatches.stream().anyMatch(x -> x.contains(s));
    }

    public void findHyphenatedMatch(String rawText, int index) {
        for (int i = 1; i < entity.getCoveredText().length() - 1; i++) {
            String hyphenated = entity.getCoveredText().substring(0, i) + "-" + entity.getCoveredText().substring(i);
            List<Span> matches = match(hyphenated, rawText).stream().filter(s -> s.getFirstIndex() >= index).collect(Collectors.toList());
            if (! matches.isEmpty()) {
                referenceMatches = Collections.singletonList(matches.get(0));
                break;
            }
        }
    }

    public void findWhiteSpaceFreeMatch(String rawText, int index) {
        String wsFree = entity.getCoveredText().replaceAll("\n", "");
        List<Span> matches = match(wsFree, rawText).stream().filter(s -> s.getFirstIndex() >= index).collect(Collectors.toList());
        if (! matches.isEmpty()) {
            referenceMatches = Collections.singletonList(matches.get(0));
        }
    }

    public void tryAgainSimpleMatch(String rawText, int index, int nextIndex) {
        List<Span> matches = match(entity.getCoveredText(), rawText).stream().filter(s -> s.getFirstIndex() >= index && s.getLastIndex() <= nextIndex).collect(Collectors.toList());
        if (! matches.isEmpty())
            referenceMatches = Collections.singletonList(matches.get(0));
    }

    @Override
    public int compareTo(AlignedEntity o) {
        return referenceMatches.get(0).compareTo(o.getReferenceMatches().get(0));
    }

    protected List<Wf> overlapping(List<Wf> wfs) {
        List<Wf> tokenSpan = wfs.stream().filter(wf -> overlaps(wf)).collect(Collectors.toList());
        if (tokenSpan.isEmpty())
            throw new IllegalArgumentException("found empty token span for " + this.toString());
        return tokenSpan;
    }

    protected boolean overlaps(Wf wf) {
        Span wfSpan = Span.fromCharPosition(wf.getOffset(), wf.getLength());
        return wfSpan.overlaps(getSpan());
    }

    protected boolean isEmbeddedInLargerNonEntityToken(Span s, String xmiRawText) {
        boolean tokenStart = s.getFirstIndex() == 0 || Character.isWhitespace(xmiRawText.charAt(entity.getBegin() - 1));
        boolean tokenEnd = s.getLastIndex() == xmiRawText.length() - 1 || Character.isWhitespace(xmiRawText.charAt(entity.getEnd()));
        boolean typeFits = hasType("LOC") || hasType("PER");
        return typeFits && (! tokenStart || ! tokenEnd);
    }
}
