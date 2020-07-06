package utils.tei;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static utils.tei.ATeiTree.TeiType.*;

public abstract class ATeiTree {

    public enum TeiType {TEI, STR, TEXT, DIV, P, FW, LB, HEAD, NOTE, BODY, HI, TABLE, ROW, CELL, PB}

    TeiType teiType;
    String id;

    protected ATeiTree(TeiType teiType, String id) {
        this.teiType = teiType;
        this.id = id;
    }

    public TeiType getTeiType() {
        return teiType;
    }

    public String getId() {
        return id;
    }

    public abstract List<ATeiTree> getTopNodes(Predicate<ATeiTree> p);

    public abstract ATeiTree remove(Predicate<ATeiTree> p);

    public abstract void accept(Consumer<ATeiTree> consumer);

    public abstract List<ATeiTree> getAllNodes(Predicate<ATeiTree> p);

    public abstract String yield();

    public boolean isParagraph() {
        return teiType == P;
    }

    public boolean isNote() {
        return teiType == NOTE;
    }

    public boolean isForeword() {
        return teiType == FW;
    }

    public boolean isHead() {
        return teiType == HEAD;
    }

    public boolean isTable() { return teiType == TABLE; }

}