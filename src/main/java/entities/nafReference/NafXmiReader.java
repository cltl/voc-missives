package entities.nafReference;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.common.Span;
import utils.naf.NafCreator;
import utils.xmi.CasDoc;
import utils.common.BaseEntity;
import utils.common.BaseToken;
import utils.common.BaseTokenAligner;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import utils.xmi.Dkpro;

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
public class NafXmiReader implements NafEntityProcessor, NafSelector, NafCreator {
    String source;
    String textType;
    NafDoc naf;
    CasDoc xmi;
    private static final String IN = "." + IO.XMI_SFX;
    private static final String OUT = "." + IO.NAF_SFX;
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

    public NafXmiReader(String nafFile, String textType, String source) {
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
            String refFile = IO.append(dirs.get(0), IO.replaceExtension(file, extension, OUT));
            String outFile = IO.append(dirs.get(1), IO.replaceExtension(file, extension, OUT));

            NafXmiReader nafXmiReader = new NafXmiReader(refFile, textType, source);
            nafXmiReader.process(file.toString(), outFile);
        }
    }

    @Override
    public void alignTokens() throws AbnormalProcessException {
        List<BaseToken> xmiTokens = xmi.getTokens().stream().map(Dkpro::asBaseToken).collect(Collectors.toList());
        List<BaseToken> nafTokens = selectedTokens(naf, textType).stream().map(NafUnits::asBaseToken).collect(Collectors.toList());
        aligner = BaseTokenAligner.create(xmiTokens, nafTokens, MAX_TOKEN_LOOK_AHEAD);
        try {
            aligner.align();
        } catch (IllegalArgumentException e) {
            // FIXME
            aligner = BaseTokenAligner.create(xmiTokens, nafTokens, MAX_TOKEN_LOOK_AHEAD);
            aligner.align();
            throw new AbnormalProcessException("Error processing file " + xmi.getId(), e);
        }
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
        naf.getNafHeader().getLinguisticProcessors().add(createLinguisticProcessors(layer));
    }


    private List<BaseEntity> collectEntities() throws IllegalArgumentException {
        List<BaseEntity> collected = new LinkedList<>();
        for (NamedEntity e: xmi.getEntities()) {
            Span ref = aligner.getReferenceSpan(e.getBegin(), e.getEnd());
            if (ref.getFirstIndex() == -1 || ref.getLastIndex() < ref.getFirstIndex())
                throw new IllegalArgumentException("Found entity with invalid reference span " + ref.toString()
                        + "; " + e.getCoveredText() + "@ " + e.getBegin() + "-" + e.getEnd());
            List<String> spannedTokenIds = aligner.getReferenceTokenSpanIds(ref.getFirstIndex(), ref.getLastIndex());
            collected.add(BaseEntity.create(e.getValue(), e.getIdentifier(), spannedTokenIds));
        }
        return collected;
    }

}
