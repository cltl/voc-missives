package entities.nafReference;

import utils.common.BaseToken;
import utils.common.Span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ConllTokenAligner {
    HashMap<Integer, Integer> firstIndexRef;
    HashMap<Integer, Integer> lastIndexRef;
    List<BaseToken> extTokens;
    List<BaseToken> refTokens;
    List<Integer> firstIndices;
    List<Integer> lastIndices;
    int maxTokenLookAhead;
    int iExt;
    int iRef;

    public ConllTokenAligner(int maxTokenLookAhead, List<BaseToken> extTokens, List<BaseToken> refTokens) {
        this.maxTokenLookAhead = maxTokenLookAhead;
        this.firstIndexRef = new HashMap<>();
        this.lastIndexRef = new HashMap<>();
        this.extTokens = extTokens;
        this.refTokens = refTokens;
        this.iExt = 0;
        this.iRef = 0;
        this.firstIndices = new ArrayList<>();
        this.lastIndices = new ArrayList<>();
    }

    public static ConllTokenAligner create(List<BaseToken> extTokens, List<BaseToken> refTokens, int maxTokenLookAhead) {
        ConllTokenAligner aligner = new ConllTokenAligner(maxTokenLookAhead, extTokens, refTokens);
        return aligner;
    }

    public Span getReferenceSpan(int begin, int end) {
        return new Span(beginReference(begin), endReference(end));
    }

    private int beginReference(int index) {
        if (firstIndexRef.containsKey(index))
            return firstIndexRef.get(index);
        else {
            int nextToken = firstIndices.stream().filter(i -> i < index).max(Integer::compareTo).orElse(-1);
            if (nextToken >= 0)
                return firstIndexRef.get(nextToken);
        }
        return -1;
    }

    private int endReference(int index) {
        if (lastIndexRef.containsKey(index))
            return lastIndexRef.get(index);
        else {
            int nextToken = lastIndices.stream().filter(i -> i > index).min(Integer::compareTo).orElse(-1);
            if (nextToken != -1)
                return lastIndexRef.get(nextToken);
        }
        return -1;
    }

    /**
     * Maps firstIndex and lastIndex indices of external tokens to indices of matching reference tokens.
     * Matching is performed following a number of strategies, accounting for the following situations:
     *  - exact match;
     *  - external token is hyphenated in reference (matching one or two reference tokens);
     *  - several external tokens match a single reference (accounting for greedy tokenization);
     *  - many-to-many alignment between tokens (for instance "57r_v ." (ext) <=> "57r _ v." (ref));
     *  - reference tokens are missing from external file;
     *  - external tokens are missing from reference file.
     */
    public void align() throws IllegalArgumentException {
        while (iExt < extTokens.size()) {
            BaseToken ref = refTokens.get(iRef);
            BaseToken ext = extTokens.get(iExt);
            boolean aligned = extTokenMatchesOneOrManyRef(ext, ref)
                    || extTokensMatchSingleRef(ext, ref)
                    || extTokenMatchesHyphenatedRef(ext, ref)
                    || manyToManyAlignment(ext, ref)
                    || refTokensNotInExt(ext)
                    || extTokensNotInRef(ref);

            if (! aligned) {
                StringBuilder message = new StringBuilder();
                message.append("Unable to match tokens (text @ char-offset) :\nExt: ")
                        .append(extTokens.get(iExt).getText())
                        .append(" @ ").append(extTokens.get(iExt).getFirstIndex())
                        .append("; (last matched token): ").append(iExt > 0 ? extTokens.get(iExt - 1).getText(): "none")
                        .append(" @ ").append(extTokens.get(iExt - 1).getLastIndex())
                        .append("\nRef: ").append(refTokens.get(iRef).getText())
                        .append(" @ ").append(refTokens.get(iRef).getFirstIndex())
                        .append("; (last matched token): ").append(iRef > 0 ? refTokens.get(iRef - 1).getText(): "none")
                        .append(" @ ").append(refTokens.get(iRef - 1).getLastIndex());
                throw new IllegalArgumentException(message.toString());
            }
        }
        lastIndices.addAll(lastIndexRef.keySet());
        lastIndices.sort(Integer::compareTo);
        firstIndices.addAll(firstIndexRef.keySet());
        firstIndices.sort(Integer::compareTo);
    }

    private boolean manyToManyAlignment(BaseToken ext, BaseToken ref) {
        String extText = ext.getText();
        String refText = ref.getText();
        int kRef = 0;
        int kExt = 0;
        while (true) {
            if (extText.equals(refText)) {
                mapIndices(ext.getFirstIndex(), ref.getFirstIndex(), extTokens.get(iExt + kExt).getLastIndex(), refTokens.get(iRef + kRef).getLastIndex());
                iRef += kRef + 1;
                iExt += kExt + 1;
                return true;
            } if (extText.startsWith(refText) && iRef + kRef < refTokens.size() - 1) {
                kRef++;
                extText = extText.substring(refText.length());
                refText = refTokens.get(iRef + kRef).getText();
            } else if (refText.startsWith(extText) && iExt + kExt < extTokens.size() - 1) {
                kExt++;
                refText = refText.substring(extText.length());
                extText = extTokens.get(iExt + kExt).getText();
            } else
                return false;
        }
    }

    private boolean extTokenMatchesRef(BaseToken ext, BaseToken ref) {
        if (matchesText(ref, ext)) {
            mapIndices(ext, ref);
            iRef++;
            iExt++;
            return true;
        }
        return false;
    }

    private boolean extTokenMatchesHyphenatedRef(BaseToken ext, BaseToken ref) {
//        if (matchesSplitRef(ext, refTokens, iRef)) {
//            mapIndices(ext, ref.getFirstIndex(), refTokens.get(iRef + 1).getLastIndex());
//            iRef += 2;
//            iExt++;
//            return true;
//        } else
        if (matchesHyphenatedRef(ext, ref)) {
            mapIndices(ext, ref);
            iRef++;
            iExt++;
            return true;
        }
//        else if (matches3SplitRef(ext, refTokens, iRef)) {
//            mapIndices(ext, ref.getFirstIndex(), refTokens.get(iRef + 2).getLastIndex());
//            iRef += 3;
//            iExt++;
//            return true;
//        }
        return false;
    }

    private boolean extTokenMatchesOneOrManyRef(BaseToken ext, BaseToken ref) {
        String refText = ref.getText();
        int k = 0;
        while (ext.getText().startsWith(refText)) {
            if (ext.getText().equals(refText)) {
                mapIndices(ext, ref.getFirstIndex(), refTokens.get(iRef + k).getLastIndex());
                iRef += k + 1;
                iExt++;
                return true;
            }
            k++;
            refText += refTokens.get(iRef + k);
        }
        return false;
    }

    private boolean extTokensMatchSingleRef(BaseToken ext, BaseToken ref) {
        int next = lastCovered(ref, extTokens, iExt);
        if (next != -1) {
            mapIndices(ext.getFirstIndex(), ref.getFirstIndex(), extTokens.get(next).getLastIndex(), ref.getLastIndex());
            iRef++;
            iExt = next + 1;
            return true;
        }
        return false;
    }

    private boolean extTokensNotInRef(BaseToken ref) {
        int next = findToken(ref, extTokens, iExt, maxTokenLookAhead);
        if (next != -1) {
            mapIndices(extTokens.get(next), ref);
            iRef++;
            iExt = next + 1;
            return true;
        }
        return false;
    }

    private boolean refTokensNotInExt(BaseToken ext) {
        int next = findToken(ext, refTokens, iRef, maxTokenLookAhead);
        if (next != -1) {
            mapIndices(ext, refTokens.get(next));
            iRef = next + 1;
            iExt++;
            return true;
        }
        return false;
    }


    private int lastCovered(BaseToken ref, List<BaseToken> extTokens, int iExt) {
        String matched = "";
        String refText = ref.getText();
        int lookup = 0;
        while (iExt + lookup <= extTokens.size() - 1
                && refText.startsWith(matched + extTokens.get(iExt + lookup).getText())) {
            matched += extTokens.get(iExt + lookup).getText();
            lookup++;
        }
        if (refText.equals(matched))
            return iExt + lookup - 1;
        return -1;
    }

    private int findToken(BaseToken t, List<BaseToken> otherTokens, int iOther, int maxLookUp) {
        for (int i = iOther; i < iOther + maxLookUp; i++) {
            if (matchesText(t, otherTokens.get(i)))
                return i;
        }
        return -1;
    }

    private boolean matchesHyphenatedRef(BaseToken ext, BaseToken ref) {
        String refText = ref.getText();
        if (refText.indexOf('-') != -1) {
            String[] parts = refText.split("-");
            return parts.length == 2 && ext.getText().equals(parts[0] + parts[1]);
        }
        return false;
    }

    private boolean matchesSplitRef(BaseToken ext, List<BaseToken> refTokens, int iRef) {
        if (iRef == refTokens.size() - 1)
            return false;
        String refText = refTokens.get(iRef).getText();
        return ext.getText().startsWith(refText.substring(0, refText.length() - 1))
                && ext.getText().endsWith(refTokens.get(iRef + 1).getText());
    }

    private boolean matches3SplitRef(BaseToken ext, List<BaseToken> refTokens, int iRef) {
        if (iRef == refTokens.size() - 2)
            return false;
        String refText = refTokens.get(iRef).getText();
        return ext.getText().startsWith(refText.substring(0, refText.length() - 1))
                && ext.getText().endsWith(refTokens.get(iRef + 2).getText());
    }

    private boolean matchesText(BaseToken t1, BaseToken t2) {
        return t1.getText().equals(t2.getText());
    }

    private void mapIndices(int beginIndex, int beginTarget, int endIndex, int endTarget) {
        firstIndexRef.put(beginIndex, beginTarget);
        lastIndexRef.put(endIndex, endTarget);
    }

    private void mapIndices(BaseToken t, int beginTarget, int endTarget) {
        firstIndexRef.put(t.getFirstIndex(), beginTarget);
        lastIndexRef.put(t.getLastIndex(), endTarget);
    }

    private void mapIndices(BaseToken ext, BaseToken ref) {
        firstIndexRef.put(ext.getFirstIndex(), ref.getFirstIndex());
        lastIndexRef.put(ext.getLastIndex(), ref.getLastIndex());
    }

    public List<String> getReferenceTokenSpanIds(int firstIndex, int lastIndex) {
        return refTokens.stream().filter(t -> t.getFirstIndex() >= firstIndex && t.getLastIndex() <= lastIndex)
                .map(BaseToken::getId)
                .collect(Collectors.toList());
    }
}
