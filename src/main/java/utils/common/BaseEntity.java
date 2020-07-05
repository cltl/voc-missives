package utils.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        Span tSpan = new Span(Integer.parseInt(tokenIds.get(0).substring(1)),
                Integer.parseInt(tokenIds.get(tokenIds.size() - 1).substring(1)));
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

    public static List<BaseEntity> overlap(List<BaseEntity> entities) {
        int i = 0;
        Set<BaseEntity> overlapping = new HashSet<>();

        for (int j = 1; j < entities.size(); j++) {
            if (entities.get(j).getFirstTokenIndex() <= entities.get(i).getLastTokenIndex()) {
                overlapping.add(entities.get(i));
                overlapping.add(entities.get(j));
            }
            if (entities.get(j).getLastTokenIndex() > entities.get(i).getLastTokenIndex())
                i = j;
        }
        List<BaseEntity> result = new LinkedList<>(overlapping);
        Collections.sort(result);
        return result;
    }

    public BaseEntity withId(int i) {
        return new BaseEntity(type, "e" + i, tokenSpan);
    }

    @Override
    public int compareTo(BaseEntity o) {
        return tokenSpan.compareTo(o.getTokenSpan());
    }
}
