package deprecated.tei2xmi;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.tei.ATeiTree;
import utils.tei.TeiDiv;
import utils.tei.TeiReader;

import static org.junit.jupiter.api.Assertions.*;
@Deprecated
class TeiTreeFactoryTest {

    @Test
    public void testCreationSimpleTEI() throws AbnormalProcessException {
        String textFile = "src/test/resources/tei2naf/short_text.xml";
        TeiReader teiReader = new TeiReader(textFile, x -> TeiTreeFactory.create(x));
        ATeiTree tree = teiReader.getTeiTree();
        assertEquals(tree.getTeiType(), ATeiTree.TeiType.TEI);
        assertEquals(((TeiDiv) tree).getChildren().size(), 1);

    }

}