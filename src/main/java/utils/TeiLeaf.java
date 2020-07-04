package utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TeiLeaf extends ATeiTree {
    String content;

    protected TeiLeaf(TeiType teiType, String id, String content) {
        super(teiType, id);
        this.content = content;
    }

    public static TeiLeaf create(TeiType teiType, String id, String content) {
        return new TeiLeaf(teiType, id, content);
    }

    public String getContent() {
        return content;
    }

    @Override
    public List<ATeiTree> getTopNodes(Predicate<ATeiTree> p) {
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
    public List<ATeiTree> getAllNodes(Predicate<ATeiTree> p) {
        if (p.test(this))
            return Collections.singletonList(this);
        return Collections.EMPTY_LIST;
    }

    @Override
    public String yield() {
        return content;
    }
}
