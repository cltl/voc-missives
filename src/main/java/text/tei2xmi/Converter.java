package text.tei2xmi;

import utils.common.AbnormalProcessException;
import utils.common.IO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tokens.Tokenizer;
import utils.tei.ATeiTree;
import utils.tei.Metadata;
import utils.tei.TeiReader;
import utils.xmi.CasDoc;

import java.nio.file.Path;

import static utils.common.ThrowingBiConsumer.throwingBiConsumerWrapper;

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

    private void convert(ATeiTree tree, String outfile) throws AbnormalProcessException {
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
        TeiReader teiReader = new TeiReader(file.toString(), x -> TeiTreeFactory.create(x));
        Metadata metadata = teiReader.getMetadata();
        ATeiTree tree = teiReader.getTeiTree();
        String outfile = outdir + "/" + fileId;
        Converter textConverter = new Converter(new TextFormatter(), metadata);
        textConverter.convert(tree, outfile);
        Converter noteConverter = new Converter(new NoteFormatter(), metadata);
        noteConverter.convert(tree, outfile);
    }


    public static void main(String[] args) {
        IO.loop(args[0], args[1],
            throwingBiConsumerWrapper((x, y) -> convertFile(x, y)));
    }

}
