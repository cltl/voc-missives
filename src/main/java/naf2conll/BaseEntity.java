package naf2conll;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tei2naf.CharPosition;
import tei2naf.NafConverter;
import xjc.naf.Entity;
import xjc.naf.References;
import xjc.naf.Span;
import xjc.naf.Target;

import java.util.*;
import java.util.stream.Collectors;

/**
 * wrapper class for NAF Entity
 */
public class BaseEntity implements Comparable<BaseEntity> {

    String type;
    String id;
    /**
     * entity target span is converted to offset and length
     * w2 w3 w4 -> offset: 2; length: 3
     */
    CharPosition position;
    public static final Logger logger = LogManager.getLogger(BaseEntity.class);
    private BaseEntity(String type, String id, CharPosition position) {
        this.type = type;
        this.id = id;
        this.position = position;
    }

    public static BaseEntity create(Entity nafEntity) {
        return new BaseEntity(nafEntity.getType(), nafEntity.getId(), createPosition(nafEntity));
    }


    public static BaseEntity create(String type, String id, List<String> idSpan) {
        return new BaseEntity(type, id, createPosition(idSpan));
    }

    private static CharPosition createPosition(List<String> targets) {
        return new CharPosition(Integer.parseInt(targets.get(0).substring(1)), targets.size());
    }

    private static CharPosition createPosition(Entity nafEntity) {
        List<Span> spans = nafEntity.getReferencesAndExternalReferences().stream()
                .filter(x -> x instanceof References)
                .map(x -> ((References) x).getSpen())
                .findFirst().orElse(Collections.EMPTY_LIST);
        List<String> targets = spans.get(0).getTargets().stream().map(x -> (String) x.getId()).collect(Collectors.toList());
        return createPosition(targets);
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public CharPosition getPosition() {
        return position;
    }

    public String toString() {
        return id + " " + type + " (w" + position.getOffset() + "-w" + (position.getEndIndex() - 1) + ")";
    }

    public static List<Entity> sortAndRenameForNaf(List<BaseEntity> entities) {
        Collections.sort(entities, BaseEntity::compareTo);
        List<BaseEntity> overlapping = overlap(entities);
        if (! overlapping.isEmpty())
            logger.warn("Found overlapping entities: "
                    + overlapping.stream().map(BaseEntity::toString).collect(Collectors.joining(", ")));
        List<Entity> nafEntities = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++)
            nafEntities.add(entities.get(i).withId(i).asNaf());
        return nafEntities;
    }

    private static List<BaseEntity> overlap(List<BaseEntity> entities) {
        int i = 0;
        Set<BaseEntity> overlapping = new HashSet<>();

        for (int j = 1; j < entities.size(); j++) {
            if (entities.get(j).getPosition().getOffset() < entities.get(i).getPosition().getEndIndex()) {
                overlapping.add(entities.get(i));
                overlapping.add(entities.get(j));
            }
            if (entities.get(j).getPosition().getEndIndex() > entities.get(i).getPosition().getEndIndex())
                i = j;
        }
        List<BaseEntity> result = new LinkedList<>(overlapping);
        Collections.sort(result);
        return result;
    }

    public Entity asNaf() {
        Entity e = new Entity();
        e.setId(id);
        e.setType(type);
        References r = new References();
        Span s = new Span();
        List<Target> ts = s.getTargets();
        for (int i = position.getOffset(); i < position.getOffset() + position.getLength(); i++) {
            Target t = new Target();
            t.setId("w" + i);
            ts.add(t);
        }
        r.getSpen().add(s);
        e.getReferencesAndExternalReferences().add(r);
        return e;
    }

    public BaseEntity withId(int i) {
        return new BaseEntity(type, "e" + i, position);
    }

    @Override
    public int compareTo(BaseEntity o) {
        return position.compareTo(o.getPosition());
    }
}
