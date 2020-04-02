package xmi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityAlignerTest {
    @Test
    public void testQuick() {
        String extFile = "src/test/resources/external.xmi";
        String refFile = "src/test/resources/reference.xmi";

        EntityAligner entityAligner = EntityAligner.create(refFile, extFile);
        entityAligner.run();
        assertTrue(entityAligner.allAligned());
    }

}