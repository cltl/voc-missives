package utils.common;

import java.util.*;

public class Fragment implements Comparable<Fragment> {
    String id;
    int offset;
    int length;

    public Fragment(String id, int offset, int length) {
        this.id = id;
        this.offset = offset;
        this.length = length;
    }

    public Fragment(int id, int offset, int length) {
        this.id = "" + id;
        this.offset = offset;
        this.length = length;
    }

    public Fragment(String id, String offset, String length) {
        this.id = id;
        this.offset = Integer.parseInt(offset);
        this.length = Integer.parseInt(length);
    }

    public String toString() {
        return "id: " + id + "; offset: " + offset + "; length: " + length;
    }

    public static void sort(List<Fragment> sections) {
        Collections.sort(sections, Fragment::compareTo);
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getId() {
        return id;
    }

    public int getEndIndex() {
        return offset + length;
    }

    public Span getSpan() { return new Span(offset, offset + length - 1); }

    public static List<Fragment> zip(List<String> ids, List<CharPosition> positions) {
        if (ids.size() != positions.size())
            throw new IllegalArgumentException("cannot zip lists of different sizes");
        List<Fragment> fragments = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++)
            fragments.add(new Fragment(ids.get(i), positions.get(i).getOffset(), positions.get(i).getLength()));
        return fragments;
    }

    public static List<Fragment> addAllWithIndexIdentifiers(List<Fragment> fragments, List<CharPosition> positions) {
        int startIndex = fragments.size();
        for (int i = 0; i < positions.size(); i++)
            fragments.add(new Fragment(startIndex + i, positions.get(i).getOffset(), positions.get(i).getLength()));
        return fragments;
    }

    @Override
    public int compareTo(Fragment o) {
        // prefix order
        if (offset < o.getOffset() || offset == o.getOffset() && length >= o.getLength())
            return -1;
        else
            return 1;
    }

    public static List<Fragment> flatten2(List<Fragment> fragments) {
        List<Fragment> fs = new LinkedList<>();
        if (fragments.size() < 2)
            return fs;
        int offset;
        LinkedList<Fragment> queue = new LinkedList<>();
        queue.add(fragments.get(0));
        for (Fragment f: fragments) {
            if (f.getLength() > 0) {
                if (queue.peekLast().getOffset() == f.getOffset() && queue.peekLast().getLength() == f.getLength()) {
                    queue.removeLast();
                    queue.add(f);
                } else if (queue.peekLast().contains(f)) {
                    offset = queue.peekLast().getOffset();
                    if (offset < f.getOffset())
                        fs.add(new Fragment(queue.peekLast().getId(), offset, f.getOffset() - offset));
                    queue.add(f);
                } else {
                    fs.add(queue.peekLast());
                    offset = queue.removeLast().getEndIndex();
                    if (offset < f.getOffset())
                        fs.add(new Fragment(queue.peekLast().getId(), offset, f.getOffset() - offset));
                    queue.add(f);
                }
            }
        }
        if (queue.size() > 1)
            fs.add(queue.peekLast());

        while (queue.size() > 1) {
            offset = queue.removeLast().getEndIndex();
            if (offset < queue.peekLast().getEndIndex()) {
                fs.add(new Fragment(queue.peekLast().getId(), offset, queue.peekLast().getEndIndex() - offset));
            }
        }
        return fs;
    }
    /**
     * flattens embedding fragments into non-overlapping subfragments
     * @param fragments
     * @return
     */
    public static List<Fragment> flatten(List<Fragment> fragments) {
        List<Fragment> result = new ArrayList<>();
        int i = 0;
        while (i < fragments.size() - 1 ) {
            // fragments disjoint from next fragment
            if (fragments.get(i).getEndIndex() < fragments.get(i + 1).getEndIndex()) {
                result.add(fragments.get(i));
                i++;
            } else {
                Fragment top = fragments.get(i);
                int k = i + 1;
                // list embedded fragments
                List<Fragment> embedded = new ArrayList<>();
                while (k < fragments.size() && top.getEndIndex() > fragments.get(k).getEndIndex()) {
                    embedded.add(fragments.get(k));
                    k++;
                }
                // split top fragment by embedded fragments
                List<Fragment> flat = new ArrayList<>();
                int offset = top.getOffset();
                for (Fragment f: embedded) {
                    if (offset < f.getOffset())
                        flat.add(new Fragment(top.getId(), offset, f.getOffset() - offset));
                    flat.add(f);
                    offset = f.getEndIndex();
                }
                if (offset < top.getEndIndex() - 1)
                    flat.add(new Fragment(top.getId(), offset, top.getEndIndex() - offset));
                result.addAll(flat);
                i = k;
            }
        }
        if (i < fragments.size())
            result.add(fragments.get(i));
        return result;
    }

    public boolean precedes(Fragment f) {
        return getEndIndex() <= f.getOffset();
    }

    public boolean contains(Fragment f) {
        return offset <= f.getOffset() && getEndIndex() >= f.getEndIndex();
    }
}
