package utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class ATeiTree {


    public enum TeiType {TEI, STR, TEXT, DIV, P, LB, HEAD, NOTE, BODY, HI, TABLE, ROW, CELL, PB}

    TeiType teiType;
    String id;

    protected ATeiTree(TeiType teiType, String id) {
        this.teiType = teiType;
        this.id = id;
    }

    public static ATeiTree create(TeiType teiType, String id, List<ATeiTree> children, String content) {
        if (! children.isEmpty())
            return new TeiTree(teiType, id, children, content);
        else
            return new TeiLeaf(teiType, id, content);
    }

    public TeiType getTeiType() {
        return teiType;
    }

    public String getId() {
        return id;
    }

    public abstract List<ATeiTree> filterHighest(Predicate<ATeiTree> p);

    public abstract ATeiTree remove(Predicate<ATeiTree> p);

    public abstract void accept(Consumer<ATeiTree> consumer);

    public abstract List<ATeiTree> collect(Predicate<ATeiTree> p);
}
