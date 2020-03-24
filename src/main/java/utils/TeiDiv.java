package utils;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TeiDiv extends ATeiTree {
    List<ATeiTree> children;

    protected TeiDiv(TeiType teiType, String id, List<ATeiTree> children) {
        super(teiType, id);
        this.children = children;
    }

    public static ATeiTree create(TeiType tei, String id, List<ATeiTree> children) {
        return new TeiDiv(tei, id, children);
    }

    public List<ATeiTree> getChildren() {
        return children;
    }

    @Override
    public List<ATeiTree> filterHighest(Predicate<ATeiTree> p) {
        List<ATeiTree> list = new LinkedList<>();
        if (p.test(this))
            list.add(this);
        else
            children.forEach(c -> list.addAll(c.filterHighest(p)));
        return list;
    }

    @Override
    public ATeiTree remove(Predicate<ATeiTree> p) {
        if (p.test(this))
            return null;
        children = children.stream().map(c -> c.remove(p)).filter(c -> c != null).collect(Collectors.toList());
        if (children.isEmpty())
            return null;
        return this;
    }

    @Override
    public void accept(Consumer<ATeiTree> consumer) {
        consumer.accept(this);
        children.forEach(c -> c.accept(consumer));
    }

    @Override
    public List<ATeiTree> collect(Predicate<ATeiTree> p) {
        List<ATeiTree> list = new LinkedList<>();
        if (p.test(this))
            list.add(this);
        list.addAll(children.stream().map(c -> c.collect(p)).flatMap(x -> x.stream()).collect(Collectors.toList()));
        return list;
    }
}
