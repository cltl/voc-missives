package utils.naf;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import utils.common.AbnormalProcessException;
import utils.common.BaseEntity;
import xjc.naf.*;
import xjc.naf.Span;

public class NafDoc {

    NAF naf;
    public NafDoc() {
        naf = new NAF();
        naf.setLang("nl");
        naf.setVersion("v3.1.b");
    }

    public static NafDoc create(String nafFile) {
        NafDoc naf = new NafDoc();
        naf.parse(nafFile);
        return naf;
    }

    public NAF getNaf() {
        return naf;
    }

    private void parse(String naf) {
        File file = new File(naf);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(NAF.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            this.naf = (NAF) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public Object getLayer(Predicate<Object> layerSelector) {
        return naf.getNafHeadersAndRawsAndTopics().stream().filter(layerSelector).findFirst().orElse(null);
    }

    public String getRawText() {
        Raw raw = (Raw) getLayer(x -> x instanceof Raw);
        return raw.getValue();
    }
    public NafHeader getNafHeader() {
        return (NafHeader) getLayer(x -> x instanceof NafHeader);
    }

    public String getId() {
        return getNafHeader().getFileDesc().getFilename();
    }

    public List<Wf> getWfs() {
        Text textLayer = (Text) getLayer(x -> x instanceof Text);
        return textLayer.getWves();
    }

    public List<Tunit> getTunits() {
        Tunits tunits = (Tunits) getLayer(x -> x instanceof Tunits);
        if (tunits != null)
            return tunits.getTunits();
        else
            return Collections.EMPTY_LIST;
    }

    public List<Object> getLayers() {
        return naf.getNafHeadersAndRawsAndTopics();
    }

    public List<LinguisticProcessors> getLinguisticProcessors() {
        NafHeader header = getNafHeader();
        return header.getLinguisticProcessors();
    }

    public List<Term> getTerms() {
        Terms termLayer = (Terms) getLayer(x -> x instanceof Terms);
        if (termLayer != null)
            return termLayer.getTerms();
        return Collections.EMPTY_LIST;
    }

    public void createTermsLayer() {
        Terms termLayer = new Terms();
        termLayer.getTerms().addAll(getWfs().stream().map(NafUnits::getTerm).collect(Collectors.toList()));
        naf.getNafHeadersAndRawsAndTopics().add(termLayer);
    }

    public List<Entity> getEntities() {
        Entities entitiesLayer = (Entities) getLayer(x -> x instanceof Entities);
        if (entitiesLayer != null)
            return entitiesLayer.getEntities();
        else
            return Collections.EMPTY_LIST;
    }

    /**
     * Replaces entities layer (or creates a new one) by input entities
     * @param entities
     */
    public void setEntities(List<Entity> entities) {
        Entities entitiesLayer = new Entities();
        entitiesLayer.getEntities().addAll(entities);
        Entities existingLayer = (Entities) getLayer(x -> x instanceof Entities);
        int i = naf.getNafHeadersAndRawsAndTopics().indexOf(existingLayer);
        if (i != -1) {
            naf.getNafHeadersAndRawsAndTopics().remove(existingLayer);
            naf.getNafHeadersAndRawsAndTopics().add(i, entitiesLayer);
        } else
            naf.getNafHeadersAndRawsAndTopics().add(entitiesLayer);


    }

    public List<BaseEntity> getBaseEntities() {
        return getEntities().stream().map(e -> NafUnits.asBaseEntity(e)).collect(Collectors.toList());
    }

    public List<Wf> getTargetSpan(Entity e) {

        List<Span> spans = e.getReferencesAndExternalReferences().stream()
                .filter(x -> x instanceof References)
                .map(x -> ((References) x).getSpen())
                .findFirst().orElse(Collections.EMPTY_LIST);
        List<String> targets = spans.get(0).getTargets().stream().map(x -> ((Term) x.getId()).getId()).collect(Collectors.toList());
        return getWfs().stream().filter(w -> targets.contains(w.getId())).collect(Collectors.toList());
    }

    public void write(String file) throws AbnormalProcessException {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(NAF.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            // the XML serializer is needed to generate CDATA blocks
            XMLSerializer serializer = getXMLSerializer(file);
            jaxbMarshaller.marshal(naf, serializer.asContentHandler());
        } catch (JAXBException e) {
            throw new AbnormalProcessException("error processing " + file, e);
        } catch (IOException e) {
            throw new AbnormalProcessException("error processing " + file, e);
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

        // asBaseToken the serializer
        XMLSerializer serializer = new XMLSerializer(of);
        serializer.setOutputCharStream(new FileWriter(new File(out)));

        return serializer;
    }


}