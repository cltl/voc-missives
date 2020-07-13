package entities.rawTextAligner;

import utils.common.Fragment;
import utils.common.Span;

import java.util.LinkedList;
import java.util.List;

/**
 * compacts text (removing whitespace, section selection) and maps its character
 * offsets to the input text
 * Section selection: inputs flat fragments
 * White space removal: subfragments corresponding to text; (id, 0, 10) -> (id, 0, 4) (id, 5, 10)
 */
public class OffsetMapper {
    CompactText indexMapper;
    String inputText;

    private OffsetMapper(String inputText, CompactText ct) {
        this.indexMapper = ct;
        this.inputText = inputText;
    }

    public static OffsetMapper create(List<Fragment> flatFragments, String inputText) {
        List<Span> whitespaceFreeSpans = subFragment(flatFragments, inputText);
        CompactText ct = CompactText.create(inputText, whitespaceFreeSpans);
        return new OffsetMapper(inputText, ct);
    }

    public static List<Span> subFragment(List<Fragment> flatFragments, String text) {
        List<Span> spans = new LinkedList<>();
        for (Fragment f: flatFragments) {
            String fText = text.substring(f.getOffset(), f.getEndIndex());
            int i = 0;
            for (String p: fText.trim().split("\\s+")) {
                i = fText.indexOf(p, i);
                spans.add(new Span(f.getOffset() + i, f.getOffset() + i + p.length() - 1));
            }
        }
        return spans;
    }

    /**
     * returns list of matched spans in input text
     * @param text
     */
    public List<Span> coarseMatches(String text) {
        int searchIndex = 0;
        List<Span> matches = new LinkedList<>();
        while (true) {
            int i = indexMapper.getCompactText().indexOf(text.replaceAll("\\s+", ""), searchIndex);
            if (i == -1)
                break;
            matches.add(indexMapper.getRawCharOffset(new Span(i, i + text.length() - 1)));
            searchIndex = i + 1;
        }
        return matches;
    }
}
