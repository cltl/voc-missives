package utils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xjc.tei.*;

import java.util.List;
import java.util.stream.Collectors;

public class TeiTreeFactory {

    public static ATeiTree create(Object o) {
        if (o instanceof TEI)
            return TeiDiv.create(ATeiTree.TeiType.TEI, ((TEI) o).getId(), createChildren((TEI) o));
        else if (o instanceof Body)
            return TeiDiv.create(ATeiTree.TeiType.BODY, ((Body) o).getId(), createChildren((Body) o));
        else if (o instanceof Div)
            return TeiDiv.create(ATeiTree.TeiType.DIV, ((Div) o).getId(), createChildren(((Div) o)));
        else if (o instanceof String)
            return TeiLeaf.create(ATeiTree.TeiType.STR, null, ((String) o).replace("\n", ""));
        else if (o instanceof Lb)
            return TeiBreak.createLineBreak();
        else if (o instanceof Head)
            return TeiDiv.create(ATeiTree.TeiType.HEAD, ((Head) o).getId(), createChildren(((Head) o).getContent()));
        else if (o instanceof P)
            return TeiDiv.create(ATeiTree.TeiType.P, ((P) o).getId(), createChildren(((P) o).getContent()));
        else if (o instanceof Hi)
            return TeiDiv.create(ATeiTree.TeiType.HI, ((Hi) o).getId(), createChildren(((Hi) o).getContent()));
        else if (o instanceof Note)
            return TeiDiv.create(ATeiTree.TeiType.NOTE, ((Note) o).getId(), createChildren(((Note) o).getContent()));
        else if (o instanceof Table)
            return TeiDiv.create(ATeiTree.TeiType.TABLE, ((Table) o).getId(), createChildren(((Table) o).getHeadsAndIndicesAndInterps()));
        else if (o instanceof Row)
            return TeiDiv.create(ATeiTree.TeiType.ROW, ((Row) o).getId(), createChildren((Row) o));
        else if (o instanceof Cell)
            return TeiDiv.create(ATeiTree.TeiType.CELL, ((Cell) o).getId(), createChildren(((Cell) o).getContent()));
        else if (o instanceof Pb)
            return TeiBreak.createPageBreak(((Pb) o).getId(),(((Pb) o).getN()));
        else if (o instanceof Text)
            return TeiDiv.create(ATeiTree.TeiType.TEXT, ((Text) o).getId(), createChildren(((Text) o).getIndicesAndInterpsAndInterpGrps()));
        else
            throw new IllegalArgumentException("Unknown TEI type: " + o.getClass());
    }


    private static List<ATeiTree> createChildren(Row t) {
        return t.getCells().stream().map(c -> create(c)).collect(Collectors.toList());
    }

    private static List<ATeiTree> createChildren(List<Object> content) {
        return content.stream().map(c -> create(c)).collect(Collectors.toList());
    }

    public static List<ATeiTree> createChildren(TEI t) {
        return createChildren(t.getTexts().stream()
                .map(x -> x.getIndicesAndInterpsAndInterpGrps())
                .flatMap(x -> x.stream()).collect(Collectors.toList()));
    }

    public static List<ATeiTree> createChildren(Body t) {
        return createChildren(t.getIndicesAndInterpsAndInterpGrps());
    }

    private static List<ATeiTree> createChildren(Div t) {
        return createChildren(t.getBylinesAndDatelinesAndArguments());
    }

}
