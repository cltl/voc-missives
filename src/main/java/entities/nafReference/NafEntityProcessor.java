package entities.nafReference;

import utils.common.AbnormalProcessException;
import utils.common.BaseEntity;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import xjc.naf.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface NafEntityProcessor {



    List<BaseEntity> readEntities(String input) throws AbnormalProcessException;

    default void process(String input, String output) throws AbnormalProcessException {
        addEntities(getNaf(), readEntities(input));
        getNaf().write(output);
    }

    NafDoc getNaf();

    default List<Entity> sortAndRenameForNaf(List<BaseEntity> entities) {
        Collections.sort(entities, BaseEntity::compareTo);
        List<BaseEntity> overlapping = BaseEntity.overlap(entities);
        if (! overlapping.isEmpty())
            BaseEntity.logger.warn("Found overlapping entities: "
                    + overlapping.stream().map(BaseEntity::toString).collect(Collectors.joining(", ")));
        List<Entity> nafEntities = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++)
            nafEntities.add(NafUnits.asNafEntity(entities.get(i).withId(i)));
        return nafEntities;
    }

    default void addEntities(NafDoc naf, List<BaseEntity> entities) {
        List<BaseEntity> all = naf.getBaseEntities();
        all.addAll(entities);
        naf.setEntities(sortAndRenameForNaf(all));
    }



}
