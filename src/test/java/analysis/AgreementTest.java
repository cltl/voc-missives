package analysis;

import manIn2naf.IndexedEntity;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgreementTest {
    @Test
    public void testAgreement() {
        List<IndexedEntity> entities1 = new LinkedList<>();
        List<IndexedEntity> entities2 = new LinkedList<>();

        entities1.add(IndexedEntity.create("Maria", "PER", 0, 5));
        entities2.add(IndexedEntity.create("Maria", "SHP", 0, 5));

        Agreement agreement = new Agreement(entities1, entities2);
        assertTrue(agreement.getDiffLabel1().size() == 1);
        assertTrue(agreement.getDiffLabel2().size() == 1);
        assertEquals(agreement.proportion(agreement.getDiffLabel1()), 100);

        entities1.add(IndexedEntity.create("VOC", "ORG", 10, 13));
        entities2.add(IndexedEntity.create("de VOC", "ORG", 7, 13));
        agreement = new Agreement(entities1, entities2);
        assertTrue(agreement.getDiffSpan1().size() == 1);
        assertTrue(agreement.getDiffSpan2().size() == 1);
        assertEquals(agreement.proportion(agreement.getDiffLabel1()), 50);

        entities1.add(IndexedEntity.create("de VOC", "ORG", 7, 13));
        agreement = new Agreement(entities1, entities2);
        assertTrue(agreement.getDiffSpan1().size() == 0);
        assertTrue(agreement.getDiffSpan2().size() == 0);
        assertTrue(agreement.getShared().size() == 1);
        assertTrue(agreement.getUnaligned1().size() == 1);
        assertEquals((int) agreement.proportion(agreement.getDiffLabel1()), 33);

        agreement.fullReport();
    }


}