package text.tei2naf;

import utils.tei.Metadata;
import utils.common.CharPosition;
import utils.common.Fragment;

import java.util.LinkedList;
import java.util.List;

public class BaseDoc {
    String rawText;
    Metadata metadata;
    List<Fragment> sections;
    List<Fragment> sentences;
    List<Fragment> tokens;

    private BaseDoc(Metadata metadata, String rawText, List<Fragment> sections) {
        this.rawText = rawText;
        this.metadata = metadata;
        this.sections = sections;
        this.sentences = new LinkedList<>();
        this.tokens = new LinkedList<>();
    }

    public static BaseDoc create(String rawText, Metadata metadata, List<Fragment> sections) {
        return new BaseDoc(metadata, rawText, sections);
    }

    public String getString(Fragment f) {
        return rawText.substring(f.getOffset(), f.getEndIndex());
    }

    public List<Fragment> getSections() {
        return sections;
    }

    public void updateSentences(List<CharPosition> sentencePositions) {
        sentences = Fragment.addAllWithIndexIdentifiers(sentences, sentencePositions);
    }

    public void updateTokens(List<CharPosition> tokenPositions) {
        tokens = Fragment.addAllWithIndexIdentifiers(tokens, tokenPositions);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getRawText() {
        return rawText;
    }

    public List<Fragment> getSentences() {
        return sentences;
    }

    public List<Fragment> getTokens() {
        return tokens;
    }

    /**
     * segments sections with embedded notes into non-overlapping (subsections).
     * Several fragments may have the same identifier, but different char-positions as a result.
     * @return
     */
    public List<Fragment> getNonOverlappingSections() {
        return Fragment.flatten(sections);
    }
}
