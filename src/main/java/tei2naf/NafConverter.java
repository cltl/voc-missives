package tei2naf;

import missives.AbnormalProcessException;
import missives.Handler;
import missives.IO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tei2xmi.*;
import utils.*;
import xjc.teiAll.TEI;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static missives.ThrowingBiConsumer.throwingBiConsumerWrapper;

/**
 * Converts TEI document to NAF
 *
 * 01/07/20 Raw text layer extraction, and writing to XMI
 */
public class NafConverter {
    private static final String IN = "." + Handler.TEI_SFX;
    private static final String OUT = "." + Handler.NAF_SFX;
    boolean tokenize;
    public static final Logger logger = LogManager.getLogger(NafConverter.class);
    BaseDoc doc;

    public NafConverter(boolean tokenize) {
        this.tokenize = tokenize;
    }

    /**
     * Extracts raw text from TEI file
     * @param teiFile
     * @return
     */
    public String extractText(String teiFile) throws AbnormalProcessException {
        read(teiFile);
        return doc.getRawText();
    }

    public BaseDoc getBaseDoc(String teiFile) throws AbnormalProcessException {
        read(teiFile);
        return doc;
    }

    private void process(String teiFile) throws AbnormalProcessException {
        read(teiFile);
        if (tokenize) {
            Tokenizer tokenizer = Tokenizer.create();
            for (Fragment section: doc.getNonOverlappingSections()) {
                if (section.getLength() > 0) {
                    TokenizedSection ts = TokenizedSection.create(doc.getString(section), section.getOffset(), tokenizer);
                    doc.updateSentences(ts.getSentencePositions());
                    doc.updateTokens(ts.getTokenPositions());
                }
            }
        }
    }

    private void read(String teiFile) throws AbnormalProcessException {
        TEI tei = Converter.load(teiFile);
        Metadata metadata = Metadata.create(tei);

        ATeiTree tree = TeiRawTreeFactory.create(tei);
        String rawText = tree.yield();

        BaseFormatter bf = new BaseFormatter();
        List<ATeiTree> subtrees = bf.selectSectionSubtrees(tree);
        List<String> subtreeYields = subtrees.stream().map(s -> s.yield()).collect(Collectors.toList());

        // mapping non-overlapping subtrees with a coverage check
        List<CharPosition> positions = mapPositions(subtreeYields, rawText);
        List<Fragment> sections = Fragment.zip(subtrees.stream().map(ATeiTree::getId).collect(Collectors.toList()), positions);

        // add embedded notes and their positions
        sections.addAll(mapEmbeddedPositions(subtrees, subtreeYields, sections));
        Fragment.sort(sections);

        doc = BaseDoc.create(rawText, metadata, sections);
    }

    private List<Fragment> mapEmbeddedPositions(List<ATeiTree> subtrees, List<String> yields, List<Fragment> sections) {
        List<Fragment> embeddedNotes = new ArrayList<>();
        for (int i = 0; i < subtrees.size(); i++) {
            if (subtrees.get(i).getTeiType() == ATeiTree.TeiType.P) {
                List<ATeiTree> notes = subtrees.get(i).getTopNodes(t -> t.getTeiType() == ATeiTree.TeiType.NOTE);
                if (! notes.isEmpty()) {
                    int index = 0;      // just in case embedded notes could have the same string in a same paragraph...
                    for (ATeiTree n: notes) {
                        String y = n.yield();
                        int offset = yields.get(i).indexOf(y, index);
                        embeddedNotes.add(new Fragment(n.getId(), offset+ sections.get(i).getOffset(), y.length()));
                        index = offset + y.length();
                    }
                }
            }
        }
        return embeddedNotes;
    }

    private List<CharPosition> mapPositions(List<String> sectionYields, String treeYield) {
        List<CharPosition> positions = new LinkedList<>();
        int i = 0;
        for (String y: sectionYields) {
            int index = treeYield.indexOf(y, i);
            if (index < 0)
                throw new IllegalArgumentException("cannot find subyield: " + y);
            if (treeYield.substring(i, index).trim().length() > 0)
                throw new IllegalArgumentException("undetected content: " + treeYield.substring(i, index));
            positions.add(new CharPosition(index, y.length()));
            i = index + y.length();
        }
        return positions;
    }


    public void toNaf(String nafFile) {
        NafDoc naf = new NafDoc();
        naf.read(doc);
        naf.write(nafFile);
    }

    public static void convertFile(Path file, String outdir, boolean tokenize) throws AbnormalProcessException {
        String fileId = IO.replaceExtension(file, IN, OUT);
        String outfile = outdir + "/" + fileId;
        NafConverter converter = new NafConverter(tokenize);
        converter.process(file.toString());
        converter.toNaf(outfile);
    }

    public static void main(String[] args) {

        IO.loop(args[0], args[1],
                throwingBiConsumerWrapper((x, y) -> convertFile(x, y, args[2].length() > 0)));
    }


}
