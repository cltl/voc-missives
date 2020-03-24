package utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TeiBreak extends ATeiTree {
    String number;

    protected TeiBreak(TeiType teiType, String id, String pageNumber) {
        super(teiType, id);
        this.number = pageNumber;
    }

    static public TeiBreak createPageBreak(String id, String pageNumber) {
        return new TeiBreak(TeiType.PB, id, pageNumber);
    }

    static public TeiBreak createLineBreak() {
        return new TeiBreak(TeiType.LB, null, null);
    }

    @Override
    public List<ATeiTree> filterHighest(Predicate<ATeiTree> p) {
        List<ATeiTree> list = new LinkedList<>();
        if (p.test(this))
            list.add(this);
        return list;
    }

    @Override
    public ATeiTree remove(Predicate<ATeiTree> p) {
        if (p.test(this))
            return null;
        else
            return this;
    }

    @Override
    public void accept(Consumer<ATeiTree> consumer) {
        consumer.accept(this);
    }

    @Override
    public List<ATeiTree> collect(Predicate<ATeiTree> p) {
        if (p.test(this))
            return Collections.singletonList(this);
        return Collections.EMPTY_LIST;
    }
}
