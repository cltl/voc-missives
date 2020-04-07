package tei2xmi;

import org.junit.jupiter.api.Test;
import utils.ATeiTree;
import utils.TeiDiv;
import utils.TeiTreeFactory;
import xjc.tei.TEI;

import static org.junit.jupiter.api.Assertions.*;

class TeiTreeFactoryTest {

    @Test
    public void testCreationSimpleTEI() {
        String textFile = "src/test/resources/tei-xml/short_text.xml";
        TEI tei = Converter.load(textFile);
        ATeiTree tree = TeiTreeFactory.create(tei);
        assertEquals(tree.getTeiType(), ATeiTree.TeiType.TEI);
        assertEquals(((TeiDiv) tree).getChildren().size(), 1);

    }

}