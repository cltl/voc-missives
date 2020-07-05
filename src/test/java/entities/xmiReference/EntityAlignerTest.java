package entities.xmiReference;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;

import static org.junit.jupiter.api.Assertions.*;

class EntityAlignerTest {
    @Test
    public void testQuick() {
        String extFile = "src/test/resources/xmiReference/external.xmi";
        String refFile = "src/test/resources/xmiReference/reference.xmi";

        EntityAligner entityAligner = null;
        try {
            entityAligner = EntityAligner.create(refFile, extFile);
        } catch (AbnormalProcessException e) {
            e.printStackTrace();
        }
        entityAligner.run();
        assertTrue(entityAligner.allAligned());
    }

}