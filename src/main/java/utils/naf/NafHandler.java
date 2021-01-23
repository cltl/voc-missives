package utils.naf;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import missives.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;
import xjc.naf.*;

public class NafHandler {
    public static final Logger logger = LogManager.getLogger(NafHandler.class);
    NAF2 naf;
    public NafHandler() {
        naf = new NAF2();
    }

    public static NafHandler create(String nafFile) throws AbnormalProcessException {
        NafHandler nafHandler = new NafHandler();
        nafHandler.parse(nafFile);
        return nafHandler;
    }

    public static NafHandler create(NafHeader header) {
        NafHandler nafHandler = new NafHandler();
        nafHandler.setNafHeader(header);
        return nafHandler;
    }

    public static NafHandler create(String title, String fileName, String publicId) {
        NafHandler nafHandler = new NafHandler();
        NafHeader nafHeader = new NafHeader();
        FileDesc fileDesc = new FileDesc();
        fileDesc.setTitle(title);
        fileDesc.setFilename(fileName);
        nafHeader.setFileDesc(fileDesc);
        Public pub = new Public();
        pub.setPublicId(publicId);
        nafHeader.setPublic(pub);
        nafHandler.setNafHeader(nafHeader);
        return nafHandler;
    }

    public static NafHandler create(String fileName, String publicId) {
        NafHandler nafHandler = new NafHandler();
        NafHeader nafHeader = new NafHeader();
        FileDesc fileDesc = new FileDesc();
        fileDesc.setFilename(fileName);
        nafHeader.setFileDesc(fileDesc);
        Public pub = new Public();
        pub.setPublicId(publicId);
        nafHeader.setPublic(pub);
        nafHandler.setNafHeader(nafHeader);
        return nafHandler;
    }

    public NAF2 getNaf() {
        return naf;
    }

    private void parse(String naf) throws AbnormalProcessException {
        File file = new File(naf);
        if (! file.exists())
            throw new AbnormalProcessException("File does not exist: " + naf);
        try {
//            JAXBContext jaxbContext = JAXBContext.newInstance(NAF.class);
////            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
////            this.naf = NAF2.create((NAF) jaxbUnmarshaller.unmarshal(file));
            JAXBContext jaxbContext = JAXBContext.newInstance(NAF2.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            this.naf = (NAF2) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static LinguisticProcessors createLinguisticProcessors(String layer, String name) {
        LinguisticProcessors lps = initLinguisticProcessors(layer);
        lps.getLps().add(getLp(name));
        return lps;
    }

    private static Lp getLp(String name) {
        Lp lp = new Lp();
        lp.setName(name);
        lp.setVersion(Handler.VERSION);
        lp.setTimestamp(createTimestamp());
        return lp;
    }

    private static LinguisticProcessors initLinguisticProcessors(String layer) {
        LinguisticProcessors lps = new LinguisticProcessors();
        lps.setLayer(layer);
        return lps;
    }

    public List<LinguisticProcessors> getLinguisticProcessorsList() {
        NafHeader header = getNafHeader();
        return header.getLinguisticProcessors();
    }

    public LinguisticProcessors getLinguisticProcessors(String layer) {
        return getNafHeader().getLinguisticProcessors().stream().filter(lp -> lp.getLayer().equals(layer)).findFirst().orElse(null);
    }

    public static String createTimestamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

    /**
     * Add processor to existing or new list of processors for that layer
     * @param layer
     * @param processorName
     */
    private void addToHeader(String layer, String processorName) {
        LinguisticProcessors lps = getLinguisticProcessors(layer);
        if (lps == null) {
            lps = initLinguisticProcessors(layer);
            naf.getNafHeader().getLinguisticProcessors().add(lps);
        }
        lps.getLps().add(getLp(processorName));
    }

    public void setNafHeader(NafHeader header) {
        naf.setNafHeader(header);
    }

    public NafHeader getNafHeader() {
        return naf.getNafHeader();
    }

    public String getPublicId() {
        return getNafHeader().getPublic().getPublicId();
    }

    public String getFileName() {
        return getNafHeader().getFileDesc().getFilename();
    }

    public String getRawText() {
        return naf.getRaw();
    }

    public void setRawText(String text) {
        naf.setRaw(text);
    }

    public void createRawLayer(String rawText, String processorName) {
        naf.setRaw(rawText);
        addToHeader("raw", processorName);
    }

    public List<utils.naf.Wf> getWfs() {
        return naf.getText().getWves();
    }

    public void createTextLayer(List<Wf> wfs, String processorName) {
        Text textLayer = new Text();
        textLayer.getWves().addAll(wfs);
        naf.setText(textLayer);
        addToHeader("text", processorName);
    }

    public List<Tunit> getTunits() {
        Tunits tunits = naf.getTunits();
        if (tunits != null)
            return tunits.getTunits();
        else
            return Collections.EMPTY_LIST;
    }

    public void createTunitsLayer(List<Tunit> tunitList, String processorName) {
        Tunits tunitsLayer = new Tunits();
        tunitsLayer.getTunits().addAll(tunitList);
        naf.setTunits(tunitsLayer);
        addToHeader("tunits", processorName);
    }

    public List<Entity> getEntities() {
        Entities entitiesLayer = naf.getEntities();
        if (entitiesLayer != null)
            return entitiesLayer.getEntities();
        else
            return Collections.EMPTY_LIST;
    }

    public boolean hasEntitiesLayer() {
        return naf.getEntities() != null;
    }

    public void createEntitiesLayer(List<Entity> entities, String processorName) {
        Entities entitiesLayer = new Entities();
        entitiesLayer.getEntities().addAll(entities);
        naf.setEntities(entitiesLayer);
        addToHeader("entities", processorName);
    }

    public String coveredText(int begin, int end) {
        return naf.getRaw().substring(begin, end);
    }

    public String coveredText(Wf wf) {
        int begin = Integer.parseInt(wf.getOffset());
        int end = begin + Integer.parseInt(wf.getLength());
        return coveredText(begin, end);
    }

    public void writeToDir(String outdir) throws AbnormalProcessException {
        write(Paths.get(outdir, getFileName()).toString());
    }

    public void write(String file) throws AbnormalProcessException {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            JAXBContext jaxbContext = JAXBContext.newInstance(NAF2.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //            Marshalling CDATA:
//            https://theopentutorials.com/tutorials/java/jaxb/jaxb-marshalling-and-unmarshalling-cdata-block/
            jaxbMarshaller.setProperty(CharacterEscapeHandler.class.getName(),
                    (CharacterEscapeHandler) (ch, start, length, isAttVal, writer1)
                            -> writer1.write(ch, start, length));
            jaxbMarshaller.marshal(naf, writer);
        } catch (JAXBException e) {
            throw new AbnormalProcessException("error processing " + file, e);
        } catch (IOException e) {
            throw new AbnormalProcessException("error processing " + file, e);
        }
    }


}
