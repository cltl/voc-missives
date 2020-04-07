package tei2xmi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.*;
import xjc.tei.TEI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.nio.file.Path;

public class Converter {

    CasDoc teiXmi;
    Tokenizer tokenizer;
    Document document;

    static String TEI_SFX = "xml";
    public static final Logger logger = LogManager.getLogger(Converter.class);

    public Converter(Formatter formatter, Metadata metadata) {
        this.teiXmi = CasDoc.create();
        this.tokenizer = Tokenizer.create();
        this.document = Document.create(formatter, metadata);
    }

    public static TEI load(String xml) {
        File file = new File(xml);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(TEI.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            TEI tei = (TEI) jaxbUnmarshaller.unmarshal(file);
            return tei;
        } catch (UnmarshalException e) {
            logger.error(file.toString(), e);
        } catch (JAXBException e) {
            logger.error(file.toString(), e);
        }
        return null;
    }

    private void convert(TEI tei, String outfile) {
        ATeiTree tree = TeiTreeFactory.create(tei);
        document.formatParagraphs(tree);
        document.segmentAndTokenize(tokenizer);
        if (! document.isEmpty())
            writeXmi(fullName(outfile));
    }

    public String fullName(String outfile) {
        return document.typedFileName(outfile) + CasDoc.FILE_EXT;
    }

    private void writeXmi(String xmiOut) {
        teiXmi.addMetadata(document.getMetadata());
        teiXmi.addRawText(document.getRawText());
        teiXmi.addParagraphs(document.getParagraphs());
        teiXmi.addSentences(document.getSentences());
        teiXmi.addTokens(document.getTokens());
        teiXmi.write(xmiOut);
    }

    public static boolean isIndex(TEI tei) {
        String title = (String) tei.getTeiHeader().getFileDesc().getTitleStmt().getTitles().get(0).getContent().get(0);
        return title.startsWith("Index");
    }

    public static void convertFile(Path file, String outdir) {
        String fileId = file.getFileName().toString().replaceAll("\\." + TEI_SFX, "");
        TEI tei = load(file.toString());
        Metadata metadata = null;
        try {
            metadata = Metadata.create(tei);
        } catch (NullPointerException e) {
            logger.warn("no fileDesc for " + fileId, e);
        }
        String outfile = outdir + "/" + fileId;
        Converter textConverter = new Converter(new TextFormatter(), metadata);
        textConverter.convert(tei, outfile);
        Converter noteConverter = new Converter(new NoteFormatter(), metadata);
        noteConverter.convert(tei, outfile);
    }



    public static void main(String[] args) {
        IO.loop(args[0], args[1], (x, y) -> convertFile(x, y));
    }


}
