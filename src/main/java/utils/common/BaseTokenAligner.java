package utils.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BaseTokenAligner {
    HashMap<Integer, Integer> firstIndexRef;
    HashMap<Integer, Integer> lastIndexRef;
    List<BaseToken> inputTokens;
    List<BaseToken> refTokens;
    List<Integer> firstIndices;
    List<Integer> lastIndices;
    int maxTokenLookAhead;
    int iInput;
    int iRef;

    public BaseTokenAligner(int maxTokenLookAhead, List<BaseToken> inputTokens, List<BaseToken> refTokens) {
        this.maxTokenLookAhead = maxTokenLookAhead;
        this.firstIndexRef = new HashMap<>();
        this.lastIndexRef = new HashMap<>();
        this.inputTokens = inputTokens;
        this.refTokens = refTokens;
        this.iInput = 0;
        this.iRef = 0;
        this.firstIndices = new ArrayList<>();
        this.lastIndices = new ArrayList<>();
    }

    public static BaseTokenAligner create(List<BaseToken> extTokens, List<BaseToken> refTokens, int maxTokenLookAhead) {
        BaseTokenAligner aligner = new BaseTokenAligner(maxTokenLookAhead, extTokens, refTokens);
        return aligner;
    }

    public List<BaseToken> getInputTokens() {
        return inputTokens;
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
        while (iInput < inputTokens.size()) {
            BaseToken ref = refTokens.get(iRef);
            BaseToken ext = inputTokens.get(iInput);
            if (ext.getText().equals("Ceylon"))
                System.out.println("FIXME");
            int startE = iInput;
            int startR = iRef;
            boolean aligned = extTokenMatchesOneOrManyRef(ext, ref);
            aligned = aligned || extTokensMatchSingleRef(ext, ref);
            aligned = aligned || extTokenMatchesHyphenatedRef(ext, ref);
            aligned = aligned || manyToManyAlignment(ext, ref);
            aligned = aligned || lookAhead(ext, ref);


            StringBuilder sb = new StringBuilder();
            for (int i = startE; i < iInput; i++)
                sb.append(inputTokens.get(i).getText()).append(" ");
            sb.append("-> ");
            for (int i = startR; i < iRef; i++)
                sb.append(refTokens.get(i).getText()).append(" ");
            sb.append(" (").append(refTokens.get(iRef - 1)).append(")");
            String m = sb.toString();
            System.out.println(m);

            if (! aligned) {
                StringBuilder message = new StringBuilder();
                message.append("Unable to match tokens (text @ char-offset) :\nExt: ")
                        .append(inputTokens.get(iInput).getText())
                        .append(" @ ").append(inputTokens.get(iInput).getFirstIndex())
                        .append("; (last matched token): ").append(iInput > 0 ? inputTokens.get(iInput - 1).getText(): "none")
                        .append(" @ ").append(inputTokens.get(iInput - 1).getLastIndex())
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

    /**
     * Look ahead. We want to decide in which direction to look first.
     *
     * If input tokens are missing from the reference,
     * a following set of input tokens will occur more frequently in a set
     * of following reference tokens; but if reference tokens are missing,
     * then the first set of input tokens will match more frequently.
     *
     * example.            Ref: a b c d e f
     * missing tokens in input: c d e f g h  -> part 1 (c d e) has 3 matches; part 2 (f g h) has 1.
     * missing tokens in ref  : y z a b c d  -> (y z a) has 1 match, (b c d) has 2.
     *
     * @param input
     * @param ref
     * @return
     */
    private boolean lookAhead(BaseToken input, BaseToken ref) {
        List<String> nextRefTokens = refTokens.subList(iRef, iRef + 100).stream().map(BaseToken::getText).collect(Collectors.toList());
        List<String> nextInputTokens = inputTokens.subList(iInput, iInput + 50).stream().map(BaseToken::getText).collect(Collectors.toList());
        List<String> followingInputTokens = inputTokens.subList(iInput + 50, iInput + 100).stream().map(BaseToken::getText).collect(Collectors.toList());
        long nbMatchesNextSet = nextInputTokens.stream().filter(t -> nextRefTokens.contains(t)).count();
        long nbMatchesFollowingSet = followingInputTokens.stream().filter(t -> nextRefTokens.contains(t)).count();
        if (nbMatchesNextSet > nbMatchesFollowingSet)
            return refTokensNotInInput(input) || inputTokensNotInRef(ref);
        else
            return inputTokensNotInRef(ref) || refTokensNotInInput(input);
    }

    private boolean manyToManyAlignment(BaseToken ext, BaseToken ref) {
        String extText = ext.getText();
        String refText = ref.getText();
        int kRef = 0;
        int kExt = 0;
        while (true) {
            if (extText.equals(refText)) {
                mapIndices(ext.getFirstIndex(), ref.getFirstIndex(), inputTokens.get(iInput + kExt).getLastIndex(), refTokens.get(iRef + kRef).getLastIndex());
                iRef += kRef + 1;
                iInput += kExt + 1;
                return true;
            } if (extText.startsWith(refText) && iRef + kRef < refTokens.size() - 1) {
                kRef++;
                extText = extText.substring(refText.length());
                refText = refTokens.get(iRef + kRef).getText();
            } else if (refText.startsWith(extText) && iInput + kExt < inputTokens.size() - 1) {
                kExt++;
                refText = refText.substring(extText.length());
                extText = inputTokens.get(iInput + kExt).getText();
            } else
                return false;
        }
    }

    private boolean extTokenMatchesHyphenatedRef(BaseToken ext, BaseToken ref) {
        if (matchesHyphenatedRef(ext, ref)) {
            mapIndices(ext, ref);
            iRef++;
            iInput++;
            return true;
        }
        return false;
    }

    private boolean extTokenMatchesOneOrManyRef(BaseToken ext, BaseToken ref) {
        String refText = ref.getText();
        int k = 0;
        while (ext.getText().startsWith(refText)) {
            if (ext.getText().equals(refText)) {
                mapIndices(ext, ref.getFirstIndex(), refTokens.get(iRef + k).getLastIndex());
                iRef += k + 1;
                iInput++;
                return true;
            }
            k++;
            refText += refTokens.get(iRef + k);
        }
        return false;
    }

    private boolean extTokensMatchSingleRef(BaseToken ext, BaseToken ref) {
        int next = lastCovered(ref, inputTokens, iInput);
        if (next != -1) {
            mapIndices(ext.getFirstIndex(), ref.getFirstIndex(), inputTokens.get(next).getLastIndex(), ref.getLastIndex());
            iRef++;
            iInput = next + 1;
            return true;
        }
        return false;
    }

    private boolean inputTokensNotInRef(BaseToken ref) {
        int next = findToken(ref, inputTokens, iInput, maxTokenLookAhead);
        if (next != -1) {
            mapIndices(inputTokens.get(next), ref);
            iRef++;
            iInput = next + 1;
            return true;
        }
        return false;
    }

    private boolean refTokensNotInInput(BaseToken ext) {
        int next = findToken(ext, refTokens, iRef, maxTokenLookAhead);
        if (next != -1) {
            mapIndices(ext, refTokens.get(next));
            iRef = next + 1;
            iInput++;
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
