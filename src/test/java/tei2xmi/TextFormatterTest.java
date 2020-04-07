package tei2xmi;

import org.junit.jupiter.api.Test;
import utils.ATeiTree;
import utils.TeiLeaf;
import utils.TeiTreeFactory;
import xjc.tei.TEI;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextFormatterTest {
    @Test
    public void testTable() {
        String input = "src/test/resources/tei-xml/table.xml";
        TextFormatter formatter = new TextFormatter();
        TEI tei = Converter.load(input);
        List<TeiLeaf> paragraphs = formatter.format(TeiTreeFactory.create(tei));
        int cellIds = (int) paragraphs.stream().map(p -> p.getId()).filter(i -> i.contains("table") && i.contains("cell")).count();
        assertEquals(cellIds, 0);
    }

}