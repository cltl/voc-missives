package entities.nafReference;

import utils.common.AbnormalProcessException;
import utils.common.BaseEntity;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import xjc.naf.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public interface NafEntityProcessor {
    boolean remapEntitiesToTerms = true;    // this is necessary for NAF3
    boolean termsMatchWfIndices = false;     // we make it happen
    void alignTokens() throws AbnormalProcessException;

    List<BaseEntity> readEntities(String input) throws AbnormalProcessException;

    default void process(String input, String output) throws AbnormalProcessException {
        List<BaseEntity> newEntities = readEntities(input);
        if (! newEntities.isEmpty())
            addEntities(getNaf(), newEntities, remapEntitiesToTerms);

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
            nafEntities.add(NafUnits.asNafEntity(entities.get(i).withId(i), remapEntitiesToTerms, getNaf()));
        return nafEntities;
    }

    void addLinguisticProcessor(String layer);

    /**
     * Existing NAF entities are converted to BaseEntities first.
     * This method is not intended for the treatment of entities containing external references.
     * @param entities
     * @return
     */
    default void addEntities(NafDoc naf, List<BaseEntity> entities, boolean remapEntitiesToTerms) {

        if (remapEntitiesToTerms) {
            if (getNaf().getTerms().isEmpty()) {
                addLinguisticProcessor("terms");
                getNaf().createTermsLayer();
            }
            if (! termsMatchWfIndices)
                entities = remap(entities, getNaf().getTerms(), remapEntitiesToTerms);
        }

        addLinguisticProcessor("entities");
        List<BaseEntity> all = getNaf().getBaseEntities(remapEntitiesToTerms);
        all.addAll(entities);
        getNaf().setEntities(sortAndRenameForNaf(all));
    }

    /**
     * Makes base entities point to term ids instead of wf ids
     *
     * @param entities
     * @param terms
     * @param mapToTerms
     * @return
     */
    default List<BaseEntity> remap(List<BaseEntity> entities, List<Term> terms, boolean mapToTerms) {

        HashMap<String,String> wf2ts = NafUnits.wf2terms(terms, mapToTerms);
        return entities.stream().map(e ->
            e.withSpan(Collections.singletonList(wf2ts.get("w" + e.getFirstTokenIndex()))))
                .collect(Collectors.toList());
    }


}
