package deprecated.tei2xmi;


import eus.ixa.ixa.pipe.ml.tok.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.tei.TeiLeaf;
import nafSelector.Tokenizer;
import deprecated.utils.Segments;

import java.util.List;
@Deprecated
public class Paragraph {
    Segments sentences;
    Segments tokens;
    String content;
    String teiId;
    int offset;

    private static final Logger logger = LogManager.getLogger(Paragraph.class);
    public Paragraph(TeiLeaf paragraph, int offset) {
        this.content = paragraph.getContent();
        this.teiId = paragraph.getId();
        this.offset = offset;
        this.sentences = new Segments();
        this.tokens = new Segments();
    }


    public static Paragraph create(TeiLeaf paragraph, int offset) {
        return new Paragraph(paragraph, offset);
    }

    public String getContent() {
        return content;
    }

    public String getTeiId() {
        return teiId;
    }

    public Segments getTokens() {
        return tokens;
    }

    public Segments getSentences() {
        return sentences;
    }

    public int getOffset() {
        return offset;
    }

    public void tokenize(Tokenizer tokenizer, int nbSentences, int nbTokens) {
        List<List<Token>> tokenizedSentences = tokenizer.getTokens(getContent(), tokenizer.segment(getContent()));
        int startSentence;
        int sentenceIndex = 1;
        int tokenIndex = 0;
        for (List<Token> tokenizedSentence: tokenizedSentences) {
            if (tokenizedSentence.isEmpty())
                logger.warn("Attempting to tokenize empty sentence in paragraph " + getTeiId() + "; this sentence will be ignored.");
            else {
                startSentence = offset + tokenizedSentence.get(0).startOffset();
                for (Token t : tokenizedSentence) {
                    tokens.addSegment(offset + t.startOffset(), t.tokenLength(), nbTokens + tokenIndex);
                    tokenIndex++;
                }
                sentences.addSegment(startSentence, tokens.endIndex() - startSentence, nbSentences + sentenceIndex);
                sentenceIndex++;
            }
        }
    }
}
