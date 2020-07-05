package naf2conll;

import missives.Handler;
import missives.IO;
import tei2naf.Fragment;
import tei2naf.NafDoc;
import missives.AbnormalProcessException;
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
 * Options:
 *  - every token set to O or read from entities if present
 *  - select notes/text or all (ignore forewords)
 *  - conll separator
 */
public class Naf2Conll {
    private static final String IN = "." + Handler.NAF_SFX;
    private static final String OUT = "." + Handler.CONLL_SFX;

    String conllSeparator;
    /**
     * used to select document sections:
     * - 'text' -> selects 'p' and 'head' sections (excluding notes embedded in paragraphs)
     * - 'notes' -> selects 'notes' sections
     * - 'mixed' -> selects 'p', 'head' and 'notes' sections
     * - 'all' -> selects all sections ('p', 'head', 'notes' and 'fw')
     */
    String selectText;
    HashMap<Entity,List<Wf>> entities2wfs;
    HashMap<Wf,Entity> wfs2entities;
    NafDoc naf;

    public Naf2Conll(String conllSeparator, String selectText, String nafFile) {
        this.conllSeparator = conllSeparator;
        this.selectText = selectText;
        entities2wfs = new HashMap<>();
        wfs2entities = new HashMap<>();
        this.naf = NafDoc.create(nafFile);
    }

    private String line(String token) {
        return token + conllSeparator + "O\n";
    }

    private String lines(String type, List<String> tokens) {
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

    /**
     * Selects tokens belonging to text/notes/mixed/all depending on <code>selectText</code>
     * @return
     */
    public List<Wf> selectedTokens() {
        return naf.selectTokens(selectText);
    }

    private void write(String outfile, List<Wf> wfs) throws AbnormalProcessException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            String sentence = wfs.get(0).getSent();
            if (entities2wfs.isEmpty()) {
                for (Wf wf: wfs) {
                    sentence = update(sentence, wf, bw);
                    bw.write(line(wf.getContent()));
                }
            } else {
                int i = 0;
                while (i < wfs.size()) {
                    // we assume here than entities do not cross sentence boundaries
                    sentence = update(sentence, wfs.get(i), bw);
                    if (wfs2entities.containsKey(wfs.get(i))) {
                        Entity e = wfs2entities.get(wfs.get(i));
                        List<String> span = entities2wfs.get(e).stream().map(Wf::getContent).collect(Collectors.toList());
                        bw.write(lines(e.getType(), span));
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

    private void readEntities() {
        List<Entity> entities = naf.getEntities();
        if (! entities.isEmpty()) {

            for (Entity e: entities) {
                List<Wf> span = naf.entitySpan(e);
                entities2wfs.put(e, span);
                wfs2entities.put(span.get(0), e);
            }
        }
    }

    public static void run(Path file, String outdir, String conllSeparator, String selectText) throws AbnormalProcessException {
        String fileId = IO.replaceExtension(file, IN, OUT);
        String outfile = outdir + "/" + fileId;
        Naf2Conll converter = new Naf2Conll(conllSeparator, selectText, file.toString());
        converter.readEntities();
        List<Wf> selectedTokens = converter.selectedTokens();
        if (! selectedTokens.isEmpty())
            converter.write(outfile, selectedTokens);
    }
}
