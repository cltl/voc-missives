package tokens;

import eus.ixa.ixa.pipe.cli.CLIArgumentsParser;
import eus.ixa.ixa.pipe.cli.Parameters;
import eus.ixa.ixa.pipe.ml.tok.RuleBasedSegmenter;
import eus.ixa.ixa.pipe.ml.tok.RuleBasedTokenizer;
import eus.ixa.ixa.pipe.ml.tok.Token;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import utils.common.AbnormalProcessException;

import java.util.List;
import java.util.Properties;


public class Tokenizer {

    private Properties properties;

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

    public List<List<Token>> tokenize(String text) {
        String[] sentences = segment(text);
        return getTokens(text, sentences);
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


}
