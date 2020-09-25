package utils.naf;

import xjc.naf.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NafUnits {

    private static Pattern teiIdEndPattern = Pattern.compile("\\w+\\.\\d+$");

    public static Entity createEntity(String id, String type, List<Wf> tokenSpan) {
        Entity e = new Entity();
        e.setId(id);
        e.setType(type);
        References r = new References();
        Span s = new Span();
        List<Target> ts = s.getTargets();

        for (Wf wf: tokenSpan) {
            Target t = new Target();
            t.setId(wf);
            ts.add(t);
        }
        r.getSpen().add(s);
        e.getReferencesAndExternalReferences().add(r);
        return e;
    }

    public static List<Wf> wfSpan(Entity e) {
        References r = (References) e.getReferencesAndExternalReferences().get(0);
        List<Target> targets = r.getSpen().get(0).getTargets();
        return targets.stream().map(t -> (Wf) t.getId()).collect(Collectors.toList());
    }

    public static Tunit withOffset(Tunit tunit, int offset) {
        Tunit t = new Tunit();
        t.setXpath(tunit.getXpath());
        t.setType(tunit.getType());
        t.setId(tunit.getId());
        t.setLength(tunit.getLength());
        t.setOffset(offset + "");
        return t;
    }


    public static Tunit asTunit(Fragment p) {
        Tunit t = new Tunit();
        t.setId(p.getId());
        Matcher matcher = teiIdEndPattern.matcher(p.getId());
        if (matcher.find()) {
            String idEnd = p.getId().substring(matcher.start());
            t.setType(idEnd.substring(0, idEnd.indexOf(".")));
        }
        String xpathStr;
        if (p.getId().indexOf(".TEI.1") != -1) {
            xpathStr = p.getId().substring(p.getId().indexOf(".TEI.1") + 6);
            xpathStr = "/TEI" + xpathStr.replaceAll("\\.([a-zA-Z]+)\\.([0-9]+)", "/$1[$2]");
            t.setXpath(xpathStr);
        } else if (p.getId().indexOf(".TEI") != -1) {
            xpathStr = p.getId().substring(p.getId().indexOf(".TEI"));
            xpathStr = xpathStr.replaceAll("\\.([a-zA-Z]+)\\.([0-9]+)", "/$1[$2]");
            t.setXpath(xpathStr);
        }
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


}
