package tei2naf;

import tei2xmi.*;
import utils.*;
import xjc.teiAll.TEI;

import java.nio.file.Path;
import java.util.List;

import static utils.ThrowingBiConsumer.throwingBiConsumerWrapper;

/**
 * Converts TEI document to NAF
 *
 * 01/07/20 Raw text layer extraction, and writing to XMI
 */
public class NafConverter {


    /**
     * Extracts raw text from TEI file
     * @param teiFile
     * @return
     */
    public String extractText(String teiFile) throws AbnormalProcessException {
        Document doc = extractParagraphs(teiFile);
        return doc.getRawText();
    }


    public Document extractParagraphs(String teiFile) throws AbnormalProcessException {
        TEI tei = Converter.load(teiFile);
        ATeiTree tree = TeiRawTreeFactory.create(tei);
        Formatter formatter = new BaseFormatter();
        Metadata metadata = Metadata.create(tei);
        Document document = Document.create(formatter, metadata);
        document.formatParagraphs(tree);
        return document;
    }

    /**
     * TODO replace XMI by NAF
     * @param teiFile
     * @param xmiFile
     * @throws AbnormalProcessException
     */
    public void writeXmi(String teiFile, String xmiFile) throws AbnormalProcessException {
        Document document = extractParagraphs(teiFile);
        if (! document.isEmpty()) {
            CasDoc teiXmi = CasDoc.create();
            String outFile = document.typedFileName(xmiFile) + CasDoc.FILE_EXT;
            teiXmi.addMetadata(document.getMetadata());
            teiXmi.addRawText(document.getRawText());
            teiXmi.addParagraphs(document.getParagraphs());
            teiXmi.write(outFile);
        }
    }

    public static void convertFile(Path file, String outdir) throws AbnormalProcessException {
        String fileId = file.getFileName().toString().replaceAll("\\." + Converter.TEI_SFX, "");
        String outfile = outdir + "/" + fileId;
        NafConverter converter = new NafConverter();
        converter.writeXmi(file.toString(), outfile);
    }

    public static void main(String[] args) {
        IO.loop(args[0], args[1],
                throwingBiConsumerWrapper((x, y) -> convertFile(x, y)));
    }


}
