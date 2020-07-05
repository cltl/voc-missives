package tei2naf;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import naf2conll.BaseEntity;
import naf2conll.SectionSelector;
import xjc.naf.*;

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

    public static Tunit createTunit(Fragment p) {
        Tunit t = new Tunit();
        t.setId(p.getId());
        t.setOffset(p.getOffset() + "");
        t.setLength(p.getLength() + "");
        return t;
    }


    public List<Wf> getWfs() {
        Text textLayer = (Text) naf.getNafHeadersAndRawsAndTopics().stream().filter(x -> x instanceof Text).findFirst().orElse(null);
        return textLayer.getWves();
    }

    public List<Entity> getEntities() {
        Entities entitiesLayer = (Entities) naf.getNafHeadersAndRawsAndTopics().stream().filter(x -> x instanceof Entities).findFirst().orElse(null);
        if (entitiesLayer != null)
            return entitiesLayer.getEntities();
        else
            return Collections.EMPTY_LIST;
    }


    public void setEntities(List<Entity> entities) {
        Entities entitiesLayer = (Entities) naf.getNafHeadersAndRawsAndTopics().stream().filter(x -> x instanceof Entities).findFirst().orElse(null);
        if (entitiesLayer != null)
            entitiesLayer.getEntities().clear();
        else
            entitiesLayer = new Entities();
        entitiesLayer.getEntities().addAll(entities);
        naf.getNafHeadersAndRawsAndTopics().add(entitiesLayer);
    }

    public List<BaseEntity> getBaseEntities() {
        return getEntities().stream().map(BaseEntity::create).collect(Collectors.toList());
    }

    public List<Wf> entitySpan(Entity e) {
        List<Span> spans = e.getReferencesAndExternalReferences().stream()
                .filter(x -> x instanceof References)
                .map(x -> ((References) x).getSpen())
                .findFirst().orElse(Collections.EMPTY_LIST);
        List<String> targets = spans.get(0).getTargets().stream().map(x -> (String) x.getId()).collect(Collectors.toList());
        return getWfs().stream().filter(w -> targets.contains(w.getId())).collect(Collectors.toList());
    }


    public List<Tunit> getTunits() {
        Tunits tunits = (Tunits) naf.getNafHeadersAndRawsAndTopics().stream().filter(x -> x instanceof Tunits).findFirst().orElse(null);
        if (tunits != null)
            return tunits.getTunits();
        else
            return Collections.EMPTY_LIST;
    }


    /**
     * Selects tokens belonging to text/notes/mixed/all depending on <code>selectText</code>
     * @return
     */
    public List<Wf> selectTokens(String selectText) {
        List<Fragment> sections = getTunits().stream().map(t -> new Fragment(t.getId(), t.getOffset(), t.getLength())).collect(Collectors.toList());
        List<Fragment> disjointSections = Fragment.flatten(sections);
        SectionSelector sectionSelector = new SectionSelector(selectText, disjointSections);
        return sectionSelector.filter(getWfs());
    }

    public List<Object> getLayers() {
        return naf.getNafHeadersAndRawsAndTopics();
    }

    public static Wf createWf(String wordForm, int sentID, Fragment t) {
        Wf wf = new Wf();
        wf.setId("w" + t.getId());
        wf.setSent(sentID + "");
        wf.setContent(wordForm);
        wf.setOffset(t.getOffset() + "");
        wf.setLength(t.getLength() + "");
        return wf;
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
