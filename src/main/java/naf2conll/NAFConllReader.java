package naf2conll;

import missives.AbnormalProcessException;
import missives.Handler;
import missives.IO;
import missives.NafProcessor;
import tei2naf.NafDoc;
import xjc.naf.Wf;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads entities from a Conll file and adds them to reference NAF
 * 1. We assume here that the NAF and Conll tokens are aligned
 * 2. Conll tokens are aligned to selected tokens
 */
public class NAFConllReader implements NafProcessor {
    NafDoc naf;
    String textType;
    String conllSeparator;
    String source;
    private final static String VERSION = "1.1";
    public NAFConllReader(String nafFile, String textType, String conllSeparator, String source) {
        this.naf = NafDoc.create(nafFile);
        this.textType = textType;
        this.conllSeparator = conllSeparator;
        this.source = source;
    }

    public NafDoc getNaf() {
        return naf;
    }

    public void read(String conllFile) throws AbnormalProcessException {
        List<String> conllTokens = conllTokens(conllFile);
        List<Wf> nafTokens = naf.selectTokens(textType);
        if (conllTokens.size() != nafTokens.size())
            throw new AbnormalProcessException("Unaligned tokens: found " + conllTokens.size() + " in " + conllFile +
                    ", but " + nafTokens.size() + " in NAF.", new IllegalArgumentException());

        List<BaseEntity> entities = naf.getBaseEntities();
        entities.addAll(collectEntities(nafTokens, conllTokens));
        naf.setEntities(BaseEntity.sortAndRenameForNaf(entities));
    }

    public List<BaseEntity> collectEntities(List<Wf> nafTokens, List<String> conllTokens) throws AbnormalProcessException {
        int i = 0;
        List<BaseEntity> collected = new ArrayList<>();
        List<String> entitySpan = new LinkedList<>();
        String entityType = null;
        while (i < nafTokens.size()) {
            if (! conllTokens.get(i).endsWith("O")) {
                String[] wordLabel = conllTokens.get(i).split(conllSeparator);
                if (wordLabel[1].charAt(0) == 'I')
                    entitySpan.add(nafTokens.get(i).getId());
                else if (wordLabel[1].charAt(0) == 'B') {
                    entityType = wordLabel[1].substring(2);
                    if (!entitySpan.isEmpty()) {
                        collected.add(BaseEntity.create(entityType, "", entitySpan));
                        entitySpan.clear();
                    }
                    entitySpan.add(nafTokens.get(i).getId());
                }
                else
                    throw new AbnormalProcessException("Unexpected Conll Label: " + wordLabel, new IllegalArgumentException());
            } else if (!entitySpan.isEmpty()) {
                collected.add(BaseEntity.create(entityType, "", entitySpan));
                entitySpan.clear();
            }
            i++;
        }
        if (!entitySpan.isEmpty())
            collected.add(BaseEntity.create(entityType, "", entitySpan));
        return collected;
    }

    public List<String> conllTokens(String conllFile) throws AbnormalProcessException {
        try (BufferedReader bfr = new BufferedReader(new FileReader(new File(conllFile)))) {
            return bfr.lines().filter(x -> x.length() > 0).collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            throw new AbnormalProcessException(conllFile, e);
        } catch (IOException e) {
            throw new AbnormalProcessException(conllFile, e);
        }
    }

    private void write(String outFile) {
        naf.write(outFile);
    }

    public static void run(Path file, List<String> dirs, String textType, String conllSeparator, String source) throws AbnormalProcessException {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(Handler.CONLL_SFX)) {
            String refFile = IO.append(dirs.get(0), fileName);
            String outFile = IO.append(dirs.get(1), fileName);

            NAFConllReader nafConllReader = new NAFConllReader(refFile, textType, conllSeparator, source);
            nafConllReader.read(file.toString());
            nafConllReader.write(outFile);
        }
    }

    public List<BaseEntity> getBaseEntities() {
        return naf.getBaseEntities();
    }

    @Override
    public String getName() {
        return "naf-conll-reader-" + source;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
