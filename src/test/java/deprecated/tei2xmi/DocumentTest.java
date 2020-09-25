package deprecated.tei2xmi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import nafSelector.Tokenizer;
import utils.tei.Metadata;
import utils.tei.TeiReader;
import deprecated.utils.Segments;

import static org.junit.jupiter.api.Assertions.*;
@Deprecated
class DocumentTest {
    static String textFile;
    static String rawText;
    static TeiReader teiReader;

    @BeforeAll
    public static void init() throws AbnormalProcessException {
        textFile = "src/test/resources/tei2naf/short_text.xml";
        rawText = "III. ANTONIO VAN DIEMEN, IN HET SCHIP DEVENTER NABIJ DE ZUIDPUNT VAN AFRIKA 5 juni 1631 *).\n" +
                "1011, fol. 1-64.\n" +
                "(Af gedrukt Indisch Verslag^.\n" +
                "1) Daar de retourvloot begin maart 1631 diende te vertrekken en Specx zijn generale missive niet gereed had, sloot hij het begin (ons nr. II) op 7 maart af, terwijl hij aan Van Diemen opdroeg de brief gedurende de reis naar patria af te maken, waartoe hem de nodige stukken werden meegegeven. Van Diemen’s brief van 5 juni 1631 dient dus als een generale missive beschouwd te worden.";
        teiReader = new TeiReader(textFile, x -> TeiTreeFactory.create(x));
    }

    @Test
    public void testParagraphSegmentation() {
        Document document = Document.create(new TextFormatter(), null);
        document.formatParagraphs(teiReader.getTeiTree());
        assertEquals(document.getParagraphs().size(), 4);
        assertEquals(document.getRawText(), rawText);

    }

    @Test
    public void testTokenization() {

        Document document = Document.create(new TextFormatter(), null);
        document.formatParagraphs(teiReader.getTeiTree());
        Tokenizer tokenizer = null;
        try {
            tokenizer = Tokenizer.create();
        } catch (AbnormalProcessException e) {
            e.printStackTrace();
        }
        document.segmentAndTokenize(tokenizer);
        Segments sentences = document.getSentences();
        Segments tokens = document.getTokens();
        int startSentence2 = sentences.getSegments().get(1).getBegin();
        int startToken1InSentence2 = tokens.getSegments().get(2).getBegin();
        assertEquals(startSentence2, startToken1InSentence2);
        int startSentence1InParagraph2 = sentences.getSegments().get(2).getBegin();
        int startParagraph2 = document.getParagraphs().get(1).getOffset();
        assertEquals(startSentence1InParagraph2, startParagraph2);
        assertEquals(document.getSentences().endIndex(), rawText.length());

        assertEquals(document.sentenceCount(), 8);
        int indexFirstSentence = document.getSentences().getSegments().get(0).getIndex();
        int indexLastSentence = document.getSentences().getSegments().get(document.sentenceCount() - 1).getIndex();
        assertEquals(indexLastSentence, indexFirstSentence + document.sentenceCount() - 1);

        assertEquals(document.tokenCount(), 112);
        int indexFirstToken = document.getTokens().getSegments().get(0).getIndex();
        int indexLastToken = document.getTokens().getSegments().get(document.tokenCount() - 1).getIndex();
        assertEquals(indexLastToken, indexFirstToken + document.tokenCount() - 1);
    }

    @Test
    public void testMetadata() throws AbnormalProcessException {
        Metadata metadata = teiReader.getMetadata();
        assertEquals(metadata.getDocumentTitle(), "Van Diemen, Deventer, nabij de Zuidpunt van Afrika, 5 juni 1631");
        assertEquals(metadata.getDocumentId(), "INT_0ca18bf8-8884-3a20-93ff-55c46b62ccbf");
        assertEquals(metadata.getCollectionId(), "missiven:vol1");
    }
}
