package utils.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xjc.naf.Entity;

import java.util.*;

/**
 * wrapper class for NAF Entity
 */
public class BaseEntity implements Comparable<BaseEntity> {

    String type;
    String id;
    /**
     * span over target tokens
     * w2 w3 w4 -> firstIndex: 2; lastIndex: 4
     */
    Span tokenSpan;
    public static final Logger logger = LogManager.getLogger(BaseEntity.class);

    public BaseEntity(String type, String id, Span tokenSpan) {
        this.type = type;
        this.id = id;
        this.tokenSpan = tokenSpan;
    }

    /**
     *
     * @param type
     * @param id
     * @param tokenIds  list of token ids, e.g. (w3, w4, w5)
     * @return
     */
    public static BaseEntity create(String type, String id, List<String> tokenIds) {
        int index = 1;
        while (! Character.isDigit(tokenIds.get(0).charAt(index)))
            index++;
        Span tSpan = new Span(Integer.parseInt(tokenIds.get(0).substring(index)),
                Integer.parseInt(tokenIds.get(tokenIds.size() - 1).substring(index)));
        return new BaseEntity(type, id, tSpan);
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Span getTokenSpan() {
        return tokenSpan;
    }

    public String toString() {
        return id + " " + type + " tokens: " + tokenSpan.toString();
    }

    public int getFirstTokenIndex() {
        return tokenSpan.getFirstIndex();
    }

    public int getLastTokenIndex() {
        return tokenSpan.getLastIndex();
    }

    public static List<BaseEntity> overlap(List<BaseEntity> entities) throws AbnormalProcessException {
        int i = 0;
        Set<BaseEntity> overlapping = new HashSet<>();
        int j = 1;
        while (j < entities.size()) {
            if (entities.get(j).getFirstTokenIndex() <= entities.get(i).getLastTokenIndex()) {
                overlapping.add(entities.get(i));
                try {
                    overlapping.add(entities.get(j));
                } catch (NullPointerException u) {
                    System.out.println("FIXME");
                    throw new AbnormalProcessException(u);
                }
                if (entities.get(j).getLastTokenIndex() > entities.get(i).getLastTokenIndex())
                    i = j;
            } else
                i++;
            j++;
        }
        List<BaseEntity> result = new LinkedList<>(overlapping);
        Collections.sort(result);
        return result;
    }

    public BaseEntity withId(int i) {
        return new BaseEntity(type, "e" + i, tokenSpan);
    }

    public BaseEntity withSpan(List<String> termSpan) {
        return BaseEntity.create(type, id, termSpan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id, getFirstTokenIndex(), getLastTokenIndex());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        BaseEntity x = (BaseEntity) o;
        return (type.equals(x.getType())
                && id.equals(x.getId())
                && getLastTokenIndex() == x.getLastTokenIndex()
                && getFirstTokenIndex() == x.getFirstTokenIndex());
    }

    @Override
    public int compareTo(BaseEntity o) {
        return tokenSpan.compareTo(o.getTokenSpan());
    }

}
