package xmi;

import org.junit.jupiter.api.Test;
import missives.AbnormalProcessException;

import static org.junit.jupiter.api.Assertions.*;

class EntityAlignerTest {
    @Test
    public void testQuick() {
        String extFile = "src/test/resources/external.xmi";
        String refFile = "src/test/resources/reference.xmi";

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