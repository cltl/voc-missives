package naf2naf;

import utils.common.Span;
import xjc.naf.Tunit;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class TunitTree {
    Tunit node;
    List<TunitTree> children;

    private TunitTree(Tunit t) {
        node = t;
        children = new LinkedList<>();
    }

    public static TunitTree create(List<Tunit> ts) {
        ArrayDeque<TunitTree> thread = new ArrayDeque<>();
        for (Tunit t: ts) {
            if (thread.isEmpty()) {
                thread.push(new TunitTree(t));
            } else {
                while (! thread.peek().dominates(t))
                    thread.pop();
                TunitTree child = new TunitTree(t);
                thread.peek().add(child);
                thread.push(child);
            }
        }
        while (thread.size() > 1)
            thread.pop();
        return thread.pop();
    }

    public Tunit getNode() {
        return node;
    }

    private void add(TunitTree child) {
        children.add(child);
    }

    public boolean dominates(Tunit t) {
        Span s = Span.fromCharPosition(node.getOffset(), node.getLength());
        Span o = Span.fromCharPosition(t.getOffset(), t.getLength());
        return s.contains(o);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(node.getId());
        if (! children.isEmpty()) {
            sb.append(" ");
            for (TunitTree c: children) {
                sb.append(c.toString()).append(" ");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * extracts the top nodes satisfying the filter
     * @param filter
     * @return
     */
    public List<TunitTree> filterTopNodes(Predicate<Tunit> filter) {
        List<TunitTree> selected = new LinkedList<>();
        filterTopNodes(filter, selected);
        return selected;
    }

    private void filterTopNodes(Predicate<Tunit> filter, List<TunitTree> list) {
        if (filter.test(node))
            list.add(this);
        else for (TunitTree c: children) {
            c.filterTopNodes(filter, list);
        }
    }

    public List<Tunit> extractTunits(Predicate<TunitTree> exclude) {
        List<Tunit> tunits = new LinkedList<>();
        loopExtraction(exclude, tunits);
        return tunits;
    }

    private void loopExtraction(Predicate<TunitTree> exclusionFilter, List<Tunit> tunits) {
        if (children.isEmpty()) {
            tunits.add(extendId(node));
        } else {
            if (nodeAndChildrenSpansMatch() && children.stream().noneMatch(exclusionFilter))
                children.forEach(c -> c.loopExtraction(exclusionFilter, tunits));
            else {
                List<TunitTree> subtrees = splitNode(exclusionFilter);
                subtrees.forEach(t -> t.loopExtraction(exclusionFilter, tunits));
            }
        }
    }

    private Tunit extendId(Tunit tunit) {
        if (tunit.getId().indexOf(".co") == -1)
            tunit.setId(tunit.getId() + ".co" + tunit.getOffset() + "-" + tunit.getLength());
        return tunit;
    }

    private int getFirstIndex() {
        return Integer.parseInt(node.getOffset());
    }

    private int getLastIndex() {
        return getFirstIndex() + getLength() - 1;
    }

    private int getLength() {
        return Integer.parseInt(node.getLength());
    }

    private List<TunitTree> splitNode(Predicate<TunitTree> exclusionFilter) {
        int i = getFirstIndex();
        List<TunitTree> partTrees = new LinkedList<>();
        for (TunitTree c : children) {
            if (c.getFirstIndex() > i)
                extractPartTree(i, c.getFirstIndex() - 1, partTrees);
            i = c.getLastIndex() + 1;
            if (! exclusionFilter.test(c))
                partTrees.add(c);
        }
        if (i <= getLastIndex()) {
            extractPartTree(i, getLastIndex(), partTrees);
        }
        return partTrees;
    }

    private void extractPartTree(int i, int j, List<TunitTree> partTrees) {
        Span partSpan = new Span(i, j);
        TunitTree partTree = new TunitTree(deriveTunit(partSpan));
        partTrees.add(partTree);
    }

    private Tunit deriveTunit(Span span) {
        Tunit splitTunit = new Tunit();
        splitTunit.setXpath(node.getXpath());
        splitTunit.setType(node.getType());
        splitTunit.setId(node.getId() + ".co" + span.getFirstIndex() + "-" + span.getLength());
        splitTunit.setOffset(span.getFirstIndex() + "");
        splitTunit.setLength(span.getLength() + "");
        return splitTunit;
    }

    public List<TunitTree> getChildren() {
        return children;
    }

    private boolean nodeAndChildrenSpansMatch() {
        int childrenSpans = 0;
        for (TunitTree c: children)
            childrenSpans += c.getLength();
        return childrenSpans == getLength();
    }

}
