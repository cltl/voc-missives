package tei2xmi;

import missives.AbnormalProcessException;
import missives.IO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.*;
import xjc.teiAll.TEI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.nio.file.Path;

import static missives.ThrowingBiConsumer.throwingBiConsumerWrapper;

public class Converter {

    CasDoc teiXmi;
    Tokenizer tokenizer;
    Document document;

    public static String TEI_SFX = "xml";
    public static final Logger logger = LogManager.getLogger(Converter.class);

    public Converter(Formatter formatter, Metadata metadata) throws AbnormalProcessException {
        this.teiXmi = CasDoc.create();
        this.tokenizer = Tokenizer.create();
        this.document = Document.create(formatter, metadata);
    }

    public static TEI load(String xml) throws AbnormalProcessException {
        File file = new File(xml);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(TEI.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            TEI tei = (TEI) jaxbUnmarshaller.unmarshal(file);
            return tei;
        } catch (UnmarshalException e) {
            throw new AbnormalProcessException(file.toString(), e);
        } catch (JAXBException e) {
            throw new AbnormalProcessException(file.toString(), e);
        }
    }

    private void convert(TEI tei, String outfile) throws AbnormalProcessException {
        ATeiTree tree;
        try {
            tree = TeiTreeFactory.create(tei);
        } catch (IllegalArgumentException e) {
            throw new AbnormalProcessException("Error while creating TEI tree", e);
        }
        document.formatParagraphs(tree);
        document.segmentAndTokenize(tokenizer);
        if (! document.isEmpty())
            writeXmi(fullName(outfile));
    }

    public String fullName(String outfile) {
        return document.typedFileName(outfile) + CasDoc.FILE_EXT;
    }

    private void writeXmi(String xmiOut) throws AbnormalProcessException {
        teiXmi.addMetadata(document.getMetadata());
        teiXmi.addRawText(document.getRawText());
        teiXmi.addParagraphs(document.getParagraphs());
        teiXmi.addSentences(document.getSentences());
        teiXmi.addTokens(document.getTokens());
        teiXmi.write(xmiOut);
    }


    public static void convertFile(Path file, String outdir) throws AbnormalProcessException {
        String fileId = file.getFileName().toString().replaceAll("\\." + TEI_SFX, "");
        TEI tei = load(file.toString());
        Metadata metadata = Metadata.create(tei);
        String outfile = outdir + "/" + fileId;
        Converter textConverter = new Converter(new TextFormatter(), metadata);
        textConverter.convert(tei, outfile);
        Converter noteConverter = new Converter(new NoteFormatter(), metadata);
        noteConverter.convert(tei, outfile);
    }


    public static void main(String[] args) {
        IO.loop(args[0], args[1],
            throwingBiConsumerWrapper((x, y) -> convertFile(x, y)));
    }

}
