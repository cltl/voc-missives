package naf2conll;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.IO;
import utils.common.AbnormalProcessException;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import xjc.naf.Entity;
import xjc.naf.Wf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts NAF files to Conll
 *  - every token set to O or read from entities if present
 *  - conll separator
 */
public class Naf2Conll {
    private static final String IN = "." + IO.NAF_SFX;
    private static final String OUT = "." + IO.CONLL_SFX;

    String conllSeparator;
    HashMap<Wf,Entity> wf2entity;
    NafDoc naf;
    int gpeCount;
    int embeddedEntityCount;
    public static final Logger logger = LogManager.getLogger(Naf2Conll.class);

    public Naf2Conll(String conllSeparator, String nafFile) throws AbnormalProcessException {
        this.conllSeparator = conllSeparator;
        this.wf2entity = new HashMap<>();
        this.naf = NafDoc.create(nafFile);
        this.gpeCount = 0;
        this.embeddedEntityCount = 0;
    }

    private String line(String token) {
        return token + conllSeparator + "O\n";
    }

    private String lines(List<String> tokens, String type) {
        StringBuilder str = new StringBuilder();
        str.append(tokens.get(0)).append(conllSeparator).append("B-").append(type).append("\n");
        for (int i = 1; i < tokens.size(); i++)
            str.append(tokens.get(i)).append(conllSeparator).append("I-").append(type).append("\n");
        return str.toString();
    }

    private String update(String sentence, Wf wf, BufferedWriter bw) throws IOException {
        if (! wf.getSent().equals(sentence)) {
            bw.write("\n");
            return wf.getSent();
        }
        return sentence;
    }

    protected void write(String outfile) throws AbnormalProcessException {
        List<Wf> wfs = naf.getWfs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            String sentence = wfs.get(0).getSent();
            if (wf2entity.isEmpty()) {
                for (Wf wf: wfs) {
                    sentence = update(sentence, wf, bw);
                    bw.write(line(wf.getContent()));
                }
            } else {
                int i = 0;
                while (i < wfs.size()) {
                    // we assume here than entities do not cross sentence boundaries
                    sentence = update(sentence, wfs.get(i), bw);
                    if (wf2entity.containsKey(wfs.get(i))) {
                        Entity e = wf2entity.get(wfs.get(i));
                        List<String> span = NafUnits.wfSpan(e).stream().map(Wf::getContent).collect(Collectors.toList());
                        bw.write(lines(span, e.getType()));
                        i += span.size();
                    } else {
                        bw.write(line(wfs.get(i).getContent()));
                        i++;
                    }
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
                } else {
                    // else the first entered entity naturally precedes and contains the new one
                    embeddedEntityCount++;
                }
            } else {
                NafUnits.wfSpan(e).forEach(w -> wf2entity.put(w, e));
            }
        }
        return wf2entity.values().stream().distinct().collect(Collectors.toList());
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

    public static void run(Path file, String outdir, String conllSeparator) throws AbnormalProcessException {
        String outfile = IO.getTargetFile(outdir, file, IN, OUT);
        Naf2Conll converter = new Naf2Conll(conllSeparator, file.toString());
        converter.filterEntities();
        converter.write(outfile);
        converter.log();
    }

    private void log() {
        StringBuilder sb = new StringBuilder();
        sb.append("wrote ").append(getEntityCount()).append(" entities; ");
        sb.append("merged ").append(getGpeCount()).append(" GPE entities; ");
        sb.append("left ").append(getEmbeddedEntityCount()).append(" embedded entities");
        logger.info(sb.toString());
    }

    public List<Entity> getEntities() {
        return wf2entity.values().stream().distinct().collect(Collectors.toList());
    }
}
