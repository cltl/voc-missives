package entities.nafReference;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.common.BaseEntity;
import utils.naf.NafDoc;
import xjc.naf.Entity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NafXmiReaderTest {
    String xmiFile = "src/test/resources/nafxmi/INT_a6ac29.xmi";
    String nafFile = "src/test/resources/nafxmi/INT_a6ac29.naf";
    String outFile = "src/test/resources/nafxmi/INT_a6ac29.out";

    @Test
    public void testEntityIntegration() throws AbnormalProcessException {
        NafXmiReader nafXmiReader = new NafXmiReader(nafFile, "text", "test-xmi-text-integration");
        List<BaseEntity> entities = nafXmiReader.readEntities(xmiFile);

        NafDoc naf = nafXmiReader.getNaf();
        List<BaseEntity> all = naf.getBaseEntities(true);
        all.addAll(entities);
        assertEquals(all.size(), entities.size());
        List<Entity> sorted = nafXmiReader.sortAndRenameForNaf(all);
        naf.setEntities(sorted);

        naf.write(outFile);
        //nafXmiReader.process(xmiFile, outFile);
//        NafDoc naf = NafDoc.create(testfile);
//        List<Wf> allWfs = naf.getWfs();
//        List<Fragment> fragments = NafUnits.asFragments(naf.getTunits());
//        assertTrue(fragments.get(0).getId().contains("fw"));
//
//        SectionSelector selector = new SectionSelector("text", fragments);
//        List<Wf> textWfs = selector.filter(allWfs);
//        assertTrue(Integer.parseInt(textWfs.get(0).getOffset()) >= fragments.get(0).getEndIndex());

    }
}