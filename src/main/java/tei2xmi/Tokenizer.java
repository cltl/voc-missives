package tei2xmi;

import eus.ixa.ixa.pipe.cli.CLIArgumentsParser;
import eus.ixa.ixa.pipe.cli.Parameters;
import eus.ixa.ixa.pipe.ml.tok.RuleBasedSegmenter;
import eus.ixa.ixa.pipe.ml.tok.RuleBasedTokenizer;
import eus.ixa.ixa.pipe.ml.tok.Token;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.AbnormalProcessException;
import utils.Paragraph;

import java.util.List;
import java.util.Properties;


public class Tokenizer {

    private Properties properties;
    private static final Logger logger = LogManager.getLogger(Tokenizer.class);

    private Tokenizer(Properties properties) {
        this.properties = properties;
    }

    public String[] segment(String text) {
        RuleBasedSegmenter segmenter = new RuleBasedSegmenter(text, properties);
        return segmenter.segmentSentence();
    }


    public List<List<Token>> getTokens(String text, String[] sentences) {
        RuleBasedTokenizer tokenizer = new RuleBasedTokenizer(text, properties);
        return tokenizer.tokenize(sentences);
    }


    public static Tokenizer create() throws AbnormalProcessException {
        String[] args = {"tok", "-l", "nl"};
        String version = "2.0.0";
        CLIArgumentsParser argumentsParser = new CLIArgumentsParser(version);
        Parameters parameters;
        try {
            parameters = argumentsParser.parse(args);
        } catch (ArgumentParserException e) {
            throw new AbnormalProcessException("Cannot parse Tokenizer parameters", e);
        }
        Properties properties = parameters.getAnnotateProperties();
        return new Tokenizer(properties);
    }

    public void tokenize(Paragraph paragraph, int nbSentences, int nbTokens) {

        List<List<Token>> tokenizedSentences = getTokens(paragraph.getContent(), segment(paragraph.getContent()));
        int startSentence;
        int sentenceIndex = 1;
        int tokenIndex = 0;
        for (List<Token> tokenizedSentence: tokenizedSentences) {
            if (tokenizedSentence.isEmpty())
                logger.warn("Attempting to tokenize empty sentence in paragraph " + paragraph.getTeiId() + "; this sentence will be ignored.");
            else {
                startSentence = paragraph.getOffset() + tokenizedSentence.get(0).startOffset();
                for (Token t : tokenizedSentence) {
                    paragraph.getTokens().addSegment(paragraph.getOffset() + t.startOffset(), t.tokenLength(), nbTokens + tokenIndex);
                    tokenIndex++;
                }
                paragraph.getSentences().addSegment(startSentence, paragraph.getTokens().endIndex() - startSentence, nbSentences + sentenceIndex);
                sentenceIndex++;
            }
        }
    }
}
