package tei2xmi;

import utils.*;
import xjc.tei.Date;
import xjc.tei.P;
import xjc.tei.TEI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class Converter {

    CasDoc teiXmi;
    Tokenizer tokenizer;
    Document document;

    static String TEI_SFX = "xml";


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
            System.out.println(file.toString());
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void convert(TEI tei, String outfile) {
        document.formatParagraphs(TeiTreeFactory.create(tei));
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
            System.out.println("no fileDesc for " + fileId);
        }
        String outfile = outdir + "/" + fileId;
        Converter textConverter = new Converter(new TextFormatter(), metadata);
        textConverter.convert(tei, outfile);
        Converter noteConverter = new Converter(new NoteFormatter(), metadata);
        noteConverter.convert(tei, outfile);
    }

    /**
     * Reads files in dir and converts them to index, or text/note files
     * @param indir
     * @param outdir
     */
    public static void convertDir(String indir, String outdir) {
        Path dirpath = Paths.get(outdir);
        if (!Files.exists(dirpath)) {
            try {
                Files.createDirectories(Paths.get(outdir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (Stream<Path> paths = Files.walk(Paths.get(indir))) {
            paths.filter(p -> Files.isRegularFile(p)).filter(p -> {
                try {
                    return ! Files.isHidden(p);
                } catch (IOException e) {
                    return false;
                }}).forEach(f -> convertFile(f, dirpath.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Converter.convertDir(args[0], args[1]);
    }

    private static String filedate(Path file) {
        String fileId = file.getFileName().toString().replaceAll("\\." + TEI_SFX, "");
        TEI tei = load(file.toString());
        StringBuilder sb = new StringBuilder();
        sb.append(fileId);
        try {
            List<P> z = tei.getTeiHeader().getFileDesc().getPublicationStmt().getPS();
            List<Object> x = z.get(0).getContent();
            Object y = x.stream().filter(c -> c instanceof Date).findFirst().orElse(null);
            try {
                String d = (String) ((Date) y).getContent().get(0);
                sb.append("\t").append(d);
            } catch (IndexOutOfBoundsException e) {
                sb.append("\t").append("None");
            }
        } catch (NullPointerException e) {
            sb.append("\t").append("None");
        }

        sb.append("\n");
        return sb.toString();
    }

}
