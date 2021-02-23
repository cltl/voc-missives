package naf2naf;

import eus.ixa.ixa.pipe.ml.tok.Token;
import javafx.util.Pair;
import missives.Handler;
import utils.common.AbnormalProcessException;
import utils.naf.NafUnits;
import utils.naf.NafHandler;
import xjc.naf.Tunit;
import xjc.naf.NafHeader;
import xjc.naf.FileDesc;
import xjc.naf.Public;
import utils.naf.Wf;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static utils.naf.NafUnits.createWf;

/**
 * Selects tunits from an input NAF to derive text/notes NAF
 */
public class NafUnitSelector  {
    String type;
    private static final boolean TOKENIZE = true;
    NafHandler inputNaf;
    NafHandler derivedNaf;
    private final static String NAME = "selector";

    public NafUnitSelector(String inputNaf, String type) throws AbnormalProcessException {
        this.type = checkType(type);
        this.inputNaf = NafHandler.create(inputNaf);
        this.derivedNaf = transferHeader();
    }

    public NafHandler getDerivedNaf() {
        return derivedNaf;
    }

    private String checkType(String type) {
        if (! (type.equals("text") || type.equals("notes") || type.equals("all")))
            throw new IllegalArgumentException("Invalid type: " + type + "; type should be either 'text', 'notes' or 'all'");
        return type;
    }

    public void extractTextTunitsAndTokens() throws AbnormalProcessException {

        List<Tunit> tunits = filter(inputNaf);

        String rawText = deriveRawText(tunits, inputNaf.getRawText());
        derivedNaf.createRawLayer(rawText, getName());
        derivedNaf.createTunitsLayer(tunits, getName());

        if (TOKENIZE && !tunits.isEmpty())
            createTextLayer(derivedNaf, rawText, tunits);
    }

    private void createTextLayer(NafHandler derivedNaf, String rawText, List<Tunit> tunits) throws AbnormalProcessException {
        Tokenizer tokenizer = Tokenizer.create();
        List<Pair<Integer,String>> textFragments = joinCohesive(tunits, rawText);
        List<Wf> wfs = getWfs(tokenizer, textFragments);
        derivedNaf.createTextLayer(wfs, getName());
    }

    private List<Wf> getWfs(Tokenizer tokenizer, List<Pair<Integer, String>> textFragments) {
        List<Wf> wfs = new LinkedList<>();
        int sentenceCounter = 0;
        int unitCounter = 0;
        for (Pair<Integer,String> t: textFragments) {
            String unitText = t.getValue();
            List<List<Token>> tokenizedSentences = tokenizer.tokenize(unitText);
            for (List<Token> sentence: tokenizedSentences) {
                addTokens(sentence, wfs, t.getKey(), sentenceCounter, unitCounter);
                sentenceCounter++;
            }
            unitCounter++;
        }
        return wfs;
    }

    /**
     * Regroups consecutive tunits with a same xpath for tokenization (units that were originally
     * interrupted by a filtered element).
     * This does not change the tunits themselves, but may result in a tokenization paragraph
     * (as given in the NAF wf `para` attribute) matching different tunits.
     * @param tunits
     * @param rawText
     * @return
     */
    protected static List<Pair<Integer,String>> joinCohesive(List<Tunit> tunits, String rawText) {
        List<Pair<Integer,String>> cohesive = new LinkedList<>();
        String xpath = tunits.get(0).getXpath();
        Pair<Integer,String> pair = getUnitOffsetAndText(rawText, tunits.get(0));
        for (Tunit current: tunits.subList(1, tunits.size())) {
            if (current.getXpath().equals(xpath)) {
                pair = new Pair<>(pair.getKey(), pair.getValue() + coveredText(current, rawText));
            } else {
                cohesive.add(pair);
                pair = getUnitOffsetAndText(rawText, current);
                xpath = current.getXpath();
            }
        }
        cohesive.add(pair);
        return cohesive;
    }

    private static Pair<Integer, String> getUnitOffsetAndText(String rawText, Tunit tunit) {
        return new Pair<>(Integer.parseInt(tunit.getOffset()), coveredText(tunit, rawText));
    }

    private static String coveredText(Tunit previous, String rawText) {
        int offset = Integer.parseInt(previous.getOffset());
        return rawText.substring(offset, offset + Integer.parseInt(previous.getLength()));
    }

    private void addTokens(List<Token> tokens, List<Wf> wfs, int tunitOffset, int sentenceCounter, int unitCounter) {
        for (Token t: tokens)
            wfs.add(createWf(t.getTokenValue(), wfs.size(), tunitOffset + t.startOffset(), sentenceCounter, unitCounter));
    }

    protected static String deriveRawText(List<Tunit> tunits, String rawText) {
        StringBuilder sb = new StringBuilder();
        for (Tunit t: tunits) {
            String[] offsetLength = t.getId().substring(t.getId().indexOf(".co") + 3).split("-");
            int offset = Integer.parseInt(offsetLength[0]);
            int endIndex = offset + Integer.parseInt(offsetLength[1]);
            String yield = rawText.substring(offset, endIndex);
            sb.append(yield);
        }
        return sb.toString();
    }

    /**
     * Filters textual tunits matching <code>type</code>
     */
    private List<Tunit> filter(NafHandler inputNaf) {
        List<Tunit> tunits = inputNaf.getTunits();
        // removes hi, lb, pb
        tunits = filterNonDisruptiveTextualElements(tunits);
        // builds a tree of the tunits
        TunitTree tree = TunitTree.create(tunits);
        // selects nodes (notes or text); excised notes or fw may split the tree
        List<Tunit> frontierNodes;
        if (type.equals("notes")) {
            frontierNodes = filterNotes(tree);
        } else if (type.equals("text")){
            frontierNodes = filterText(tree);
        } else
            frontierNodes = filterAll(tree);
        return updateSpans(frontierNodes);
    }

    protected static List<Tunit> filterAll(TunitTree tree) {
        return tree.extractTunits(x -> false);
    }

    protected static List<Tunit> filterText(TunitTree tree) {
        Predicate<TunitTree> textFilter = x -> x.getNode().getType().equals("note") || x.getNode().getType().equals("fw");
        return tree.extractTunits(textFilter);
    }

    protected static List<Tunit> filterNotes(TunitTree tree) {
        Predicate<Tunit> noteFilter = x -> x.getType().equals("note");
        Predicate<TunitTree> fwFilter = x -> x.getNode().getType().equals("fw");
        List<TunitTree> selected = tree.filterTopNodes(noteFilter);
        List<Tunit> tunits = new LinkedList<>();
        selected.forEach(t -> tunits.addAll(t.extractTunits(fwFilter)));
        return tunits;
    }

    protected static List<Tunit> updateSpans(List<Tunit> tunits) {
        List<Tunit> updated = new LinkedList<>();
        int i = 0;
        for (Tunit t: tunits) {
            updated.add(NafUnits.withOffset(t, i));
            i += Integer.parseInt(t.getLength());
        }
        return updated;
    }
    /**
     * Removes non textual elements: lb, pb, hi
     * @param tunits
     * @return
     */
    private List<Tunit> filterNonDisruptiveTextualElements(List<Tunit> tunits) {
        java.util.function.Predicate<String> isNonDisruptive = x -> ! (x.equals("lb") || x.equals("pb") || x.equals("hi"));
        return tunits.stream().filter(t -> isNonDisruptive.test(t.getType())).collect(Collectors.toList());
    }

    protected NafHandler transferHeader() {
        NafHeader header = new NafHeader();
        String derivedId;
        derivedId = inputNaf.getNafHeader().getPublic().getPublicId() + "_" + type;
        FileDesc fd = new FileDesc();
        fd.setFilename(derivedId + ".naf");
        header.setFileDesc(fd);
        Public pub = new Public();
        pub.setPublicId(derivedId);
        header.setPublic(pub);
        header.getLinguisticProcessors().addAll(inputNaf.getLinguisticProcessorsList());
        return NafHandler.create(header);
    }

    protected void writeDerivedNaf(String outdir) throws AbnormalProcessException {
        derivedNaf.write(Paths.get(outdir, derivedNaf.getFileName()).toString());
    }

    public static void run(Path file, String outdir, String documentType) throws AbnormalProcessException {
        NafUnitSelector converter = new NafUnitSelector(file.toString(), documentType);
        converter.extractTextTunitsAndTokens();
        converter.writeDerivedNaf(outdir);
    }

    public String getName() {
        return Handler.NAME + "-" + type + "-" + NAME;
    }

}
