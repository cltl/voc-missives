package xmi;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.CasDoc;
import utils.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Writes out entities in XMI doc to CoNLL 2002 format.
 */
public class Conll {
    CasDoc doc;
    final static String Conll2Sep = " ";
    final static String ConllUSep = "\t";
    final static Logger logger = LogManager.getLogger(Conll.class);
    String sep;

    public Conll(CasDoc doc) {
        this.doc = doc;
        this.sep = Conll2Sep;
    }

    public static Conll create(String xmi) {
        CasDoc doc = CasDoc.create();
        doc.read(xmi);
        return new Conll(doc);
    }

    public void write(String outfile) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
            write(bw);
        } catch (IOException e) {
            logger.fatal("Error writing to " + outfile, e);
        }
    }

    public void write(BufferedWriter writer) throws IOException {
        List<NamedEntity> entities = filterDisjoint(doc.getEntities());
        List<Token> tokens = doc.getTokens();
        ListIterator<Sentence> sentIter = doc.getSentences().listIterator();
        int eos = sentIter.next().getEnd();
        NamedEntity e = entities.isEmpty() ? null : entities.remove(0);
        ListIterator<Token> iter = tokens.listIterator();

        while (iter.hasNext()) {
            Token t = iter.next();
            if (t.getEnd() > eos) {
                eos = sentIter.next().getEnd();
                writer.write("\n");
            }
            if (e != null && t.getEnd() > e.getEnd())
                e = entities.isEmpty() ? null : entities.remove(0);
            if (e == null || t.getBegin() < e.getBegin())
                writer.write(defaultLabel(t));
            else if (t.getBegin() == e.getBegin())
                writer.write(beginLabel(t, e));
            else if (t.getEnd() <= e.getEnd())
                writer.write(innerLabel(t, e));
        }
        writer.close();
    }

    private String innerLabel(Token t, NamedEntity e) {
        return t.getText() + sep + "I-" + e.getValue() + "\n";
    }

    private String beginLabel(Token t, NamedEntity e) {
        return t.getText() + sep + "B-" + e.getValue() + "\n";
    }

    private String defaultLabel(Token t) {
        return t.getText() + sep + "O\n";
    }

    private List<NamedEntity> filterDisjoint(List<NamedEntity> entities) {
        if (entities.size() < 2)
            return entities;
        ListIterator<NamedEntity> iter = entities.listIterator();
        LinkedList<NamedEntity> filtered = new LinkedList<>();
        filtered.add(iter.next());
        while (iter.hasNext()) {
            NamedEntity e = iter.next();
            NamedEntity previous = filtered.getLast();
            if (areDisjoint(e, previous))
                filtered.add(e);
            else if (isLonger(e, previous)) {
                filtered.removeLast();
                filtered.add(e);
            }
        }
        return filtered;
    }

    private boolean areDisjoint(NamedEntity e, NamedEntity other) {
        return other.getEnd() <= e.getBegin() || e.getEnd() <= other.getBegin();
    }

    private boolean isLonger(NamedEntity e, NamedEntity other) {
        return e.getEnd() - e.getBegin() > other.getEnd() - other.getBegin();
    }

    public static void run(Path file, String outdir) {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".xmi")) {
            String outFile = IO.append(outdir, fileName.replaceAll("\\.xmi", ".conll"));
            Conll conll = Conll.create(file.toString());
            conll.write(outFile);
        }
    }

    public static void main(String[] args) {
        IO.loop(args[0], args[1], (x, y) -> run(x, y));
    }
}
