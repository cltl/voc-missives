package tei2naf;

import org.junit.jupiter.api.Test;
import missives.AbnormalProcessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NafConverterTest {
    @Test
    public void testRawTextLayerExtraction() throws AbnormalProcessException {
        String teiFile = "src/test/resources/tei-xml/index_persons.xml";
        NafConverter nc = new NafConverter(false);
        String extracted = nc.extractText(teiFile);
        String toExtract = "245INDEX VAN PERSOONSNAMEN\nAchmad, Sayid, Koning van Kirman, 124,\n 211, 213\nAdipati, Pangéran (Palembang), 78, 136\n Adipati Anom, Pangéran (Cheribon), 218\n Adipati Anom, Pangéran (Djambi), 199,\n 200, 226\nAdipati Anom, Pangéran (Palembang), Pangéran\n Mangkubumi, 4, 14, 34, 35, 47, 60,\n 111, 122, 135, 137, 156, 182, 183, 226, 236\nAlber(t)s, Joannes Coenradus, 66\nAnom, Sultan, zie Adipati Anom, Pangéran\n (Palembang)\n246INDEX VAN PERSOONSNAMENBaning, Entjik, 46\n";
        assertEquals(extracted, toExtract);

        teiFile = "src/test/resources/tei-xml/table.xml";
        extracted = nc.extractText(teiFile);
        toExtract = "Coetchin .\n»\n32925 - d.° janu.r 1686 tot\nd.° december 1686 . . .\nff\n139761\nff\n26547\nff\n199232\n";
        assertEquals(extracted, toExtract);

        teiFile = "src/test/resources/tei-xml/text_and_notes.xml";
        extracted = nc.extractText(teiFile);
        toExtract = "Reniers, Maetsuyker, Demmer, Hartzinck, Van Oudtshoorn, enz. VI, 5 februari 1652 583VI. CAREL RENIERS, JOAN MAETSUYKER, GERARD DEMMER,\n CAREL HARTZINCK, AERNOUT DE VLAMING VAN OUDTSHOORN,\n CORNELIS CAESAR EN WILLEM VERSTEGHEN, BATAVIA 5 febr. 1652.\n1079, fol. 167-172.\n(Afsluiting contract met den Koning van Ternate, af gedrukt Corpus II, nr.\n CGIV; zending van 3 fluitschepen met cargasoen van f. 73809.15.5 naar Makassar;\n Daer wort gepresumeert ende heeft groote waerschijnelijcheyt, dat den\n Coningh voorgehat heeft het jacht1) af te loopen ende het volck doot te slaen, deselve priserende 2) ende betalende na sijn welgevallen, daer sij apparent noch\n1) NI. dat van Verstegen.\n2) Priserende, schattende, de waarde bepalende.\n584 Reniers, Maetsuyker, Demmer, Hartzinck, Van Oudtshoorn, snz. VI, 5 februari 1652al moedich ende stout op sijn. De mestice barbier 1)J aldaer voor desen gevangen,is daer alsnu weder verbleven, niet afgecomen sijnde, als d’andere vertrocken, diewij dan vresen, beswaerlijck uyt hare handen sal geraecken, te meer dewijl wij deQuinammers alhier, die op de getroffen vrede rede waren vrijgelaten, weder inslavernij e hebben doen nemen.( Verwachting , dat de schepen 7 of 8 febr. naar Amboina kunnen vertrekken) .1) Geheten Jacob Discordt.\n";
        assertEquals(extracted, toExtract);
    }

    @Test
    public void testSectionFlattening() throws AbnormalProcessException {
        String teiFile = "src/test/resources/tei-xml/index_persons.xml";
        NafConverter nc = new NafConverter(false);
        BaseDoc doc = nc.getBaseDoc(teiFile);
        List<Fragment> sections = doc.getSections();
        List<Fragment> flat = doc.getNonOverlappingSections();
        assertEquals(sections.size(), flat.size());


        teiFile = "src/test/resources/tei-xml/text_and_notes.xml";
        doc = nc.getBaseDoc(teiFile);
        sections = doc.getSections();
        flat = doc.getNonOverlappingSections();
        assertTrue(sections.size() < flat.size());  // section 4 gets split in two
        Fragment lastSection = sections.get(4);
        String id = lastSection.getId();
        // checking ids
        for (int i = sections.size() - 1; i < flat.size(); i++) {
            String flatId = flat.get(i).getId();
            assertTrue(flatId.equals(id)
                    || flatId.startsWith(id + ".note"));
        }
    }

}