package tei2naf;

import missives.AbnormalProcessException;
import missives.Handler;
import missives.IO;
import missives.NafProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tei2xmi.*;
import utils.*;
import xjc.naf.*;
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
public class NafConverter implements NafProcessor {
    private static final String IN = "." + Handler.TEI_SFX;
    private static final String OUT = "." + Handler.NAF_SFX;
    private final static String NAME = "voc-missives-tei-naf-converter";
    private final static String VERSION = "1.1";
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


    private NafDoc convertBaseDocToNafRepresentation() {

        NafDoc naf = new NafDoc();
        NafHeader nafHeader = new NafHeader();
        FileDesc fileDesc = new FileDesc();
        fileDesc.setTitle(doc.getMetadata().getDocumentTitle());
        fileDesc.setFilename(doc.getMetadata().getDocumentId());
        nafHeader.setFileDesc(fileDesc);
        String rawText = doc.getRawText();
        Raw raw = new Raw(rawText);
        nafHeader.getLinguisticProcessors().add(createLinguisticProcessors("raw"));

        Tunits tunits = new Tunits();
        doc.getSections().forEach(p -> tunits.getTunits().add(naf.createTunit(p)));
        nafHeader.getLinguisticProcessors().add(createLinguisticProcessors("tunits"));

        List<Object> layers = naf.getLayers();
        layers.add(nafHeader);
        layers.add(raw);
        layers.add(tunits);

        if (! doc.getTokens().isEmpty()) {
            Text text = getWfs();
            nafHeader.getLinguisticProcessors().add(createLinguisticProcessors("text"));
            layers.add(text);
        }
        return naf;
    }

    private Text getWfs() {
        Text text = new Text();
        List<Fragment> sentences = doc.getSentences();
        List<Fragment> tokens = doc.getTokens();
        int endIndex = sentences.remove(0).getEndIndex();
        int sentID = 0;
        for (Fragment t: tokens) {
            if (t.getEndIndex() > endIndex && ! sentences.isEmpty()) {
                endIndex = sentences.remove(0).getEndIndex();
                sentID++;
            }
            text.getWves().add(NafDoc.createWf(doc.getString(t), sentID, t));
        }
        return text;
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
        NafDoc naf = convertBaseDocToNafRepresentation();
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


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
