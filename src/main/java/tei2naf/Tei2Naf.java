package tei2naf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.Fragment;
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
import java.util.LinkedList;
import java.util.List;

import static utils.common.ThrowingBiConsumer.throwingBiConsumerWrapper;

/**
 * Converts TEI document to input NAF: raw text and all text units
 *
 */
public class Tei2Naf implements NafCreator {
    private static final String IN = "." + IO.TEI_SFX;
    private static final String OUT = "." + IO.NAF_SFX;
    private final static String NAME = "tei2naf";
    private final static String VERSION = "0.1.2";
    public static final Logger logger = LogManager.getLogger(Tei2Naf.class);
    BaseDoc doc;

    public Tei2Naf() { }

    private NafDoc convertBaseDocToNafRepresentation() {
        NafDoc naf = new NafDoc();
        NafHeader nafHeader = new NafHeader();
        FileDesc fileDesc = new FileDesc();
        fileDesc.setTitle(doc.getMetadata().getDocumentTitle());
        fileDesc.setFilename(doc.getMetadata().getDocumentId());
        nafHeader.setFileDesc(fileDesc);
        Public pub = new Public();
        pub.setPublicId(doc.getMetadata().getDocumentId() + ".naf");
        nafHeader.setPublic(pub);

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

    public void process(String teiFile, String outdir) throws AbnormalProcessException {
        read(teiFile);
        NafDoc naf = convertBaseDocToNafRepresentation();
        naf.write(outdir + naf.getId() + OUT);
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
     * Test
     * @param teiFile
     * @return
     * @throws AbnormalProcessException
     */
    protected List<Fragment> getFragments(String teiFile) throws AbnormalProcessException {
        TeiReader teiReader = new TeiReader(teiFile, x -> TeiInputTreeFactory.create(x));
        ATeiTree tree = teiReader.getTeiTree();
        return mapElementPositions(tree);
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
        fragments = loopElementPositions(tree, thread, fragments, 0);
        fragments.remove(0); // removes root element
        return fragments;
    }

    private List<Fragment> loopElementPositions(ATeiTree tree, ArrayDeque<AnchoredNode> thread, List<Fragment> fragments, int offsetFromParent) {
        AnchoredNode anchoredNode;
        if (thread.isEmpty())
            anchoredNode = AnchoredNode.createRoot(tree);
        else
            anchoredNode = AnchoredNode.create(tree, thread.peek(), offsetFromParent);

        if (tree.getTeiType() != ATeiTree.TeiType.STR)
            fragments.add(new Fragment(anchoredNode.getId(), anchoredNode.getOffset(), anchoredNode.length()));

        if (tree instanceof TeiDiv) {
            thread.push(anchoredNode);
            List<ATeiTree> children = assignIds(tree, ((TeiDiv) tree).getChildren());
            int relativeOffset = 0;
            for (ATeiTree child: children) {
                loopElementPositions(child, thread, fragments, relativeOffset);
                relativeOffset += child.yield().length();
            }
            thread.pop();
        }
        return fragments;
    }

    /**
     * assigns ID to elements other than leaf strings, if the ID is missing (this
     * currently applies to 'lb' and 'hi' TEI elements).
     * @param parent
     * @param children
     * @return
     */
    private List<ATeiTree> assignIds(ATeiTree parent, List<ATeiTree> children) {
        if (children.stream().anyMatch(x -> x.getId() == null && x.getTeiType() != ATeiTree.TeiType.STR)) {
            int hiCounter = 1;
            int lbCounter = 1;
            String pfx = parent.getId();
            for (ATeiTree child: children) {
                if (child.getId() == null) {
                    if (child.getTeiType() == ATeiTree.TeiType.HI) {
                        child.setId(pfx + ".hi." + hiCounter);
                        hiCounter++;
                    } else if (child.getTeiType() == ATeiTree.TeiType.LB) {
                        child.setId(pfx + ".lb." + lbCounter);
                        lbCounter++;
                    } else if (child.getTeiType() != ATeiTree.TeiType.STR)
                        throw new IllegalArgumentException("found unexpected TEI type with null id: " + child.getTeiType().name());
                }
            }
        }
        return children;
    }

    public static void convertFile(Path file, String outdir) throws AbnormalProcessException {
        Tei2Naf converter = new Tei2Naf();
        converter.process(file.toString(), outdir + "/");
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
