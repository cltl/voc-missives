package entities.nafReference;

import utils.common.*;
import utils.naf.NafCreator;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import xjc.naf.LinguisticProcessors;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static entities.nafReference.NafXmiReader.MAX_TOKEN_LOOK_AHEAD;

/**
 * Reads entities from a Conll file and adds them to reference NAF
 * 1. We assume here that the NAF and Conll tokens are aligned
 * 2. Conll tokens are aligned to selected tokens
 */
public class NAFConllReader implements NafEntityProcessor, NafSelector, NafCreator {

    NafDoc naf;
    String textType;
    String conllSeparator;
    String source;
    BaseTokenAligner aligner;
    String input;
    private static final String IN = "." + IO.CONLL_SFX;
    private static final String OUT = "." + IO.NAF_SFX;
    private final static String VERSION = "1.1";

    public NAFConllReader(String nafFile, String textType, String conllSeparator, String source) {
        this.naf = NafDoc.create(nafFile);
        this.textType = textType;
        this.conllSeparator = conllSeparator;
        this.source = source;
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

    @Override
    public void alignTokens() throws AbnormalProcessException {
        List<BaseToken> conllTokens = read(input);
        List<BaseToken> nafTokens = selectedTokens(naf, textType).stream().map(NafUnits::asBaseToken).collect(Collectors.toList());
        aligner = BaseTokenAligner.create(conllTokens, nafTokens, MAX_TOKEN_LOOK_AHEAD);
        aligner.align();
    }

    /**
     * Reads conll tokens and labels into <code>BaseToken</code> objects,
     * where:
     * - <code>id</code> stores the Conll BIO label
     * - the character span is artificial (we assume all tokens are separated by a
     * single space), and is to be mapped to NAF character offsets.
     *
     * @param conllFile
     * @return
     * @throws AbnormalProcessException
     */
    public List<BaseToken> read(String conllFile) throws AbnormalProcessException {
        String line;
        List<BaseToken> tokens = new ArrayList<>();
        int offset = 0;
        try (BufferedReader bfr = new BufferedReader(new FileReader(conllFile))) {

            while ((line = bfr.readLine()) != null) {
                if (line.length() > 0) {
                    String[] tokenLabel = line.split(conllSeparator);
                    tokens.add(new BaseToken(tokenLabel[0],
                            tokenLabel[1],
                            new Span(offset, offset + tokenLabel[0].length() - 1)));
                    offset += tokenLabel[0].length();
                }
            }
        } catch (FileNotFoundException e) {
            throw new AbnormalProcessException(conllFile, e);
        } catch (IOException e) {
            throw new AbnormalProcessException(conllFile, e);
        }
        return tokens;
    }

    @Override
    public List<BaseEntity> readEntities(String input) throws AbnormalProcessException {
        this.input = input;
        alignTokens();
        return collectEntities();
    }

    private List<BaseEntity> collectEntities() {
        List<BaseToken> eTokens = aligner.getInputTokens().stream().filter(t -> !t.getId().equals("O")).collect(Collectors.toList());
        if (eTokens.isEmpty())
            return Collections.EMPTY_LIST;
        int first = eTokens.get(0).getFirstIndex();
        int last = eTokens.get(0).getLastIndex();
        String type = eTokens.get(0).getId().substring(2);
        List<BaseEntity> entities = new LinkedList<>();
        for (int i = 1; i < eTokens.size(); i++) {
            BaseToken t = eTokens.get(i);
            if (t.getId().startsWith("B")) {
                Span ref = aligner.getReferenceSpan(first, last); // align the previous token span
                List<String> spannedTokenIds = aligner.getReferenceTokenSpanIds(ref.getFirstIndex(), ref.getLastIndex());
                BaseEntity e = BaseEntity.create(type, "", spannedTokenIds);
                if (entities.contains(e)) {
                    //System.out.println("FIXME");
                    throw new IllegalArgumentException("Entity already collected: " + e.toString() + "\n Something must have gone wrong during token alignment.");

                }
                entities.add(e);
                first = t.getFirstIndex();
                type = t.getId().substring(2);
            }
            last = t.getLastIndex();
        }
        return entities;
    }


    public static void run(Path file, List<String> dirs, String textType, String conllSeparator, String source) throws AbnormalProcessException {
        String fileName = file.getFileName().toString();
        String inNote = "_notes" + IN;

        String extension = IN;
        if (fileName.endsWith(inNote))
            extension = inNote;

        if (fileName.endsWith(IN)) {
            String refFile = IO.append(dirs.get(0), IO.replaceExtension(file, extension, OUT));
            String outFile = IO.append(dirs.get(1), IO.replaceExtension(file, extension, OUT));

            NAFConllReader nafConllReader = new NAFConllReader(refFile, textType, conllSeparator, source);
            nafConllReader.process(file.toString(), outFile);
        }
    }

    @Override
    public String getName() {
        return "naf-conll-reader-" + source;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
