package naf2tsv;

import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.BaseEntity;
import utils.naf.NafHandler;
import utils.naf.NafUnits;
import xjc.naf.Tunit;
import xjc.naf.Entity;
import utils.naf.Wf;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Outputs NAF entities to TSV under the following assumptions:
 * - entities come from NER system and do not overlap
 * Entity ids are modified to guarantee unique ids over the entire corpus for TF.
 */
public class Naf2Tsv {
    static final String IN = "." + IO.NAF_SFX;
    static final String OUT = "." + IO.TSV_SFX;
    private final static int CHAR_WINDOW = 25;

    NafHandler naf;
    String entityPfx;
    boolean forTF;

    public Naf2Tsv(String nafFile, boolean forTF) throws AbnormalProcessException {
        this.naf = NafHandler.create(nafFile);
        this.entityPfx = this.naf.entityPfx();
        this.forTF = forTF;
    }


    public NafHandler getNaf() {
        return naf;
    }

    String getTFLine(String begin, String end, String entityId, String entityKind) {
        StringBuilder sb = new StringBuilder();
        sb.append(begin).append("\t").append(end).append("\t").append(entityId).append("\t").append(entityKind);
        return sb.toString();
    }

    String getLine(Entity entity) {
        BaseEntity e = BaseEntity.create(entity);
        if (forTF)
            return getTFLine(e.begin() + "", e.end() + "", entity.getId(), e.getType());
        else
            return getContextLine(naf.coveredText(e.begin(), e.end()), e.begin() + "", e.end() + "", e.getType(),
                    naf.coveredText(e.begin() - CHAR_WINDOW, e.end() + CHAR_WINDOW).replaceAll("\n", " "));
    }

    String getContextLine(String mention, String begin, String end, String type, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append(mention).append("\t").append(begin).append("\t").append(end).append("\t")
                .append(type).append("\t").append(context);
        return sb.toString();
    }

    private String getHeader() {
        if (forTF)
            return getTFLine("begin", "end", "entityId", "entityKind");
        else
            return getContextLine("mention", "begin", "end", "type", "context");
    }

    public List<String> getLines() {
        List<String> lines = new LinkedList<>();
        lines.add(getHeader());
        lines.addAll(naf.getEntities().stream().map(this::getLine).collect(Collectors.toList()));
        return lines;
    }

    void write(List<String> lines, String outfile) throws AbnormalProcessException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            for (String line: lines)
                bw.write(line + "\n");
        } catch (IOException e) {
            throw new AbnormalProcessException("error writing " + outfile);
        }
    }

    public static void run(Path file, String outdir, boolean forTF) throws AbnormalProcessException {
        String outfile = IO.getTargetFile(outdir, file, IN, OUT);
        Naf2Tsv converter = new Naf2Tsv(file.toString(), forTF);
        List<String> lines = converter.getLines();
        converter.write(lines, outfile);
    }
}
