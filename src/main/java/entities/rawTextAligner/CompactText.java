package entities.rawTextAligner;

import utils.common.Span;

import java.util.*;

/**
 * removes selected parts of an input text, and provides a character offset
 * mapping function from the compact text to the original input text.
 */
public class CompactText {

    /**
     * the compact text can be subdivided in sections from the input text.
     * the section offsets map only the beginning of each section.
     */
    Map<Integer,Integer> sectionOffsets;
    /**
     * (ordered) list of sectionOffsets keys
     */
    List<Integer> keys;

    String compactText;

    private CompactText(String text, Map<Integer,Integer> sectionOffsets) {
        this.compactText = text;
        this.sectionOffsets = sectionOffsets;
        this.keys = new LinkedList<>(sectionOffsets.keySet());
        Collections.sort(keys);
    }

    public static CompactText create(String inputText, List<Span> sections) {
        Map<Integer,Integer> sectionOffsets = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        for (Span s: sections) {
            sb.append(inputText.substring(s.getFirstIndex(), s.getLastIndex() + 1));
            sectionOffsets.put(offset, s.getFirstIndex());
            offset += s.getLength();
        }
        return new CompactText(sb.toString(), sectionOffsets);
    }

    public String getCompactText() {
        return compactText;
    }

    public int getRawCharOffset(int i) {
        int sectionOffset = 0;
        for (int k: keys) {
            if (k <= i)
                sectionOffset = k;
            else
                break;
        }
        return sectionOffsets.get(sectionOffset) + i - sectionOffset;
    }

    public Span getRawCharOffset(Span s) {
        return new Span(getRawCharOffset(s.getFirstIndex()), getRawCharOffset(s.getLastIndex()));
    }
}
