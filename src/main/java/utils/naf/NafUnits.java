package utils.naf;

import xjc.naf.Entity;
import xjc.naf.Tunit;
import xjc.naf.Span;
import xjc.naf.Target;

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
        Span s = new Span();
        List<Target> ts = s.getTargets();

        for (Wf wf: tokenSpan) {
            Target t = new Target();
            t.setId(wf);
            ts.add(t);
        }
        e.getSpenAndExternalReferences().add(s);
        return e;
    }

    public static List<Wf> wfSpan(Entity e) {
        Span s = (Span) e.getSpenAndExternalReferences().stream().filter(x -> x instanceof Span).findFirst().orElse(null);
        List<Target> targets = s.getTargets();
        return targets.stream().map(t -> (Wf) t.getId()).collect(Collectors.toList());
    }

    public static utils.common.Span indexSpan(Entity e) {
        List<Wf> wfs = wfSpan(e);
        Wf last = wfs.get(wfs.size() - 1);
        int end = Integer.parseInt(last.getOffset()) + Integer.parseInt(last.getLength());
        int length = end - Integer.parseInt(wfs.get(0).getOffset());
        return utils.common.Span.fromCharPosition(wfs.get(0).getOffset(), "" + length);
    }

    public static String getContent(Wf wf) {
        return wf.getContent();
       // return (String) wf.getContent().stream().filter(x -> x instanceof String).findFirst().orElse(null);
    }

    public static Wf createWf(String token, int id, int offset, int sentenceId, int paragraphId) {
        Wf wf = createWf(token, id, offset);
        wf.setSent(sentenceId + "");
        wf.setPara(paragraphId + "");
        return wf;
    }

    public static Wf createWf(String token, int id, int offset, int sentenceId) {
        Wf wf = createWf(token, id, offset);
        wf.setSent(sentenceId + "");
        return wf;
    }

    public static Wf createWf(String token, int id, int offset) {
        Wf wf = new Wf();
        wf.setId("w" + id);
        wf.setOffset(offset + "");
        wf.setLength(token.length() + "");
        //wf.getContent().add(token);
        wf.setContent(token);
        return wf;
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
