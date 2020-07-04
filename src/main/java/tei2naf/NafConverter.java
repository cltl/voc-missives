package tei2naf;

import missives.IO;
import tei2xmi.*;
import utils.*;
import xjc.teiAll.TEI;

import java.nio.file.Path;

import static missives.ThrowingBiConsumer.throwingBiConsumerWrapper;

/**
 * Converts TEI document to NAF
 *
 * 01/07/20 Raw text layer extraction, and writing to XMI
 */
public class NafConverter {
    private static final String NAF_SFX = ".naf";
    boolean tokenize;
    public NafConverter(boolean tokenize) {
        this.tokenize = tokenize;
    }
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


    public void toNaf(String teiFile, String nafFile) throws AbnormalProcessException {
        Document document = extractParagraphs(teiFile);
        if (tokenize)
            document.segmentAndTokenize(Tokenizer.create());
        if (! document.isEmpty()) {
            NafDoc naf = new NafDoc();
            naf.read(document);
            naf.write(nafFile);
        }
    }

    public static void convertFile(Path file, String outdir, boolean tokenize) throws AbnormalProcessException {
        String fileId = file.getFileName().toString().replaceAll("\\." + Converter.TEI_SFX, NAF_SFX);
        String outfile = outdir + "/" + fileId;
        NafConverter converter = new NafConverter(tokenize);
        converter.toNaf(file.toString(), outfile);
    }

    public static void main(String[] args) {

        IO.loop(args[0], args[1],
                throwingBiConsumerWrapper((x, y) -> convertFile(x, y, args[2].length() > 0)));
    }


}
