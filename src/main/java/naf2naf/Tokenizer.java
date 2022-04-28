package naf2naf;

import eus.ixa.ixa.pipe.cli.CLIArgumentsParser;
import eus.ixa.ixa.pipe.cli.Parameters;
import eus.ixa.ixa.pipe.ml.tok.RuleBasedSegmenter;
import eus.ixa.ixa.pipe.ml.tok.RuleBasedTokenizer;
import eus.ixa.ixa.pipe.ml.tok.Token;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import utils.common.AbnormalProcessException;
import utils.common.Pair;
import utils.naf.Wf;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static utils.naf.NafUnits.createWf;


public class Tokenizer {

    private Properties properties;
    private final static String VERSION = "2.0.0";

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
        CLIArgumentsParser argumentsParser = new CLIArgumentsParser(VERSION);
        Parameters parameters;
        try {
            parameters = argumentsParser.parse(args);
        } catch (ArgumentParserException e) {
            throw new AbnormalProcessException("Cannot parse Tokenizer parameters", e);
        }
        Properties properties = parameters.getAnnotateProperties();
        return new Tokenizer(properties);
    }

    public List<Wf> getWfs(List<Pair<Integer, String>> textFragments) {
        List<Wf> wfs = new LinkedList<>();
        int sentenceCounter = 0;
        int unitCounter = 0;
        for (Pair<Integer,String> t: textFragments) {
            String unitText = t.getSecond();
            List<List<Token>> tokenizedSentences = tokenize(unitText);
            for (List<Token> sentence: tokenizedSentences) {
                addTokens(sentence, wfs, t.getFirst(), sentenceCounter, unitCounter);
                sentenceCounter++;
            }
            unitCounter++;
        }
        return wfs;
    }


    private void addTokens(List<Token> tokens, List<Wf> wfs, int tunitOffset, int sentenceCounter, int unitCounter) {
        for (Token t: tokens)
            wfs.add(createWf(t.getTokenValue(), wfs.size(), tunitOffset + t.startOffset(), sentenceCounter, unitCounter));
    }

    public String version() {
        return VERSION;
    }
}
