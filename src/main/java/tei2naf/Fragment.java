package tei2naf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
