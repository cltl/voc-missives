package naf2naf;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class NafTokenizerTest {
    private final String testdirOut = "src/test/resources/out";
    private final String testDir = "src/test/resources/tf";
    private final String fileName = "missive_9_9_text_base.naf";

    @Test
    public void testTokenize() throws AbnormalProcessException {
        NafTokenizer tokenizer = new NafTokenizer(Paths.get(testDir, fileName).toString());
        tokenizer.tokenize();
        assertEquals(tokenizer.getNaf().getFileName(), "missive_9_9_text.naf");
        assertFalse(tokenizer.getNaf().getWfs().isEmpty());
    }


}