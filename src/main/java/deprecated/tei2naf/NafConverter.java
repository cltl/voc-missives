package deprecated.tei2naf;

import tei2naf.BaseDoc;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import nafSelector.Tokenizer;
import utils.naf.CharPosition;
import utils.naf.Fragment;
import utils.tei.ATeiTree;
import utils.tei.Metadata;
import utils.tei.TeiReader;
import xjc.naf.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.common.ThrowingBiConsumer.throwingBiConsumerWrapper;

/**
 * Converts TEI document to NAF
 *
 * 01/07/20 Raw text layer extraction, and writing to XMI
 */
@Deprecated
public class NafConverter implements NafCreator {
    private static final String IN = "." + IO.TEI_SFX;
    private static final String OUT = "." + IO.NAF_SFX;
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
        doc.getSections().forEach(p -> tunits.getTunits().add(NafUnits.asTunit(p)));
        nafHeader.getLinguisticProcessors().add(createLinguisticProcessors("tunits"));

        List<Object> layers = naf.getLayers();
        layers.add(nafHeader);
        layers.add(raw);
        layers.add(tunits);

        if (! doc.getTokens().isEmpty()) {
            nafHeader.getLinguisticProcessors().add(createLinguisticProcessors("text"));
            layers.add(createTextLayer());
        }
        return naf;
    }

    private Text createTextLayer() {
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
            text.getWves().add(createWf(doc.getString(t), sentID, t));
        }
        return text;
    }

    public static Wf createWf(String wordForm, int sentID, Fragment t) {
        Wf wf = new Wf();
        wf.setId("w" + t.getId());
        wf.setSent(sentID + "");
        wf.setContent(wordForm);
        wf.setOffset(t.getOffset() + "");
        wf.setLength(t.getLength() + "");
        return wf;
    }

    public void process(String teiFile, String nafFile) throws AbnormalProcessException {
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

        NafDoc naf = convertBaseDocToNafRepresentation();
        naf.write(nafFile);
    }

    List<ATeiTree> selectSectionSubtrees(ATeiTree tei) {
        return tei.getTopNodes(t ->  t.isForeword() || t.isNote() || t.isParagraph() || t.isHead() || t.isTable());
    }
    private void read(String teiFile) throws AbnormalProcessException {
        TeiReader teiReader = new TeiReader(teiFile, x -> TeiRawTreeFactory.create(x));
        Metadata metadata = teiReader.getMetadata();
        ATeiTree tree = teiReader.getTeiTree();
        String rawText = tree.yield();

        List<ATeiTree> subtrees = selectSectionSubtrees(tree);
        List<String> subtreeYields = subtrees.stream().map(s -> s.yield()).collect(Collectors.toList());

        // mapping non-overlapping subtrees with a coverage check
        List<CharPosition> positions = mapPositions(subtreeYields, rawText);
        List<Fragment> sections = Fragment.zip(subtrees.stream().map(ATeiTree::getId).collect(Collectors.toList()), positions);

        // add embedded notes and forewords and their positions
        sections.addAll(mapEmbeddedPositions(subtrees, subtreeYields, sections));
        Fragment.sort(sections);

        doc = BaseDoc.create(rawText, metadata, sections);
    }

    private List<Fragment> mapEmbeddedPositions(List<ATeiTree> subtrees, List<String> yields, List<Fragment> sections) {
        List<Fragment> embeddedNodes = new ArrayList<>();
        for (int i = 0; i < subtrees.size(); i++) {
            if (subtrees.get(i).isParagraph()) {
                List<ATeiTree> nodes = subtrees.get(i).getTopNodes(t -> t.isNote() || t.isForeword() || t.isTable());
//                List<ATeiTree> nodes2 = new ArrayList<>();
//                for (ATeiTree n: nodes) {
//                    nodes2.add(n);
//                    nodes2.addAll(n.getAllNodes(ATeiTree::isForeword));
//                }
                if (! nodes.isEmpty()) {
                    addToEmbeddedNodes(yields, sections, embeddedNodes, i, nodes);
                }
            } else if (subtrees.get(i).isTable() || subtrees.get(i).isNote()) {
                List<ATeiTree> forewords = subtrees.get(i).getTopNodes(t -> t.isForeword());
                if (! forewords.isEmpty()) {
                    addToEmbeddedNodes(yields, sections, embeddedNodes, i, forewords);
                }
            }
            //FIXME: 2-level embeddings P > note > Fw || P > Table > Fw
        }

        return embeddedNodes;
    }

    /**
     * FIXME find a clearer way to map indices for level2 nodes
     * @param yields
     * @param sections
     * @param embeddedNodes
     * @param i
     * @param notes
     */
    private void addToEmbeddedNodes(List<String> yields, List<Fragment> sections, List<Fragment> embeddedNodes, int i, List<ATeiTree> notes) {
        int index = 0;      // just in case embedded nodes could have the same string in a same paragraph...
        for (ATeiTree n: notes) {
            String y = n.yield();
            int offset = yields.get(i).indexOf(y, index);
            embeddedNodes.add(new Fragment(n.getId(), offset+ sections.get(i).getOffset(), y.length()));
            index = offset + y.length();
        }
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

    public static void convertFile(Path file, String outdir, boolean tokenize) throws AbnormalProcessException {
        String fileId = IO.replaceExtension(file, IN, OUT);
        String outfile = outdir + "/" + fileId;
        NafConverter converter = new NafConverter(tokenize);
        converter.process(file.toString(), outfile);
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
