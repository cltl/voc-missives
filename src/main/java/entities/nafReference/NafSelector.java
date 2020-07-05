package entities.nafReference;

import utils.naf.NafDoc;
import utils.naf.NafUnits;
import xjc.naf.Wf;

import java.util.List;

public interface NafSelector {
    /**
     * Selects tokens belonging to text/notes/mixed/all depending on <code>selectText</code>
     * @return
     */
    default List<Wf> selectedTokens(NafDoc naf, String selectText) {
        SectionSelector selector = new SectionSelector(selectText, NafUnits.asFragments(naf.getTunits()));
        return selector.filter(naf.getWfs());
    }
}
