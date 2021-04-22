package conllIn2naf;

import utils.common.AbnormalProcessException;
import utils.common.Span;
import utils.naf.NafUnits;
import utils.naf.Wf;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class WfAligner {

    class WfAlignment {
        List<Wf> wfs;
        List<String> tokens;

        public WfAlignment(List<Wf> alignedWfs, List<String> alignedTokens) {
            this.wfs = alignedWfs;
            this.tokens = alignedTokens;
        }

        void checkWfIndices() throws AbnormalProcessException {
            int expectedOffset = Integer.parseInt(wfs.get(0).getOffset());
            for (Wf wf: wfs) {
                if (Integer.parseInt(wf.getOffset()) != expectedOffset)
                    throw new AbnormalProcessException("Expected consecutive wfs, found "
                            + wfs.stream().map(w -> w.toString()).collect(Collectors.joining(" ")));
                expectedOffset += Integer.parseInt(wf.getLength());
            }
        }

        List<Wf> alignedWfs() {
            List<Wf> aligned = new LinkedList<>();
            int offset = Integer.parseInt(wfs.get(0).getOffset());
            int id = 0;
            int sentId = Integer.parseInt(wfs.get(0).getSent());
            int paraId = Integer.parseInt(wfs.get(0).getPara());
            for (String t: tokens)
                aligned.add(NafUnits.createWf(t, id, offset, sentId, paraId));
            return aligned;
        }

        int tokenCount() {
            return tokens.size();
        }
        int wfCount() {
            return wfs.size();
        }

        List<List<Integer>> alignedPerToken() {
            List<Span> wfSpans = new LinkedList<>();
            int wfOff = 0;
            for (Wf wf: wfs) {
                wfSpans.add(new Span(wfOff, wfOff + wf.getContent().length() - 1));
                wfOff += wf.getOffset().length();
            }
            List<List<Integer>> idmap = new LinkedList<>();
            int offset = 0;
            for (String t: tokens) {
                Span s = new Span(offset, t.length() - 1);
                idmap.add(wfSpans.stream().filter(span -> span.overlaps(s)).map(span -> wfSpans.indexOf(span)).collect(Collectors.toList()));
                offset += t.length();
            }
            return idmap;
        }
    }
    List<WfAlignment> alignments;

    public WfAligner(List<Wf> wfs, List<String> tokens) throws AbnormalProcessException {
        this.alignments = align(wfs, tokens);
    }

    public List<WfAlignment> getAlignments() {
        return alignments;
    }

    /**
     * maps every token (through its list index) to the indices of its aligned wfs
     * @return
     */
    public List<List<Integer>> indexMap() {
        List<List<Integer>> map = new LinkedList<>();
        int wfId = 0;
        for (WfAlignment a: alignments) {
            for (List<Integer> tokenWfs: a.alignedPerToken()) {
                int finalWfId = wfId;
                map.add(tokenWfs.stream().map(i -> i + finalWfId).collect(Collectors.toList()));
            }
            wfId += a.wfCount();
        }
        return map;
    }

    protected List<Wf> getAlignedWfs() {
        List<Wf> aligned = alignments.stream().map(a -> a.alignedWfs()).flatMap(x -> x.stream()).collect(Collectors.toList());
        for (int i = 0; i < aligned.size(); i++)
            aligned.get(i).setId("w" + i);
        return aligned;
    }

    private List<WfAlignment> align(List<Wf> wfs, List<String> tokens) throws AbnormalProcessException {
        List<WfAlignment> alignments = new LinkedList<>();
        int i = 0;
        int j = 0;
        List<String> alignedTokens = new LinkedList<>();
        List<Wf> alignedWfs = new LinkedList<>();
        String tokStr = tokens.get(j);
        String wfStr = wfs.get(i).getContent();
        while (i < wfs.size()) {
            if (wfStr.equals(tokStr)) {
                alignedTokens.add(tokens.get(j));
                alignedWfs.add(wfs.get(i));
                WfAlignment a = new WfAlignment(alignedWfs, alignedTokens);
                a.checkWfIndices();
                alignments.add(a);
                i++;
                j++;
                if (i < wfs.size() && j < tokens.size()) {
                    wfStr = wfs.get(i).getContent();
                    tokStr = tokens.get(j);
                    alignedTokens = new LinkedList<>();
                    alignedWfs = new LinkedList<>();
                }
            } else if (wfStr.startsWith(tokStr)) {
                alignedTokens.add(tokens.get(j));
                j++;
                tokStr += tokens.get(j);
            } else if (tokStr.startsWith(wfStr)) {
                alignedWfs.add(wfs.get(i));
                i++;
                wfStr += wfs.get(i).getContent();
            } else
                throw new AbnormalProcessException("cannot align tokens \"" + wfStr + "\" and \"" + tokStr );

        }
        return alignments;
    }
}
