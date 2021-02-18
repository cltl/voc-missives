package utils.naf;

import utils.common.Span;
import xjc.naf.Entity;

public class BaseEntity implements Comparable<BaseEntity> {
    Span indexSpan;
    String type;

    public BaseEntity(Span indexSpan, String type) {
        this.indexSpan = indexSpan;
        this.type = type;
    }

    public static BaseEntity create(Entity e) {
        return new BaseEntity(NafUnits.indexSpan(e), e.getType());
    }

    public Span getIndexSpan() {
        return indexSpan;
    }

    public String getType() {
        return type;
    }

    public int begin() {
        return indexSpan.getFirstIndex();
    }

    public int end() {
        return indexSpan.getEnd();
    }

    @Override
    public int compareTo(BaseEntity o) {
        return indexSpan.compareTo(o.getIndexSpan());
    }
}
