package text.tei2inputNaf;

import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import text.tei2naf.BaseDoc;
import text.tei2naf.TeiRawTreeFactory;
import text.tei2naf.TokenizedSection;
import tokens.Tokenizer;
import utils.common.AbnormalProcessException;
import utils.common.CharPosition;
import utils.common.Fragment;
import utils.common.IO;
import utils.naf.NafCreator;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import utils.tei.ATeiTree;
import utils.tei.Metadata;
import utils.tei.TeiDiv;
import utils.tei.TeiReader;
import xjc.naf.*;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.common.ThrowingBiConsumer.throwingBiConsumerWrapper;

/**
 * Converts TEI document to input NAF: raw text and all text units
 *
 */
public class InputNafConverter implements NafCreator {
    private static final String IN = "." + IO.TEI_SFX;
    private static final String OUT = "." + IO.NAF_SFX;
    private final static String NAME = "tei2inputnaf";
    private final static String VERSION = "0.1";
    public static final Logger logger = LogManager.getLogger(InputNafConverter.class);
    BaseDoc doc;

    public InputNafConverter() { }

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

        return naf;
    }

    public void process(String teiFile, String nafFile) throws AbnormalProcessException {
        read(teiFile);
        NafDoc naf = convertBaseDocToNafRepresentation();
        naf.write(nafFile);
    }

    private void read(String teiFile) throws AbnormalProcessException {
        TeiReader teiReader = new TeiReader(teiFile, x -> TeiInputTreeFactory.create(x));
        Metadata metadata = teiReader.getMetadata();
        ATeiTree tree = teiReader.getTeiTree();
        String rawText = tree.yield();

        List<Fragment> sections = mapElementPositions(tree);

        doc = BaseDoc.create(rawText, metadata, sections);
    }

    /**
     * Lists nodes in prefix order with their character offsets.
     *
     * @param tree
     * @return  list of nodes as <code>Fragment</code> objects, containing id and character offsets
     */
    private List<Fragment> mapElementPositions(ATeiTree tree) {
        ArrayDeque<AnchoredNode> thread = new ArrayDeque<>();
        List<Fragment> fragments = new LinkedList<>();
        fragments = loopElementPositions(tree, thread, fragments);
        return fragments;
    }

    private List<Fragment> loopElementPositions(ATeiTree tree, ArrayDeque<AnchoredNode> thread, List<Fragment> fragments) {
        AnchoredNode anchoredNode;
        if (thread.isEmpty())
            anchoredNode = AnchoredNode.createRoot(tree);
        else
            anchoredNode = AnchoredNode.create(tree, thread.peek());

        if (tree.getId() != null)
            fragments.add(new Fragment(anchoredNode.getId(), anchoredNode.getOffset(), anchoredNode.length()));

        if (tree instanceof TeiDiv) {
            thread.push(anchoredNode);
            for (ATeiTree child: ((TeiDiv) tree).getChildren())
                loopElementPositions(child, thread, fragments);
            thread.pop();
        }
        return fragments;
    }

    public static void convertFile(Path file, String outdir) throws AbnormalProcessException {
        String fileId = IO.replaceExtension(file, IN, OUT);
        String outfile = outdir + "/" + fileId;
        InputNafConverter converter = new InputNafConverter();
        converter.process(file.toString(), outfile);
    }

    public static void main(String[] args) {
        IO.loop(args[0], args[1],
                throwingBiConsumerWrapper((x, y) -> convertFile(x, y)));
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
