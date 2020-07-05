package naf2conll;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import utils.Span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BaseTokenAligner {
    HashMap<Integer, Integer> beginToRef;
    HashMap<Integer, Integer> endToRef;
    List<BaseToken> extTokens;
    List<BaseToken> refTokens;
    List<Integer> beginKeys;
    List<Integer> endKeys;
    int maxTokenLookAhead;
    int iExt;
    int iRef;

    public BaseTokenAligner(int maxTokenLookAhead, List<BaseToken> extTokens, List<BaseToken> refTokens) {
        this.maxTokenLookAhead = maxTokenLookAhead;
        this.beginToRef = new HashMap<>();
        this.endToRef = new HashMap<>();
        this.extTokens = extTokens;
        this.refTokens = refTokens;
        this.iExt = 0;
        this.iRef = 0;
        this.beginKeys = new ArrayList<>();
        this.endKeys = new ArrayList<>();
    }

    public static BaseTokenAligner create(List<BaseToken> extTokens, List<BaseToken> refTokens, int maxTokenLookAhead) {
        BaseTokenAligner aligner = new BaseTokenAligner(maxTokenLookAhead, extTokens, refTokens);
        return aligner;
    }

    public Span getReferenceSpan(int begin, int end) {
        return new Span(beginReference(begin), endReference(end));
    }

    private int beginReference(int index) {
        if (beginToRef.containsKey(index))
            return beginToRef.get(index);
        else {
            int nextToken = beginKeys.stream().filter(i -> i < index).max(Integer::compareTo).orElse(-1);
            if (nextToken >= 0)
                return beginToRef.get(nextToken);
        }
        return -1;
    }

    private int endReference(int index) {
        if (endToRef.containsKey(index))
            return endToRef.get(index);
        else {
            int nextToken = endKeys.stream().filter(i -> i > index).min(Integer::compareTo).orElse(-1);
            if (nextToken != -1)
                return endToRef.get(nextToken);
        }
        return -1;
    }

    /**
     * Maps begin and end indices of external tokens to indices of matching reference tokens.
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
            boolean aligned = extTokenMatchesRef(ext, ref)
                    || extTokensMatchSingleRef(ext, ref)
                    || extTokenMatchesHyphenatedRef(ext, ref)
                    || manyToManyAlignment(ext, ref)
                    || refTokensNotInExt(ext)
                    || extTokensNotInRef(ref);

            if (! aligned) {
                StringBuilder message = new StringBuilder();
                message.append("Unable to match tokens (text @ char-offset) :\nExt: ")
                        .append(extTokens.get(iExt).getText())
                        .append(" @ ").append(extTokens.get(iExt).getBegin())
                        .append("; (last matched token): ").append(extTokens.get(iExt - 1).getText())
                        .append(" @ ").append(extTokens.get(iExt - 1).getEnd())
                        .append("\nRef: ").append(refTokens.get(iRef).getText())
                        .append(" @ ").append(refTokens.get(iRef).getBegin())
                        .append("; (last matched token): ").append(refTokens.get(iRef - 1).getText())
                        .append(" @ ").append(refTokens.get(iRef - 1).getEnd());
                throw new IllegalArgumentException(message.toString());
            }
        }
        endKeys.addAll(endToRef.keySet());
        endKeys.sort(Integer::compareTo);
        beginKeys.addAll(beginToRef.keySet());
        beginKeys.sort(Integer::compareTo);
    }

    private boolean manyToManyAlignment(BaseToken ext, BaseToken ref) {
        String extText = ext.getText();
        String refText = ref.getText();
        int kRef = 0;
        int kExt = 0;
        while (true) {
            if (extText.equals(refText)) {
                mapIndices(ext.getBegin(), ref.getBegin(), extTokens.get(iExt + kExt).getEnd(), refTokens.get(iRef + kRef).getEnd());
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
        if (matchesSplitRef(ext, refTokens, iRef)) {
            mapIndices(ext, ref.getBegin(), refTokens.get(iRef + 1).getEnd());
            iRef += 2;
            iExt++;
            return true;
        } else if (matchesHyphenatedRef(ext, ref)) {
            mapIndices(ext, ref);
            iRef++;
            iExt++;
            return true;
        } else if (matches3SplitRef(ext, refTokens, iRef)) {
            mapIndices(ext, ref.getBegin(), refTokens.get(iRef + 2).getEnd());
            iRef += 3;
            iExt++;
            return true;
        }
        return false;
    }

    private boolean extTokensMatchSingleRef(BaseToken ext, BaseToken ref) {
        int next = lastCovered(ref, extTokens, iExt);
        if (next != -1) {
            mapIndices(ext.getBegin(), ref.getBegin(), extTokens.get(next).getEnd(), ref.getEnd());
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
        beginToRef.put(beginIndex, beginTarget);
        endToRef.put(endIndex, endTarget);
    }

    private void mapIndices(BaseToken t, int beginTarget, int endTarget) {
        beginToRef.put(t.getBegin(), beginTarget);
        endToRef.put(t.getEnd(), endTarget);
    }

    private void mapIndices(BaseToken ext, BaseToken ref) {
        beginToRef.put(ext.getBegin(), ref.getBegin());
        endToRef.put(ext.getEnd(), ref.getEnd());
    }

}
