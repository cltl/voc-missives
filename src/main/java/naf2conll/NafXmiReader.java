package naf2conll;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import missives.AbnormalProcessException;
import missives.Handler;
import missives.IO;
import missives.NafProcessor;
import tei2naf.NafDoc;
import utils.CasDoc;
import utils.Span;
import xmi.TokenAligner;

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
public class NafXmiReader implements NafProcessor {
    String source;
    String textType;
    NafDoc naf;
    CasDoc xmi;
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
        if (fileName.endsWith(Handler.XMI_SFX)) {
            String refFile = IO.append(dirs.get(0), fileName);
            String outFile = IO.append(dirs.get(1), fileName);

            NafXmiReader nafXmiReader = new NafXmiReader(refFile, textType, source);
            nafXmiReader.read(file.toString());
            nafXmiReader.align();
            nafXmiReader.addEntities();
            nafXmiReader.write(outFile);
        }
    }

    private void align() {
        List<BaseToken> xmiTokens = xmi.getTokens().stream().map(BaseToken::create).collect(Collectors.toList());
        List<BaseToken> nafTokens = naf.selectTokens(textType).stream().map(BaseToken::create).collect(Collectors.toList());
        aligner = BaseTokenAligner.create(xmiTokens, nafTokens, MAX_TOKEN_LOOK_AHEAD);
        aligner.align();
    }

    private void addEntities() {
        List<BaseEntity> entities = naf.getBaseEntities();
        entities.addAll(collectEntities());
        naf.setEntities(BaseEntity.sortAndRenameForNaf(entities));
    }

    private void write(String outFile) {
        naf.write(outFile);
    }

    private void read(String xmiFile) throws AbnormalProcessException {
        xmi = CasDoc.create();
        xmi.read(xmiFile);
    }

    @Override
    public String getName() {
        return "naf-xmi-reader-" + source;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    private List<BaseEntity> collectEntities() throws IllegalArgumentException {
        List<BaseEntity> collected = new LinkedList<>();
        for (NamedEntity e: xmi.getEntities()) {
            Span ref = aligner.getReferenceSpan(e.getBegin(), e.getEnd());
            if (ref.getBegin() == -1 || ref.getEnd() < ref.getBegin())
                throw new IllegalArgumentException("Found entity with invalid reference span " + ref.toString()
                        + "; " + e.getCoveredText() + "@ " + e.getBegin() + "-" + e.getEnd());
            collected.add(BaseEntity.create(e));
        }
        return collected;
    }

}
