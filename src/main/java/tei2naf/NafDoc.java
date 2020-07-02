package tei2naf;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import tei2xmi.Document;
import utils.Paragraph;
import xjc.naf.*;

public class NafDoc {
    private final static String NAME = "voc-missives-tei-naf-converter";
    private final static String VERSION = "1.1";

    NAF naf;
    public NafDoc() {
        naf = new NAF();
        naf.setLang("nl");
        naf.setVersion("v3.1");
    }

    public NafDoc(NAF naf) {
        this.naf = naf;
    }

    public NAF getNaf() {
        return naf;
    }

    public void parse(String naf) {
        File file = new File(naf);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(NAF.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            this.naf = (NAF) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static Tunit createTunit(Paragraph p) {
        Tunit t = new Tunit();
        t.setId(p.getTeiId());
        t.setOffset(p.getOffset() + "");
        t.setLength(p.getContent().length() + "");
        return t;
    }

    static public String createTimestamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

    private static LinguisticProcessors createLinguisticProcessors(String layer) {
        LinguisticProcessors lps = new LinguisticProcessors();
        Lp lp = new Lp();
        lp.setName(NAME);
        lp.setVersion(VERSION);
        lp.setTimestamp(createTimestamp());
        lps.getLps().add(lp);
        lps.setLayer(layer);
        return lps;
    }

    private NafHeader getNafHeader() {
        return (NafHeader) naf.getNafHeadersAndRawsAndTopics().stream().filter(x -> x instanceof NafHeader).findFirst().orElse(null);
    }

    private Raw getRawLayer() {
        return (Raw) naf.getNafHeadersAndRawsAndTopics().stream().filter(x -> x instanceof Raw).findFirst().orElse(null);
    }

    public void read(Document document) {

        NafHeader nafHeader = new NafHeader();
        FileDesc fileDesc = new FileDesc();
        fileDesc.setTitle(document.getMetadata().getDocumentTitle());
        fileDesc.setFilename(document.getMetadata().getDocumentId());
        nafHeader.setFileDesc(fileDesc);

        Raw raw = new Raw(document.getRawText());
        nafHeader.getLinguisticProcessors().add(createLinguisticProcessors("raw"));

        Tunits tunits = new Tunits();
        document.getParagraphs().forEach(p -> tunits.getTunits().add(createTunit(p)));
        nafHeader.getLinguisticProcessors().add(createLinguisticProcessors("tunits"));

        List<Object> layers = naf.getNafHeadersAndRawsAndTopics();
        layers.add(nafHeader);
        layers.add(raw);
        layers.add(tunits);
    }

    public void write(String file) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(NAF.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            // the XML serializer is needed to generate CDATA blocks
            XMLSerializer serializer = getXMLSerializer(file);
            jaxbMarshaller.marshal(naf, serializer.asContentHandler());
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static XMLSerializer getXMLSerializer(String out) throws IOException {
        // configure an OutputFormat to handle CDATA
        OutputFormat of = new OutputFormat();
        of.setIndenting(true);      // specify indenting here for pretty printing
        //of.setIndent(2);

        // specify which of your elements you want to be handled as CDATA.
        // The use of the '^' between the namespaceURI and the localname
        // seems to be an implementation detail of the xerces code.
        // When processing xml that doesn't use namespaces, simply omit the
        // namespace prefix as shown in the third CDataElement below.
        String[] cdata = new String[] { "^raw" };   // the namespaceURI is "" for xjc.naf.RAW
        of.setCDataElements(cdata);

        // set any other options you'd like
        //of.setPreserveSpace(true);
        //of.setNonEscapingElements(cdata);

        // create the serializer
        XMLSerializer serializer = new XMLSerializer(of);
        serializer.setOutputCharStream(new FileWriter(new File(out)));

        return serializer;
    }

}
