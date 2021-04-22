package naf2conll;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.IO;
import utils.common.AbnormalProcessException;
import utils.naf.CharPosition;
import utils.naf.NafHandler;
import utils.naf.NafUnits;
import xjc.naf.Entity;
import utils.naf.Wf;
import xjc.naf.Tunit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts NAF files to Conll. Entities in the NAF are assumed to come from
 * manual annotations and may overlap.
 *  - BIO scheme
 *  - conll separator is a single space by default
 */
public class Naf2Conll {
    private static final String IN = "." + IO.NAF_SFX;
    private static final String OUT = "." + IO.CONLL_SFX;
    private static final String CONLL_SEP = " ";
    HashMap<Wf,Entity> wf2entity;
    NafHandler naf;
    int gpeCount;
    int embeddedEntityCount;
    public static final Logger logger = LogManager.getLogger(Naf2Conll.class);

    public Naf2Conll(String nafFile) throws AbnormalProcessException {
        this.wf2entity = new HashMap<>();
        this.naf = NafHandler.create(nafFile);
        this.gpeCount = 0;
        this.embeddedEntityCount = 0;
    }

    private String line(String token) {
        return token + CONLL_SEP + "O\n";
    }

    private String lines(List<String> tokens, String type) {
        StringBuilder str = new StringBuilder();
        str.append(tokens.get(0)).append(CONLL_SEP).append("B-").append(type).append("\n");
        for (int i = 1; i < tokens.size(); i++)
            str.append(tokens.get(i)).append(CONLL_SEP).append("I-").append(type).append("\n");
        return str.toString();
    }

    /**
     * adds new line and returns the new sentence index
     * @param sentence
     * @param wf
     * @param bw
     * @return
     * @throws IOException
     */
    private String update(String sentence, Wf wf, BufferedWriter bw) throws IOException {
        if (! wf.getSent().equals(sentence)) {
            bw.write("\n");
            return wf.getSent();
        }
        return sentence;
    }

    protected List<List<Wf>> segmentTokensBySentences() {
        List<List<Wf>> segments = new LinkedList<>();
        String sentence = naf.getWfs().get(0).getSent();
        List<Wf> segment = new LinkedList<>();
        for (Wf wf: naf.getWfs()) {
            if (! wf.getSent().equals(sentence)) {
                sentence = wf.getSent();
                if (! segment.isEmpty()) {
                    segments.add(segment);
                    segment = new LinkedList<>();
                }
            }
            segment.add(wf);
        }
        if (! segment.isEmpty())
            segments.add(segment);
        return segments;
    }

    protected List<List<Wf>> segmentTokensByTUnits() {
        List<List<Wf>> segments = new LinkedList<>();
        List<Tunit> tunits = naf.getTunits();
        int nextUnitOffset = CharPosition.create(tunits.get(0).getOffset(), tunits.get(0).getLength()).getEndIndex();
        List<Wf> segment = new LinkedList<>();
        for (Wf wf: naf.getWfs()) {
            if (Integer.parseInt(wf.getOffset()) >= nextUnitOffset) {
                tunits.remove(0);
                if (! tunits.isEmpty())
                    nextUnitOffset = CharPosition.create(tunits.get(0).getOffset(), tunits.get(0).getLength()).getEndIndex();
                if (! segment.isEmpty()) {
                    segments.add(segment);
                    segment = new LinkedList<>();
                }
            }
            segment.add(wf);
        }
        if (! segment.isEmpty())
            segments.add(segment);
        return segments;
    }

    /**
     * writes tokens and their labels out to Conll
     * @param outfile
     * @throws AbnormalProcessException
     */
    protected void write(List<List<Wf>> segments, String outfile) throws AbnormalProcessException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            if (wf2entity.isEmpty()) {
                for (List<Wf> segment: segments) {
                    for (Wf wf: segment)
                        bw.write(line(NafUnits.getContent(wf)));
                    bw.write("\n");
                }
            } else {
                for (List<Wf> wfs: segments) {
                    int i = 0;
                    while (i < wfs.size()) {
                        if (wf2entity.containsKey(wfs.get(i))) {
                            Entity e = wf2entity.get(wfs.get(i));
                            List<String> span = NafUnits.wfSpan(e).stream().map(wf -> NafUnits.getContent(wf)).collect(Collectors.toList());
                            bw.write(lines(span, e.getType()));
                            i += span.size();
                        } else {
                            bw.write(line(NafUnits.getContent(wfs.get(i))));
                            i++;
                        }
                    }
                    bw.write("\n");
                }
            }
        } catch (IOException e) {
            throw new AbnormalProcessException("Error writing to " + outfile, e);
        }
    }

    /**
     * Creates a list of non-overlapping entities from the NAF input
     * - duplicate entities LOC/ORG are merged to GPE
     * - the largest (first) entity is selected in other cases of overlap
     * @return
     */
    protected List<Entity> filterEntities() {
        for (Entity e: naf.getEntities()) {
            Wf firstWf = NafUnits.wfSpan(e).get(0);
            if (wf2entity.containsKey(firstWf)) {
                if (isGPE(wf2entity.get(firstWf), e)) {
                    wf2entity.get(firstWf).setType("GPE");
                    gpeCount++;
                } else {    // the first token of this entity is part of a previously recorded entity
                    embeddedEntityCount++;
                }
            } else {        // record all the tokens spanned by that entity
                NafUnits.wfSpan(e).forEach(w -> wf2entity.put(w, e));
            }
        }
        List<Entity> filtered = wf2entity.values().stream().distinct().collect(Collectors.toList());
        Collections.sort(filtered, Comparator.comparing(NafUnits::indexSpan));
        return filtered;
    }

    protected int getEntityCount() {
        return getEntities().size();
    }

    public int getGpeCount() {
        return gpeCount;
    }

    public int getEmbeddedEntityCount() {
        return embeddedEntityCount;
    }

    private boolean isGPE(Entity e1, Entity e2) {
        return e1.getType().equals("LOC") && e2.getType().equals("ORG")
                || e1.getType().equals("ORG") && e2.getType().equals("LOC");
    }

    public static void run(Path file, String outdir, boolean segmentByTUnits) throws AbnormalProcessException {
        String outfile = IO.getTargetFile(outdir, file, IN, OUT);
        Naf2Conll converter = new Naf2Conll(file.toString());
        converter.filterEntities();
        if (segmentByTUnits)
            converter.write(converter.segmentTokensByTUnits(), outfile);
        else
            converter.write(converter.segmentTokensBySentences(), outfile);
        converter.log();
    }

    private void log() {
        StringBuilder sb = new StringBuilder();
        sb.append(naf.getPublicId()).append(": ");
        sb.append("wrote ").append(getEntityCount()).append(" entities; ");
        sb.append("merged ").append(getGpeCount()).append(" GPE entities; ");
        sb.append("left ").append(getEmbeddedEntityCount()).append(" embedded entities");
        logger.info(sb.toString());
    }

    public List<Entity> getEntities() {
        return wf2entity.values().stream().distinct().collect(Collectors.toList());
    }
}
