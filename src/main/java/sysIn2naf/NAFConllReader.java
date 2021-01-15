package sysIn2naf;

import missives.Handler;
import utils.common.*;
import utils.naf.NafCreator;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import xjc.naf.Entity;
import xjc.naf.Wf;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Reads entities from a Conll file and adds them to reference NAF
 * The NAF and Conll tokens should be aligned.
 */
public class NAFConllReader implements NafCreator {

    NafDoc naf;
    private final static String NAME = "sys-in2naf";

    public NAFConllReader(String nafFile) throws AbnormalProcessException {
        this.naf = NafDoc.create(nafFile);
        checkNafHasNoEntities();
    }

    private void checkNafHasNoEntities() throws AbnormalProcessException {
        if (! naf.getEntities().isEmpty())
            throw new AbnormalProcessException("This NAF file already contains entities! Refusing to overwrite");
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
        if (tokens.size() != wfs.size())
            throw new IllegalArgumentException("tokens do not match: NAF has " + wfs.size() + ", input Conll has " + tokens.size());
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
                entities.add(NafUnits.createEntity("e" + eIndex, label(tokens.get(i)), wfSpan));
                eIndex++;
            }
            i += j;
        }
        return entities;
    }

    protected void write(List<Entity> entities, String outFile) throws AbnormalProcessException {
        createEntitiesLayer(naf, entities);
        naf.write(outFile);
    }

    private void process(String conllFile, String outFile) throws AbnormalProcessException {
        List<String[]> tokens = conllTokens(conllFile);
        List<Entity> entities = readEntities(tokens);
        write(entities, outFile);
    }

    public static void run(Path file, List<String> dirs) throws AbnormalProcessException {

            File refFile = IO.findFileWithSameId(file, new File(dirs.get(0)));
            File outFile = Paths.get(dirs.get(1), refFile.getName()).toFile();
            NAFConllReader nafConllReader = new NAFConllReader(refFile.getPath());
            nafConllReader.process(file.toString(), outFile.toString());
    }

    @Override
    public String getName() {
        return Handler.NAME + "-" + NAME;
    }

}
