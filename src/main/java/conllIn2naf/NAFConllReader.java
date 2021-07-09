package conllIn2naf;

import missives.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.*;
import utils.naf.BaseEntity;
import utils.naf.NafHandler;
import utils.naf.NafUnits;
import xjc.naf.Entity;
import utils.naf.Wf;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enriches NAF file based on Conll file.
 * Tokens in both files may differ as long as their string content
 * aligns overall.
 * The enriched NAF may keep its existing tokens or have them replaced
 * by the Conll tokens.
 * Entities can be added or replaced. In this last case, the differences between
 * both sets of entities are logged.
 */
public class NAFConllReader {

    private final static String NAME = "conllIn2naf";
    private final static int CHAR_WINDOW = 25;
    public static final Logger logger = LogManager.getLogger(NAFConllReader.class);
    NafHandler naf;
    boolean replaceEntities;
    boolean replaceTokens;
    String entityPfx;
    String entitySource;
    String dataRevisionVersion;

    public NAFConllReader(String nafFile, String entitySource, String dataRevisionVersion, boolean replaceTokens, boolean addEntities) throws AbnormalProcessException {
        this.naf = NafHandler.create(nafFile);
        this.replaceEntities = ! addEntities;
        this.replaceTokens = replaceTokens;
        this.entityPfx = naf.entityPfx();
        this.entitySource = entitySource;
        this.dataRevisionVersion = dataRevisionVersion;
    }

    public NafHandler getNaf() {
        return naf;
    }

    public static List<String[]> conllLines(String conllFile) throws AbnormalProcessException {
        String line;
        List<String[]> tokens = new ArrayList<>();
        try (BufferedReader bfr = new BufferedReader(new FileReader(conllFile))) {
            while ((line = bfr.readLine()) != null) {
                if (line.length() > 0) {
                    String[] tokenLabel = line.split("\\s+");
                    tokens.add(tokenLabel);
                }
            }
        } catch (FileNotFoundException e) {
            throw new AbnormalProcessException(conllFile, e);
        } catch (IOException e) {
            throw new AbnormalProcessException(conllFile, e);
        }
        return tokens;
    }

    static List<String> conllTokens(List<String[]> conllLines) {
        return conllLines.stream().map(x -> x[0]).collect(Collectors.toList());
    }

    static List<String> conllLabels(List<String[]> conllLines) {
        return conllLines.stream().map(x -> x[1]).collect(Collectors.toList());
    }

    static List<BaseEntity> entitiesWithIdSpans(List<String> conllLabels) throws AbnormalProcessException {
        List<BaseEntity> entities = new LinkedList<>();
        int first = -1;
        int last = -1;
        String type = "";
        for (int i = 0; i < conllLabels.size(); i++) {
            String label = conllLabels.get(i);
            if (label.startsWith("B")) {
                if (last != -1)     // in case B- directly follows on I-
                    entities.add(new BaseEntity(new Span(first, last), type));
                first = i;
                last = i;
                if (label.split("-").length != 2)
                    throw new AbnormalProcessException("invalid label for token " + i + ": " + label);
                type = label.split("-")[1];
            } else if (label.startsWith("I")) {
                last = i;
                if (first == -1)
                    throw new AbnormalProcessException("entity at index " + i + " starts with I- label");
            } else {
                if (last != -1)
                    entities.add(new BaseEntity(new Span(first, last), type));
                first = -1;
                last = -1;
                type = "";
            }
        }
        if (last != -1)
            entities.add(new BaseEntity(new Span(first, last), type));
        return entities;
    }


    protected void writeNaf(String outFile) throws AbnormalProcessException {
        naf.write(outFile);
    }

    void process(String conllFile, String outFile) throws AbnormalProcessException {
        List<String[]> lines = conllLines(conllFile);
        List<String> conllTokens = conllTokens(lines);
        List<BaseEntity> entitiesWithIdSpans;
        try {
            entitiesWithIdSpans = entitiesWithIdSpans(conllLabels(lines));
        } catch (AbnormalProcessException e) {
            logger.error("Error while processing " + conllFile);
            throw e;
        }

        List<BaseEntity> conllEntities;
        List<Wf> tokens;
        List<Entity> nafEntities;
        if (replaceTokens) {
            tokens = createWfs(conllTokens);
            writeTokensToNaf(tokens);
            nafEntities = realignNafEntities(tokens);
        } else {
            entitiesWithIdSpans = mapEntitiesToNafWfIds(entitiesWithIdSpans, mapConllTokensToNafWfs(conllTokens));
            tokens = naf.getWfs();
            nafEntities = naf.getEntities();
        }
        conllEntities = createEntitiesWithOffsets(entitiesWithIdSpans, tokens);

        if (nafEntities.isEmpty()) {
            logger.warn("Adding entities to an empty entity layer");
            writeEntitiesToNaf(asNafEntities(entitiesWithIdSpans, tokens, false), "", dataRevisionVersion);
        } else {
            String version = naf.getLinguisticProcessors("entities").getLps().get(0).getVersion()
                    + "." + dataRevisionVersion;
            if (replaceEntities) {
                compareToExisting(nafEntities, conllEntities);
                writeEntitiesToNaf(asNafEntities(entitiesWithIdSpans, tokens, false), "", version);
            } else {
                logger.info("adding " + conllEntities.size() + " new entities to NAF...");

                conllEntities.addAll(asBaseEntities(nafEntities));
                conllEntities = conllEntities.stream().distinct().collect(Collectors.toList());
                Collections.sort(conllEntities);
                writeEntitiesToNaf(asNafEntities(conllEntities, tokens, true), "-add", version);
            }
        }

        writeNaf(outFile);
    }

    private List<Entity> realignNafEntities(List<Wf> tokens) {
        return asNafEntities(asBaseEntities(naf.getEntities()), tokens, true);
    }

    List<BaseEntity> createEntitiesWithOffsets(List<BaseEntity> entitiesWithIdSpans, List<Wf> wfs) {
        List<BaseEntity> offsetEntities = new LinkedList<>();
        for (BaseEntity eWithIdSpan: entitiesWithIdSpans) {
            int first = Integer.parseInt(wfs.get(eWithIdSpan.begin()).getOffset());
            Wf lastWf = wfs.get(eWithIdSpan.end() - 1);
            int end = Integer.parseInt(lastWf.getOffset()) + Integer.parseInt(lastWf.getLength());
            offsetEntities.add(new BaseEntity(new Span(first, end - 1), eWithIdSpan.getType()));
        }
        return offsetEntities;
    }

    List<BaseEntity> mapEntitiesToNafWfIds(List<BaseEntity> entitiesWithIdSpans, List<List<Integer>> indexMap) {
        List<BaseEntity> reindexed = new LinkedList<>();
        for (BaseEntity entity: entitiesWithIdSpans) {
            Set<Integer> wfidset = new HashSet<>();
            for (int i = entity.begin(); i < entity.end(); i++)
                wfidset.addAll(indexMap.get(i));
            List<Integer> wfids = new LinkedList<>(wfidset);
            Collections.sort(wfids);
            reindexed.add(new BaseEntity(new Span(wfids.get(0), wfids.get(wfids.size() - 1)), entity.getType()));
        }
        return reindexed;
    }

    List<List<Integer>> mapConllTokensToNafWfs(List<String> conllTokens) throws AbnormalProcessException {
        WfAligner wfAligner = new WfAligner(naf.getWfs(), conllTokens);
        return wfAligner.indexMap();
    }

    /**
     * align conll tokens to naf wfs
     * @param conllTokens
     * @return
     */
    private List<Wf> createWfs(List<String> conllTokens) throws AbnormalProcessException {
        WfAligner wfAligner = new WfAligner(naf.getWfs(), conllTokens);
        return wfAligner.getAlignedWfs();
    }

    List<Entity> asNafEntities(List<BaseEntity> entitiesWithWfIds, List<Wf> wfs, boolean charOffsets) {
        List<Entity> entities = new LinkedList<>();
        for (int i = 0; i < entitiesWithWfIds.size(); i++) {
            BaseEntity entityWithWfId = entitiesWithWfIds.get(i);
            int firstWfIndex = charOffsets ? getWfWithOffset(entityWithWfId.begin(), wfs) : entityWithWfId.begin();
            int lastWfIndex = charOffsets ? getWfEndingAt(entityWithWfId.end(), wfs) : entityWithWfId.end() - 1;
            List<Wf> wfSpan = new LinkedList<>();
            for (int j = firstWfIndex; j <= lastWfIndex; j++)
                wfSpan.add(wfs.get(j));
            entities.add(NafUnits.createEntity(entityPfx + i, entityWithWfId.getType(), wfSpan));
        }
        return entities;
    }

    private int getWfEndingAt(int end, List<Wf> wfs) {
        for (int i = 0; i < wfs.size(); i++)
            if (end == Integer.parseInt(wfs.get(i).getOffset()) + Integer.parseInt(wfs.get(i).getLength()))
                return i;
        return -1;
    }

    private int getWfWithOffset(int offset, List<Wf> wfs) {
        for (int i = 0; i < wfs.size(); i++)
            if (offset == Integer.parseInt(wfs.get(i).getOffset()))
                return i;
        return -1;
    }

    List<BaseEntity> asBaseEntities(List<Entity> entities) {
        return entities.stream().map(e -> BaseEntity.create(e)).collect(Collectors.toList());
    }

    void writeEntitiesToNaf(List<Entity> conllEntities, String mod, String version) {
        naf.createEntitiesLayer(conllEntities, getName() + mod, version);
    }

    private void writeTokensToNaf(List<Wf> conllWfs) {
        naf.createTextLayer(conllWfs, getName());
    }

    List<BaseEntity> compareToExisting(List<Entity> nafEntities, List<BaseEntity> entities) {
        List<BaseEntity> currentEntities = asBaseEntities(nafEntities);
        List<BaseEntity> removed = new LinkedList<>(currentEntities);
        removed.removeAll(entities);

        List<BaseEntity> added = new LinkedList<>(entities);
        added.removeAll(currentEntities);

        List<BaseEntity> diff = new LinkedList<>(removed);
        diff.addAll(added);

        Collections.sort(diff);
        logDifferences(removed, diff);
        return diff;
    }

    private void logDifferences(List<BaseEntity> removed, List<BaseEntity> diff) {
        StringBuilder sb = new StringBuilder();
        sb.append(naf.getPublicId()).append("\n");
        sb.append("found ").append(diff.size()).append(" differences between current and new entities:\n");
        int countRemoved = (int) diff.stream().filter(e -> removed.contains(e)).count();
        sb.append("removed ").append(countRemoved).append("; added ").append(diff.size() - countRemoved).append("\n");
        for (BaseEntity e: diff) {
            if (removed.contains(e))
                sb.append("removed\t");
            else
                sb.append("added\t");
            sb.append(naf.coveredText(e.begin(), e.end())).append("\t")
                    .append(e.begin()).append("\t")
                    .append(e.end()).append("\t")
                    .append(e.getType()).append("\t")
                    .append(naf.coveredText(e.begin() - CHAR_WINDOW, e.end() + CHAR_WINDOW).replaceAll("\n", " "))
                    .append("\n");
        }
        logger.info(sb.toString());
    }

    public static void run(Path file, List<String> dirs, String entitySource, String dataVersion, boolean replaceTokens, boolean addEntities) throws AbnormalProcessException {
            File refFile = IO.findFileWithSameId(file, new File(dirs.get(0)));
            File outFile = Paths.get(dirs.get(1), refFile.getName()).toFile();
            NAFConllReader nafConllReader = new NAFConllReader(refFile.getPath(), entitySource, dataVersion, replaceTokens, addEntities);
            nafConllReader.process(file.toString(), outFile.toString());
    }

    public String getName() {
        return Handler.NAME + "-" + entitySource + "-" + NAME;
    }
}
