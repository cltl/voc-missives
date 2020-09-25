package deprecated.nafReference;

import utils.common.AbnormalProcessException;
import deprecated.utils.BaseEntity;
import utils.naf.NafDoc;
import utils.naf.NafUnits;
import xjc.naf.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
@Deprecated
public interface NafEntityProcessor {
    boolean remapEntitiesToTerms = false;
    boolean termsMatchWfIndices = true;     // we make it happen
    void alignTokens() throws AbnormalProcessException;

    List<BaseEntity> readEntities(String input) throws AbnormalProcessException;

    default void process(String input, String output) throws AbnormalProcessException {
        List<BaseEntity> newEntities = readEntities(input);
        if (! newEntities.isEmpty())
            addEntities(newEntities, remapEntitiesToTerms);
        getNaf().write(output);
    }

    NafDoc getNaf();

    default List<Entity> sortAndRenameForNaf(List<BaseEntity> entities) throws AbnormalProcessException {
        Collections.sort(entities, BaseEntity::compareTo);
        List<BaseEntity> overlapping = BaseEntity.overlap(entities);
        if (! overlapping.isEmpty())
            BaseEntity.logger.warn("Found overlapping entities: "
                    + overlapping.stream().map(BaseEntity::toString).collect(Collectors.joining(", ")));
        List<Entity> nafEntities = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++)
            nafEntities.add(asNafEntity(entities.get(i).withId(i), getNaf()));
        return nafEntities;
    }


    static Entity asNafEntity(BaseEntity baseEntity, NafDoc nafDoc) {

        Entity e = new Entity();
        e.setId(baseEntity.getId());
        e.setType(baseEntity.getType());
        References r = new References();
        Span s = new Span();
        List<Target> ts = s.getTargets();

        for (int i = baseEntity.getFirstTokenIndex(); i <= baseEntity.getLastTokenIndex(); i++) {
            Target t = new Target();
            //Term term = nafDoc.getTerms().get(i);
            t.setId(nafDoc.getWfs().get(i));
            ts.add(t);
        }
        r.getSpen().add(s);
        e.getReferencesAndExternalReferences().add(r);
        return e;
    }
    void addLinguisticProcessor(String layer);

    /**
     * Existing NAF entities are converted to BaseEntities first.
     * This method is not intended for the treatment of entities containing external references.
     * @param entities
     * @return
     */
    default void addEntities(List<BaseEntity> entities, boolean remapEntitiesToTerms) throws AbnormalProcessException {

        if (remapEntitiesToTerms) {
            if (getNaf().getTerms().isEmpty()) {
                addLinguisticProcessor("terms");
                createTermsLayer();
            }
            if (! termsMatchWfIndices)
                entities = remap(entities, getNaf().getTerms(), remapEntitiesToTerms);
        }

        addLinguisticProcessor("entities");
        List<BaseEntity> all = getBaseEntities();
        all.addAll(entities);
        getNaf().setEntities(sortAndRenameForNaf(all));
    }

    default void createTermsLayer() {
        Terms termLayer = new Terms();
        termLayer.getTerms().addAll(getNaf().getWfs().stream().map(w -> getTerm(w)).collect(Collectors.toList()));
        getNaf().getLayers().add(termLayer);
    }


    static Term getTerm(Wf w) {
        Target t = new Target();
        t.setId(w);
        Span s = new Span();
        s.getTargets().add(t);
        Term term = new Term();
        term.setId("t" + w.getId().substring(1));
        term.getSentimentsAndSpenAndExternalReferences().add(s);
        return term;
    }

    default List<BaseEntity> getBaseEntities() {
        return getNaf().getEntities().stream().map(e -> asBaseEntity(e)).collect(Collectors.toList());
    }
    static BaseEntity asBaseEntity(Entity nafEntity) {
        return BaseEntity.create(nafEntity.getType(), nafEntity.getId(), getWfIdSpan(nafEntity));
    }


    static List<String> getWfIdSpan(Entity e) {
        List<Span> spans = e.getReferencesAndExternalReferences().stream()
                .filter(x -> x instanceof References)
                .map(x -> ((References) x).getSpen())
                .findFirst().orElse(Collections.EMPTY_LIST);
        return spans.get(0).getTargets().stream().map(x -> ((Term) x.getId()).getId()).collect(Collectors.toList());
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

        HashMap<String,String> wf2ts = wf2terms(terms, mapToTerms);
        return entities.stream().map(e ->
            e.withSpan(Collections.singletonList(wf2ts.get("w" + e.getFirstTokenIndex()))))
                .collect(Collectors.toList());
    }

    /**
     *
     * @param terms
     * @param entitiesMapToTerms    FIXME added for clarity's sake; can be inferred
     * @return a map of wf indices to term indices
     */
    static HashMap<String,String> wf2terms(List<Term> terms, boolean entitiesMapToTerms) {
        HashMap<String,String> wf2ts = new HashMap<>();
        terms.forEach(t -> {
            Span s = (Span) (t.getSentimentsAndSpenAndExternalReferences()
                    .stream().filter(x -> x instanceof Span)
                    .collect(Collectors.toList())).get(0);

            Target target = s.getTargets().get(0);
            //if (target.getId() instanceof Term)
            if (entitiesMapToTerms)
                wf2ts.put(((Wf) target.getId()).getId(), t.getId());
            else
                wf2ts.put((String) target.getId(), t.getId());
        });
        return wf2ts;
    }
}
