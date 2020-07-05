package utils.naf;

import utils.common.BaseEntity;
import utils.common.BaseToken;
import utils.common.Fragment;
import xjc.naf.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NafUnits {

    public static Entity asNafEntity(BaseEntity baseEntity) {
        Entity e = new Entity();
        e.setId(baseEntity.getId());
        e.setType(baseEntity.getType());
        References r = new References();
        Span s = new Span();
        List<Target> ts = s.getTargets();
        for (int i = baseEntity.getFirstTokenIndex(); i <= baseEntity.getLastTokenIndex(); i++) {
            Target t = new Target();
            t.setId("w" + i);
            ts.add(t);
        }
        r.getSpen().add(s);
        e.getReferencesAndExternalReferences().add(r);
        return e;
    }

    public static BaseToken asBaseToken(Wf token) {
        return BaseToken.create(token.getContent(), token.getId(), token.getOffset(), token.getLength());
    }

    public static BaseEntity asBaseEntity(Entity nafEntity) {
        List<Span> spans = nafEntity.getReferencesAndExternalReferences().stream()
                .filter(x -> x instanceof References)
                .map(x -> ((References) x).getSpen())
                .findFirst().orElse(Collections.EMPTY_LIST);
        List<String> targets = spans.get(0).getTargets().stream().map(x -> (String) x.getId()).collect(Collectors.toList());
        return BaseEntity.create(nafEntity.getType(), nafEntity.getId(), targets);
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
}
