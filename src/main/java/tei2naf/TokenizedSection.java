package tei2naf;

import eus.ixa.ixa.pipe.ml.tok.Token;
import tei2xmi.Tokenizer;

import java.util.ArrayList;
import java.util.List;

public class TokenizedSection {
    String rawText;
    String compactText;
    List<Integer> rawOffsets;
    List<CharPosition> sentencePositions;
    List<CharPosition> tokenPositions;

    private TokenizedSection(String fragmentText) {
        this.rawText = fragmentText;
        this.rawOffsets = new ArrayList<>();
        this.sentencePositions = new ArrayList<>();
        this.tokenPositions = new ArrayList<>();
    }

    public List<CharPosition> getSentencePositions() {
        return sentencePositions;
    }

    public List<CharPosition> getTokenPositions() {
        return tokenPositions;
    }

    public static TokenizedSection create(String fragmentText, int fragmentOffset, Tokenizer tokenizer) {
        TokenizedSection rst = new TokenizedSection(fragmentText);
        rst.trimWhitespaceSequences();
        rst.tokenize(fragmentOffset, tokenizer);
        return rst;
    }

    private void tokenize(int fragmentOffset, Tokenizer tokenizer) {
        String[] sentenceStrings = tokenizer.segment(compactText);
        List<List<Token>> tokenized = tokenizer.getTokens(compactText, sentenceStrings);
        for (List<Token> tokens: tokenized) {
            int sentenceStart = rawOffsets.get(tokens.get(0).startOffset());
            int sentenceEnd = rawOffsets.get(tokens.get(tokens.size() - 1).startOffset()
                    + tokens.get(tokens.size() - 1).tokenLength() - 1) + 1;
            sentencePositions.add(new CharPosition(sentenceStart + fragmentOffset, sentenceEnd - sentenceStart));
            tokens.forEach(t -> tokenPositions.add(new CharPosition(rawOffsets.get(t.startOffset()) + fragmentOffset, t.tokenLength())));

        }
    }

    /**
     * Maps the raw text to a compact text, with whitespace sequences replaced by a
     * single whitespace. The <code>rawOffsets</code> allow to map the compact text
     * back to raw text indices; whitespace offsets are included in the list but
     * should not be used as they may correspond to several positions in the raw text.
     */
    private void trimWhitespaceSequences() {
        boolean ws = false;
        StringBuilder ctb = new StringBuilder();
        for (int i = 0; i < rawText.length(); i++) {
            if (Character.isWhitespace(rawText.charAt(i))) {
                if (ws)
                    continue;
                else {
                    ctb.append(" ");
                    rawOffsets.add(i);      // maps the *first* whitespace in a sequence
                    ws = true;
                }
            } else {
                ctb.append(rawText.charAt(i));
                rawOffsets.add(i);
                ws = false;
            }
        }
        compactText = ctb.toString();
    }
}
