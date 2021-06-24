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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import missives.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;
import utils.common.Span;
import xjc.naf.*;

public class NafHandler {
    public static final Logger logger = LogManager.getLogger(NafHandler.class);
    NAF2 naf;
    public NafHandler() {
        naf = new NAF2();
    }
    private static final Pattern XPATH = Pattern.compile("volume\\[(\\d+)\\]/missive\\[(\\d+)\\]");

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
            JAXBContext jaxbContext = JAXBContext.newInstance(NAF2.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            this.naf = (NAF2) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }


    private static Lp getLp(String name, String version) {
        Lp lp = new Lp();
        lp.setName(name);
        lp.setVersion(version);
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
        addToHeader(layer, processorName, Handler.VERSION);
    }

    private void addToHeader(String layer, String processorName, String version) {
        LinguisticProcessors lps = getLinguisticProcessors(layer);
        if (lps == null) {
            lps = initLinguisticProcessors(layer);
            naf.getNafHeader().getLinguisticProcessors().add(lps);
        }
        lps.getLps().add(getLp(processorName, version));
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

    public void createRawLayer(String rawText, String processorName) {
        naf.setRaw(rawText);
        addToHeader("raw", processorName);
    }

    public List<utils.naf.Wf> getWfs() {
        return naf.getText().getWves();
    }

    public void createTextLayer(List<Wf> wfs, String processorName) {
        createTextLayer(wfs, processorName, Handler.VERSION);
    }

    public void createTextLayer(List<Wf> wfs, String processorName, String version) {
        Text textLayer = new Text();
        textLayer.getWves().addAll(wfs);
        naf.setText(textLayer);
        addToHeader("text", processorName, version);
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

    public String entityPfx() throws AbnormalProcessException {
        if (! getTunits().isEmpty()) {
            Tunit tunit = getTunits().get(0);
            Matcher m = XPATH.matcher(tunit.getXpath());
            if (m.find()) {
                String volume = m.group(1);
                String missive = m.group(2);
                return "e_" + getTextType(tunit.getType()) + volume + "_" + missive + "_";
            }
        }
        return "e_";
    }

    private String getTextType(String tunitType) throws AbnormalProcessException {
        if (tunitType.equals("remark") || tunitType.equals("footnote"))
            return "n";     // notes
        else if (tunitType.equals("header") || tunitType.equals("paragraph"))
            return "t";     // text
        else throw new AbnormalProcessException("unrecognized tunit type: " + tunitType);
    }

    public String coveredText(Entity e) {
        Span span = NafUnits.indexSpan(e);
        return coveredText(span.getFirstIndex(), span.getEnd());
    }

    public String coveredText(String offset, String length) {
        int begin = Integer.parseInt(offset);
        int end = begin + Integer.parseInt(length);
        return coveredText(begin, end);
    }

    public String coveredText(int begin, int end) {
        int i = Math.max(begin, 0);
        int j = Math.min(end, naf.getRaw().length() - 1);
        return naf.getRaw().substring(i, j);
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
