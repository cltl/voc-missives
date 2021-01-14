package deprecated.nafReference;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.common.Span;
import utils.naf.NafCreator;
import utils.xmi.CasDoc;
import deprecated.utils.BaseEntity;
import deprecated.utils.BaseToken;
import deprecated.utils.BaseTokenAligner;
import utils.naf.NafDoc;
import deprecated.utils.Dkpro;
import xjc.naf.LinguisticProcessors;
import xjc.naf.Wf;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Integrates annotations coming from manual annotations in CAS XMI format.
 * 1. These annotations are separate for text and notes
 * 2. The text for the annotations does not match exactly, as it is based on older raw text
 * 3. Entity annotations in XMI are based on character offsets and may not match word boundaries
 * 4. Tokenization is based on the Inception tokenizer, so tokens do not match.
 */
@Deprecated
public class NafXmiReader implements NafEntityProcessor, NafSelector, NafCreator {
    String source;
    String textType;
    NafDoc naf;
    CasDoc xmi;
    private static final String IN = "." + IO.XMI_SFX;
    private static final String OUT = "." + IO.NAF_SFX;

    public static final Logger logger = LogManager.getLogger(NafXmiReader.class);
    /**
     * Maps indices from external to reference character offsets.
     */
    BaseTokenAligner aligner;
    /**
     * Numbers of tokens to overlook when text is missing from the external or reference document.
     * The selected figure was necessary for one of the tested files, where the external document missed a larger portion
     * of text.
     */
    static final int MAX_TOKEN_LOOK_AHEAD = 350;
    private final static String VERSION = "1.1";

    public NafXmiReader(String nafFile, String textType, String source) throws AbnormalProcessException {
        this.naf = NafDoc.create(nafFile);
        this.textType = textType;
        this.source = source;
    }

    public static void run(Path file, List<String> dirs, String textType, String source) throws AbnormalProcessException {
        String fileName = file.getFileName().toString();
        String inNote = "_notes" + IN;

        String extension = IN;
        if (fileName.endsWith(inNote))
            extension = inNote;

        if (fileName.endsWith(IN)) {
            String refFile = IO.getTargetFile(dirs.get(0), file, extension, OUT);
            String outFile = IO.getTargetFile(dirs.get(1), file, extension, OUT);

            NafXmiReader nafXmiReader = new NafXmiReader(refFile, textType, source);
            nafXmiReader.process(file.toString(), outFile);
        }
    }

    @Override
    public void alignTokens() {
        List<BaseToken> xmiTokens = xmi.getTokens().stream().map(Dkpro::asBaseToken).collect(Collectors.toList());
        List<BaseToken> nafTokens = selectedTokens(naf, textType).stream().map(t -> asBaseToken(t)).collect(Collectors.toList());
        aligner = BaseTokenAligner.create(xmiTokens, nafTokens, MAX_TOKEN_LOOK_AHEAD);
        aligner.align();
    }
    public static BaseToken asBaseToken(Wf token) {
        return BaseToken.create(token.getContent(), token.getId(), token.getOffset(), token.getLength());
    }

    @Override
    public String getName() {
        return "naf-xmi-reader-" + source;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public List<BaseEntity> readEntities(String input) throws AbnormalProcessException {
        xmi = CasDoc.create();
        xmi.read(input);
        alignTokens();
        return collectEntities();
    }

    @Override
    public NafDoc getNaf() {
        return naf;
    }

    @Override
    public void addLinguisticProcessor(String layer) {
        List<LinguisticProcessors> lps = naf.getLinguisticProcessorsList();
        LinguisticProcessors lp = createLinguisticProcessors(layer);
        List<LinguisticProcessors> existing = lps.stream().filter(x -> x.getLayer().equals(layer)).collect(Collectors.toList());
        if (existing.isEmpty())
            lps.add(lp);
        else
            existing.get(0).getLps().addAll(lp.getLps());
    }


    private List<BaseEntity> collectEntities() throws AbnormalProcessException {
        List<BaseEntity> collected = new LinkedList<>();

        for (NamedEntity e: xmi.getEntities()) {
            Span ref = aligner.getReferenceSpan(e.getBegin(), e.getEnd());
            if (ref.getFirstIndex() == -1) {
                if (ref.getLastIndex() != -1) {
                    ref = aligner.matchFromLastToken(e.getCoveredText(), ref);
                } else {
                    throw new AbnormalProcessException("null aligner span for " + e.toShortString());
                }
            } else if (ref.getLastIndex() == -1){
                ref = aligner.matchFromFirstToken(e.getCoveredText(), ref);
            }
            // TODO test
//            if (ref.getLength() > e.getEnd() - e.getBegin() + 10)
//                logger.warn("lengths differ between: " + e.getCoveredText() + " (Xmi) and " +
//                        naf.getRawText().substring(ref.getFirstIndex(), ref.getLastIndex() + 1) + " (NAF)");
            List<String> spannedTokenIds = aligner.getReferenceTokenSpanIds(ref.getFirstIndex(), ref.getLastIndex());

            collected.add(BaseEntity.create(e.getValue(), e.getIdentifier(), spannedTokenIds));
//            checkLengthsCompare(e, spannedTokenIds, naf);
        }
        return collected;
    }

    private void checkLengthsCompare(NamedEntity e, List<String> spannedTokenIds, NafDoc naf) {
        int length1 = e.getEnd() - e.getBegin() + 1;
        Wf last = naf.getWfs().get(Integer.parseInt(spannedTokenIds.get(spannedTokenIds.size() - 1).substring(1)));
        Wf first = naf.getWfs().get(Integer.parseInt(spannedTokenIds.get(0).substring(1)));
        int length2 = Integer.parseInt(last.getOffset()) + Integer.parseInt(last.getLength()) - Integer.parseInt(first.getOffset());
        if (Math.abs(length2 - length1) > 10)
            logger.warn("lengths differ between: " + e.getCoveredText() + " (" + e.getIdentifier() + " Xmi) and " +
                    naf.getRawText().substring(Integer.parseInt(first.getOffset()),
                            Integer.parseInt(last.getLength()) + Integer.parseInt(last.getOffset())) + " (" + first.getId() + " NAF)");
    }

}
