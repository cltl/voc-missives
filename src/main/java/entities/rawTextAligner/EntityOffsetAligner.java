package entities.rawTextAligner;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.*;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import utils.xmi.CasDoc;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityOffsetAligner {
    CasDoc inputEntitiesDoc;
    NafDoc referenceNAF;
    String outputXMI;
    OffsetMapper mapper;
    String rawText;
    java.util.function.Predicate<Fragment> selector;

    static int countUnaligned = 0;
    static int countManyAligned = 0;
    static int countAligned = 0;
    static int countAll = 0;
    static final Pattern TEXT = Pattern.compile(".*\\.p\\.\\d+$");
    static final Pattern FW = Pattern.compile(".*fw\\.\\d+$");
    static final Pattern NOTE = Pattern.compile(".*note\\.\\d+$");
    static final Pattern NOTE_TEXT = Pattern.compile(".*note.*\\.p\\.\\d+");
    static final Pattern HEAD = Pattern.compile(".*head\\.\\d+$");

    static final java.util.function.Predicate<Fragment> TEXT_SELECTOR = x -> (TEXT.matcher(x.getId()).matches()
            || HEAD.matcher(x.getId()).matches()) && ! NOTE_TEXT.matcher(x.getId()).matches();

    static final java.util.function.Predicate<Fragment> NOTE_SELECTOR = x -> NOTE.matcher(x.getId()).matches();

    private static final String IN = "." + IO.XMI_SFX;
    private static final String REF = "." + IO.NAF_SFX;
    private static final String OUT = "." + IO.XMI_SFX;
    public static final Logger logger = LogManager.getLogger(EntityOffsetAligner.class);
    List<Fragment> selected;
    public EntityOffsetAligner(String inXmi, String inNaf, String outXmi, String selection) throws AbnormalProcessException {
        this.inputEntitiesDoc = CasDoc.create(inXmi);
        this.referenceNAF = NafDoc.create(inNaf);
        this.rawText = referenceNAF.getRawText();
        this.outputXMI = outXmi;
        this.mapper = OffsetMapper.create(Fragment.flatten2(NafUnits.asFragments(referenceNAF.getTunits())),
                rawText);
        this.selector = selection.equals("text") ? TEXT_SELECTOR : NOTE_SELECTOR;
        selected = NafUnits.asFragments(referenceNAF.getTunits()).stream().filter(selector).collect(Collectors.toList());

    }

    public void writeOutputXmi() throws AbnormalProcessException {
        CasDoc outputDoc = CasDoc.create();
        outputDoc.addRawText(rawText);

        logger.info("\n* " + outputXMI);
        List<NamedEntity> alignedEntities = alignEntitiesToRawTextOffsets();

    }

    public static void finalStats() {
        StringBuilder m = new StringBuilder();
        m.append("\n\n-------\naligned: ").append(countAligned);
        m.append("; unaligned: ").append(countUnaligned);
        m.append("; manyAligned: ").append(countManyAligned);
        m.append("; all: ").append(countAll);
        m.append("\n alignment rate: ").append(countAligned * 1.0 / countAll);
        logger.info(m.toString());
    }

    private List<NamedEntity> alignEntitiesToRawTextOffsets() {
        List<List<Span>> alignedSpans = applyAlignmentStrategies();

        List<NamedEntity> entities = mapToEntities(alignedSpans);

        return entities;
    }

    private List<List<Span>> applyAlignmentStrategies() {
        List<List<Span>> alignedSpans = alignMentions();
        alignedSpans = extendString(alignedSpans);
        alignedSpans = filterInconsistentMatches(alignedSpans);
        return alignedSpans;
    }

    /***
     * Maps entity mention types to their occurrences in the list of entities,
     * finds for each mention its string matches in the raw text,
     * and returns a list of string matches for each entity occurrence.
     * @return
     */
    private List<List<Span>> alignMentions() {
        // map mention types to indices in list
        HashMap<String, List<Integer>> mentions2indices = mentions2indices();
        HashMap<Integer,List<Span>> aligned = new HashMap<>();
        for (String mention: mentions2indices.keySet()) {
            List<Span> matches = mapper.coarseMatches(mention);     // nb string matches
            matches = filterByType(matches);
            List<Integer> eIndices = mentions2indices.get(mention); // nb occurrences in entities
            int matchIndex = 0;
            if (eIndices.size() == matches.size()) {
                for (int i: eIndices) {
                    aligned.put(i, Collections.singletonList(matches.get(matchIndex)));
                    matchIndex++;
                }
            } else {
                for (int i: eIndices)
                    aligned.put(i, matches);
            }
        }
        List<List<Span>> result = new LinkedList<>();
        for (int i = 0; i < aligned.size(); i++)
            result.add(aligned.get(i));
        return result;
    }

    /**
     * Tries to reduce multiple matches to a single match by matching larger
     * spans of text, first to the right and otherwise to the left.
     * Multiple matches that cannot be reduced to a single match are returned unchanged.
     * @param matches
     * @return
     */
    private List<List<Span>> extendString(List<List<Span>> matches) {
        List<NamedEntity> entities = inputEntitiesDoc.getEntities();
        List<List<Span>> result = new LinkedList<>();
        for (int i = 0; i < entities.size(); i++) {
            if (matches.get(i).size() > 1) {
                int k = 2;
                List<Span> filtered = matches.get(i);
                while (filtered.size() > 1 && entities.get(i).getEnd() + k + 1 <= inputEntitiesDoc.getRawText().length()) {
                    String rightExtended = inputEntitiesDoc.getRawText().substring(entities.get(i).getBegin(),
                            entities.get(i).getEnd() + k + 1);
                    List<Span> extendedMatches = mapper.coarseMatches(rightExtended);

                    List<Span> finalExtendedMatches1 = extendedMatches;
                    filtered = matches.get(i).stream().filter(m -> finalExtendedMatches1.stream().anyMatch(em -> em.contains(m))).collect(Collectors.toList());
                    k++;
                }

                if (filtered.size() == 1) {
                    result.add(filtered);
                } else {
                    k = 2;
                    filtered = matches.get(i);
                    while (filtered.size() > 1 && entities.get(i).getBegin() - k >= 0) {
                        String leftExtended = inputEntitiesDoc.getRawText().substring(entities.get(i).getBegin() - k,
                                entities.get(i).getEnd() + 1);
                        List<Span> extendedMatches = mapper.coarseMatches(leftExtended);

                        List<Span> finalExtendedMatches1 = extendedMatches;
                        filtered = matches.get(i).stream().filter(m -> finalExtendedMatches1.stream().anyMatch(em -> em.contains(m))).collect(Collectors.toList());
                        k++;
                    }
                    if (filtered.size() == 1) {
                        result.add(filtered);
                    } else {
                        result.add(matches.get(i));
                    }
                }
            } else {
                result.add(matches.get(i));
            }
        }
        return result;
    }

    /**
     * Filters out spans that fall outside the sections of the selected text type
     * ("text" or "notes")
     * @param matches
     * @return
     */
    private List<Span> filterByType(List<Span> matches) {
        return matches.stream().filter(m -> selected.stream().anyMatch(s -> s.getSpan().contains(m))).collect(Collectors.toList());
    }

    private HashMap<String,List<Integer>> mentions2indices() {
        HashMap<String,List<Integer>> mention2Index = new HashMap<>();
        List<NamedEntity> entities = inputEntitiesDoc.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            String mention = entities.get(i).getCoveredText();
            if (mention2Index.containsKey(mention))
                mention2Index.get(mention).add(i);
            else {
                List<Integer> indices = new LinkedList<>();
                indices.add(i);
                mention2Index.put(mention, indices);
            }
        }
        return mention2Index;
    }



    private List<NamedEntity> mapToEntities(List<List<Span>> alignedSpans ) {
        List<NamedEntity> entities = new LinkedList<>();
        List<NamedEntity> unaligned = new LinkedList<>();
        List<NamedEntity> manyAligned = new LinkedList<>();

        for (int i = 0; i < inputEntitiesDoc.getEntities().size(); i++) {
            NamedEntity e = inputEntitiesDoc.getEntities().get(i);
            if (alignedSpans.get(i).size() == 1)
                entities.add(e);
            else if (alignedSpans.get(i).size() > 1)
                manyAligned.add(e);
            else
                unaligned.add(e);
        }
        alignmentStats(entities, unaligned, manyAligned);

        return entities;

    }




    /**
     * Uses single matches as anchors, and
     * filters multiple matches that fall outside of these anchors.
     * @param coarseMatches
     * @return
     */
    private List<List<Span>> filterInconsistentMatches(List<List<Span>> coarseMatches) {
        //
        long manyAlignedBefore = coarseMatches.size() + 1;
        long manyAlignedAfter = coarseMatches.stream().filter(x -> x.size() > 1).count();
        List<List<Span>> filtered = coarseMatches;

        while (manyAlignedAfter < manyAlignedBefore) {
            manyAlignedBefore = manyAlignedAfter;
            filtered = filterMultipleMatchesLeftToRight(coarseMatches);
            filtered = filterMultipleMatchesRightToLeft(filtered);
            manyAlignedAfter = filtered.stream().filter(x -> x.size() > 1).count();
        }
        return filtered;
    }


    private List<List<Span>> filterMultipleMatchesLeftToRight(List<List<Span>> coarseMatches) {
        Span previousSingleMatch = null;
        List<List<Span>> filteredAll = new LinkedList<>();
        for (int i = 0; i < coarseMatches.size(); i++) {
            List<Span> currentMatches = coarseMatches.get(i);

            if (currentMatches.size() == 1) {
                previousSingleMatch = currentMatches.get(0);
                filteredAll.add(currentMatches);
            } else if (previousSingleMatch != null && currentMatches.size() > 1) {
                Span finalPreviousSingleMatch = previousSingleMatch;
                filteredAll.add(currentMatches.stream()
                        .filter(s -> s.getFirstIndex() >= finalPreviousSingleMatch.getFirstIndex())
                        .collect(Collectors.toList()));
            } else
                filteredAll.add(currentMatches);    // keeps lists aligned
        }
        return filteredAll;
    }

    private List<List<Span>> filterMultipleMatchesRightToLeft(List<List<Span>> coarseMatches) {
        Span previousSingleMatch = null;
        List<List<Span>> filteredAll = new LinkedList<>();
        for (int i = coarseMatches.size() - 1; i >= 0; i--) {
            List<Span> currentMatches = coarseMatches.get(i);

            if (currentMatches.size() == 1) {
                previousSingleMatch = currentMatches.get(0);
                filteredAll.add(currentMatches);
            } else if (previousSingleMatch != null && currentMatches.size() > 1) {
                Span finalPreviousSingleMatch = previousSingleMatch;
                filteredAll.add(currentMatches.stream()
                        .filter(s -> s.getLastIndex() <= finalPreviousSingleMatch.getLastIndex())
                        .collect(Collectors.toList()));
            } else
                filteredAll.add(currentMatches);
        }
        return filteredAll;
    }

    private void alignmentStats(List<NamedEntity> entities, List<NamedEntity> unaligned, List<NamedEntity> manyAligned) {
        double percentageAligned = getPercentage(entities, inputEntitiesDoc.getEntities());
        StringBuilder m = new StringBuilder();
        m.append("aligned ").append(percentageAligned).append(" entities\n");
        m.append(unaligned.size()).append(" unaligned: ");
        for (NamedEntity e: unaligned)
            m.append(e.getCoveredText()).append(" -- ");
        m.append("\n").append(manyAligned.size()).append(" many aligned: ");
        for (NamedEntity e: manyAligned)
            m.append(e.getCoveredText()).append(" -- ");
        logger.info(m.toString());
        this.countAligned += entities.size();
        this.countManyAligned += manyAligned.size();
        this.countUnaligned += unaligned.size();
        this.countAll += inputEntitiesDoc.getEntities().size();
    }

    private double getPercentage(List<NamedEntity> entities, List<NamedEntity> total) {
        return 0.01 * ((int) (entities.size() * 10000.0 / total.size()));
    }

    public static void run(Path file, List<String> dirs) throws AbnormalProcessException {
        String fileName = file.getFileName().toString();
        String inNote = "_notes" + IN;
        String selection = "text";
        String extension = IN;
        if (fileName.endsWith(inNote)) {
            extension = inNote;
            selection = "notes";
        }
        if (fileName.endsWith(IN)) {
            String refFile = IO.append(dirs.get(0), IO.replaceExtension(file, extension, REF));
            String outFile = IO.append(dirs.get(1), IO.replaceExtension(file, IN, OUT));

            EntityOffsetAligner offsetAligner = new EntityOffsetAligner(file.toString(), refFile, outFile, selection);
            offsetAligner.writeOutputXmi();
        }
    }
}
