package utils.naf;

import entities.nafReference.NAFConllReader;
import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.common.BaseEntity;
import xjc.naf.Entity;
import xjc.naf.LinguisticProcessors;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NafDocTest {
    String example = "src/test/resources/nafInOut/example.naf";
    String outdir = "src/test/resources/nafInOut";

    @Test
    public void testEntityWriting() throws AbnormalProcessException {
        NAFConllReader ncr = new NAFConllReader(example, "all", " ", "test-naf-writing");
        List<Entity> existing = ncr.getNaf().getEntities();
        assertEquals(existing.size(), 1);
        LinguisticProcessors entitiesLPs = ncr.getNaf().getLinguisticProcessors().stream().filter(x -> x.getLayer().equals("entities")).collect(Collectors.toList()).get(0);
        assertEquals(entitiesLPs.getLps().size(), 2);
        // add entity
        List<String> tokenIds = Collections.singletonList(ncr.getNaf().getWfs().subList(10, 11).get(0).getId());
        BaseEntity newEntity = BaseEntity.create("PER", "e0", tokenIds);
        List<BaseEntity> newEntities = Collections.singletonList(newEntity);

        ncr.addEntities(newEntities, true);

        List<Entity> list2 = ncr.getNaf().getEntities();
        assertEquals(list2.size(), 2);
        assertEquals(entitiesLPs.getLps().size(), 2);

        String out = outdir + "/example_out.naf";
        ncr.getNaf().write(out);
        assertTrue(Files.exists(Paths.get(out)));
    }


}