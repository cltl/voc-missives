package sysIn2naf;

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
 * Reads entities from a Conll file and adds them to a reference NAF.
 * NAF and Conll tokens should be aligned.
 * This class is intended to be used to integrate system annotations
 * into a bare reference NAF, or to replace existing entities by a new
 * set of entities.
 * If an entities layer already exists, the differences between
 * both sets of entities are logged.
 */
public class NAFConllReader {

    private final static String NAME = "sys-in2naf";
    private final static int CHAR_WINDOW = 25;
    public static final Logger logger = LogManager.getLogger(NAFConllReader.class);
    NafHandler naf;
    boolean replaceEntities;
    String entityPfx;

    public NAFConllReader(String nafFile) throws AbnormalProcessException {
        this.naf = NafHandler.create(nafFile);
        this.replaceEntities = naf.hasEntitiesLayer();
        this.entityPfx = naf.entityPfx();
    }


    public NafHandler getNaf() {
        return naf;
    }

    public static List<String[]> conllTokens(String conllFile) throws AbnormalProcessException {
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


    static boolean startsEntity(String[] token) {
        return token[1].startsWith("B");
    }

    static boolean isInEntity(String[] token) {
        return token[1].startsWith("I");
    }

    String label(String[] token) {
        return token[1].split("-")[1];
    }

    protected List<Entity> readEntities(List<String[]> tokens) {
        List<Wf> wfs = naf.getWfs();
        int eIndex = 0;
        List<Entity> entities = new LinkedList<>();
        int i = 0;
        while (i < tokens.size()) {
            int j = 1;
            if (startsEntity(tokens.get(i))) {
                List<Wf> wfSpan = new LinkedList<>();
                wfSpan.add(wfs.get(i));
                while (i + j < tokens.size() && isInEntity(tokens.get(i + j))) {
                    wfSpan.add(wfs.get(i + j));
                    j++;
                }
                entities.add(NafUnits.createEntity(entityPfx + eIndex, label(tokens.get(i)), wfSpan));
                eIndex++;
            }
            i += j;
        }
        return entities;
    }

    protected void write(List<Entity> entities, String outFile) throws AbnormalProcessException {
        naf.createEntitiesLayer(entities, getName());
        naf.write(outFile);
    }

    private void process(String conllFile, String outFile) throws AbnormalProcessException {
        List<String[]> tokens = conllTokens(conllFile);
        List<Entity> entities = readEntities(tokens);
        if (tokens.size() != naf.getWfs().size())
            throw new AbnormalProcessException("tokens do not match: NAF has " + naf.getWfs().size() + ", input Conll has " + tokens.size());
        if (replaceEntities)
            compareToExisting(entities);
        else
            logger.info("adding new entities to NAF...");
        write(entities, outFile);
    }

    List<BaseEntity> compareToExisting(List<Entity> entities) {
        List<BaseEntity> newEntities = entities.stream().map(e -> BaseEntity.create(e)).collect(Collectors.toList());
        List<BaseEntity> currentEntities = naf.getEntities().stream().map(e -> BaseEntity.create(e)).collect(Collectors.toList());

        List<BaseEntity> removed = new LinkedList<>(currentEntities);
        removed.removeAll(newEntities);

        List<BaseEntity> added = new LinkedList<>(newEntities);
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

    public static void run(Path file, List<String> dirs) throws AbnormalProcessException {
            File refFile = IO.findFileWithSameId(file, new File(dirs.get(0)));
            File outFile = Paths.get(dirs.get(1), refFile.getName()).toFile();
            NAFConllReader nafConllReader = new NAFConllReader(refFile.getPath());
            nafConllReader.process(file.toString(), outFile.toString());
    }

    public String getName() {
        String name = Handler.NAME + "-" + NAME;
        if (replaceEntities)
            name += "-mod";
        return name;
    }

}
