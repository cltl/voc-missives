package naf2tsv;

import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.NafHandler;
import utils.naf.NafUnits;
import xjc.naf.Tunit;
import xjc.naf.Entity;
import utils.naf.Wf;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Outputs NAF entities to TSV under the following assumptions:
 * - entities come from NER system and do not overlap
 * Entity ids are modified to guarantee unique ids over the entire corpus for TF.
 */
public class Naf2Tsv {
    static final String IN = "." + IO.NAF_SFX;
    static final String OUT = "." + IO.TSV_SFX;
    private static final Pattern XPATH = Pattern.compile("volume\\[(\\d+)\\]/missive\\[(\\d+)\\]");

    NafHandler naf;
    String entityPfx;

    public Naf2Tsv(String nafFile) throws AbnormalProcessException {
        this.naf = NafHandler.create(nafFile);
        getVolumeAndMissiveId();
    }

    public NafHandler getNaf() {
        return naf;
    }

    private String getType(String tunitType) throws AbnormalProcessException {
        if (tunitType.equals("remark") || tunitType.equals("footnote"))
            return "n";     // notes
        else if (tunitType.equals("header") || tunitType.equals("paragraph"))
            return "t";     // text
        else throw new AbnormalProcessException("unrecognized tunit type: " + tunitType);
    }

    private void getVolumeAndMissiveId() throws AbnormalProcessException {
        if (! naf.getTunits().isEmpty()) {
            Tunit tunit = naf.getTunits().get(0);
            Matcher m = XPATH.matcher(tunit.getXpath());
            if (m.find()) {
                String volume = m.group(1);
                String missive = m.group(2);
                entityPfx = "e_" + getType(tunit.getType()) + volume + "_" + missive + "_";
            } else
                entityPfx = "e";
        }
    }

    String getLine(String begin, String end, String entityId, String entityKind) {
        StringBuilder sb = new StringBuilder();
        sb.append(begin).append("\t").append(end).append("\t").append(entityId).append("\t").append(entityKind).append("\n");
        return sb.toString();
    }

    String getLine(Entity e) {
        List<Wf> tokens = NafUnits.wfSpan(e);
        Wf last = tokens.get(tokens.size() - 1);
        int endIndex = Integer.parseInt(last.getOffset()) + Integer.parseInt(last.getLength());
        String id = entityPfx + e.getId().substring(1);
        return getLine(tokens.get(0).getOffset(), endIndex + "", id, e.getType());
    }

    void write(String outfile) throws AbnormalProcessException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            bw.write(getLine("begin", "end", "entityId", "entityKind"));
            for (Entity e: naf.getEntities())
                bw.write(getLine(e));
        } catch (IOException e) {
            throw new AbnormalProcessException("error writing " + outfile);
        }
    }

    public static void run(Path file, String outdir) throws AbnormalProcessException {
        String outfile = IO.getTargetFile(outdir, file, IN, OUT);
        Naf2Tsv converter = new Naf2Tsv(file.toString());
        converter.write(outfile);
    }
}
