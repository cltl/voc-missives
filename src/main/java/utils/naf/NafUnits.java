package utils.naf;

import utils.common.BaseEntity;
import utils.common.BaseToken;
import utils.common.Fragment;
import xjc.naf.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class NafUnits {


    /**
     *
     * @param terms
     * @param entitiesMapToTerms    FIXME added for clarity's sake; can be inferred
     * @return a map of wf indices to term indices
     */
    public static HashMap<String,String> wf2terms(List<Term> terms, boolean entitiesMapToTerms) {
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

    public static Entity asNafEntity(BaseEntity baseEntity, NafDoc nafDoc) {

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

    public static BaseToken asBaseToken(Wf token) {
        return BaseToken.create(token.getContent(), token.getId(), token.getOffset(), token.getLength());
    }

    public static List<String> getWfIdSpan(Entity e) {
        List<Span> spans = e.getReferencesAndExternalReferences().stream()
                .filter(x -> x instanceof References)
                .map(x -> ((References) x).getSpen())
                .findFirst().orElse(Collections.EMPTY_LIST);
        return spans.get(0).getTargets().stream().map(x -> ((Term) x.getId()).getId()).collect(Collectors.toList());
    }

    public static BaseEntity asBaseEntity(Entity nafEntity) {
        return BaseEntity.create(nafEntity.getType(), nafEntity.getId(), getWfIdSpan(nafEntity));
    }

    public static Tunit asTunit(Fragment p) {
        Tunit t = new Tunit();
        t.setId(p.getId());
        t.setOffset(p.getOffset() + "");
        t.setLength(p.getLength() + "");
        return t;
    }

    public static Fragment asFragment(Tunit t) {
        return new Fragment(t.getId(), t.getOffset(), t.getLength());
    }

    public static List<Fragment> asFragments(List<Tunit> tunits) {
        return tunits.stream().map(NafUnits::asFragment).collect(Collectors.toList());

    }

    public static Wf createWf(String wordForm, int sentID, Fragment t) {
        Wf wf = new Wf();
        wf.setId("w" + t.getId());
        wf.setSent(sentID + "");
        wf.setContent(wordForm);
        wf.setOffset(t.getOffset() + "");
        wf.setLength(t.getLength() + "");
        return wf;
    }

    public static Term getTerm(Wf w) {
        Target t = new Target();
        t.setId(w);
        Span s = new Span();
        s.getTargets().add(t);
        Term term = new Term();
        term.setId("t" + w.getId().substring(1));
        term.getSentimentsAndSpenAndExternalReferences().add(s);
        return term;
    }
}
