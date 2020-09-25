package deprecated.tei2xmi;

import org.junit.jupiter.api.Test;
import utils.common.AbnormalProcessException;
import utils.tei.TeiLeaf;
import utils.tei.TeiReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@Deprecated
class TextFormatterTest {
    @Test
    public void testTable() throws AbnormalProcessException {
        String input = "src/test/resources/tei2naf/table.xml";
        TextFormatter formatter = new TextFormatter();
        TeiReader teiReader = new TeiReader(input, x -> TeiTreeFactory.create(x));
        List<TeiLeaf> paragraphs = formatter.format(teiReader.getTeiTree());
        int cellIds = (int) paragraphs.stream().map(p -> p.getId()).filter(i -> i.contains("table") && i.contains("cell")).count();
        assertEquals(cellIds, 0);
    }

    @Test
    public void testText() throws AbnormalProcessException {
        String input = "src/test/resources/tei2naf/text_and_notes.xml";
        TextFormatter formatter = new TextFormatter();
        TeiReader teiReader = new TeiReader(input, x -> TeiTreeFactory.create(x));
        List<TeiLeaf> paragraphs = formatter.format(teiReader.getTeiTree());
        String head = "VI. CAREL RENIERS, JOAN MAETSUYKER, GERARD DEMMER, CAREL HARTZINCK, AERNOUT DE VLAMING VAN OUDTSHOORN, CORNELIS CAESAR EN WILLEM VERSTEGHEN, BATAVIA 5 febr. 1652.";
        String p1 = "1079, fol. 167-172.";
        String p2 = "Daer wort gepresumeert ende heeft groote waerschijnelijcheyt, dat den" +
                " Coningh voorgehat heeft het jacht1) af te loopen ende het volck doot te slaen, deselve priserende" +
                " 2) ende betalende na sijn welgevallen, daer sij apparent noch" +
                " 1) Geheten Jacob Discordt.";
        assertEquals(paragraphs.size(), 3);
        assertEquals(paragraphs.get(0).getContent(), head);
        assertEquals(paragraphs.get(1).getContent(), p1);
        assertEquals(paragraphs.get(2).getContent(), p2);
    }

}