package deprecated.tei2xmi;

import eus.ixa.ixa.pipe.ml.tok.Token;
import utils.common.AbnormalProcessException;
import org.junit.jupiter.api.Test;
import nafSelector.Tokenizer;
import utils.tei.ATeiTree;
import utils.tei.TeiLeaf;
import utils.tei.TeiReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@Deprecated
class TokenizerTest {
    @Test
    public void testTokenizer() throws AbnormalProcessException {
        String textFile = "src/test/resources/tei2naf/short_text.xml";
        String sentence1 = "III. ANTONIO VAN DIEMEN, IN HET SCHIP DEVENTER NABIJ DE ZUIDPUNT VAN AFRIKA 5 juni 1631 *).";
        String rawText = "III. ANTONIO VAN DIEMEN, IN HET SCHIP DEVENTER NABIJ DE ZUIDPUNT VAN AFRIKA 5 juni 1631 *).\n" +
                "1011, fol. 1-64.\n" +
                "(Af gedrukt Indisch Verslag^.\n" +
                "1) Daar de retourvloot begin maart 1631 diende te vertrekken en Specx zijn generale missive niet gereed had, sloot hij het begin (ons nr. II) op 7 maart af, terwijl hij aan Van Diemen opdroeg de brief gedurende de reis naar patria af te maken, waartoe hem de nodige stukken werden meegegeven. Van Diemen’s brief van 5 juni 1631 dient dus als een generale missive beschouwd te worden.";
        TeiReader teiReader = new TeiReader(textFile, x -> TeiTreeFactory.create(x));
        ATeiTree tree = teiReader.getTeiTree();
        Formatter formatter = new TextFormatter();
        List<TeiLeaf> paragraphs = formatter.format(tree);
        Tokenizer tokenizer = Tokenizer.create();
        String[] sentences = tokenizer.segment(paragraphs.get(0).getContent());
        assertEquals(sentences.length, 2);
        assertEquals(sentences[0].length(), 4);
        assertEquals(sentence1.length(), sentences[0].length() + sentences[1].length());

        List<List<Token>> tokens = tokenizer.getTokens(paragraphs.get(0).getContent(), sentences);
        assertEquals(tokens.get(0).size(), 2);
        assertEquals(tokens.get(0).get(0).tokenLength(), 3);
        assertEquals(tokens.get(0).get(1).startOffset(), 3);
        assertEquals(tokens.get(1).get(0).startOffset(), 5);
    }

}