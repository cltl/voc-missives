package utils.xmi;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import utils.common.AbnormalProcessException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;
import utils.tei.Metadata;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CasDoc {
    JCas jCas;
    TypeSystemDescription tsd;
    private static final String TSD_FILE = "src/main/resources/dkproTypeSystem.xml";
    public static final String FILE_EXT = ".entities.xmi";
    private static final String VOL_ID_PFX = "missiven:vol";
    private static final int VOL_FORMAT_INDEX = VOL_ID_PFX.length();

    private CasDoc(JCas jCas, TypeSystemDescription tsd) {
        this.jCas = jCas;
        this.tsd = tsd;
        init();
    }

    public static CasDoc create() throws AbnormalProcessException {
        XMLParser xmlp = UIMAFramework.getXMLParser();
        XMLInputSource fis = null;
        try {
            fis = new XMLInputSource(TSD_FILE);
            TypeSystemDescription tsd = xmlp.parseTypeSystemDescription(fis);
            fis.close();
            return new CasDoc(JCasFactory.createJCas(tsd), tsd);
        } catch (InvalidXMLException e) {
            throw new AbnormalProcessException("Error creating CAS doc", e);
        } catch (IOException e) {
            throw new AbnormalProcessException("Error creating CAS doc", e);
        } catch (ResourceInitializationException e) {
            throw new AbnormalProcessException("Error creating CAS doc", e);
        } catch (CASException e) {
            throw new AbnormalProcessException("Error creating CAS doc", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    throw new AbnormalProcessException("Error creating CAS doc", e);
                }
            }
        }
    }

    public static CasDoc create(String inXmi) throws AbnormalProcessException {
        CasDoc doc = create();
        doc.read(inXmi);
        return doc;
    }

    public JCas getjCas() {
        return jCas;
    }

    public void read(String xmi) throws AbnormalProcessException {
        try (FileInputStream fis = new FileInputStream(xmi)) {
            XmiCasDeserializer.deserialize(fis, jCas.getCas());
        } catch (SAXException e) {
            throw new AbnormalProcessException("Error reading " + xmi, e);
        } catch (IOException e) {
            throw new AbnormalProcessException("Error reading " + xmi, e);
        }
    }

    public void write(String outXmi) throws AbnormalProcessException {
        try (FileOutputStream fos = new FileOutputStream(outXmi)) {
            XMLSerializer sax2xml = new XMLSerializer(fos, true);
            XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(jCas.getTypeSystem());
            xmiCasSerializer.serialize(jCas.getCas(), sax2xml.getContentHandler());
        } catch (SAXException e) {
            throw new AbnormalProcessException("Error writing to " + outXmi, e);
        } catch (FileNotFoundException e) {
            throw new AbnormalProcessException("Error writing to " + outXmi, e);
        } catch (IOException e) {
            throw new AbnormalProcessException("Error writing to " + outXmi, e);
        }
    }

    public void init() {
        jCas.setDocumentLanguage("nl");
    }


    public void addRawText(String rawText) {
        jCas.setDocumentText(rawText);
    }

    public void addMetadata(Metadata metadata) {

        DocumentMetaData a = DocumentMetaData.create(jCas);
        a.setDocumentId(metadata.getDocumentId());
        a.setCollectionId(metadata.getCollectionId());
        a.setDocumentTitle(metadata.getDocumentTitle());
        a.addToIndexes(jCas);

        MetaDataStringField dateStringField = new MetaDataStringField(jCas);
        dateStringField.setKey("date");
        dateStringField.setValue(metadata.getDate());
        dateStringField.addToIndexes(jCas);
    }

    public List<Token> getTokens() {
        return (List) JCasUtil.select(jCas, Token.class);
    }

    public List<NamedEntity> getEntities() {
        return (List) JCasUtil.select(jCas, NamedEntity.class);
    }

    public void addEntity(int begin, int end, String value) {
        Annotation a = AnnotationFactory.createAnnotation(jCas, begin, end, NamedEntity.class);
        ((NamedEntity) a).setValue(value);
        a.addToIndexes(jCas);
    }

    public String getId() {
        List<DocumentMetaData> metadata = (List) JCasUtil.select(jCas, DocumentMetaData.class);
        return metadata.get(0).getDocumentId();
    }

    public String getVolumeId() {
        List<DocumentMetaData> metadata = (List) JCasUtil.select(jCas, DocumentMetaData.class);
        String collectionId = metadata.get(0).getCollectionId();

        if (! collectionId.startsWith(VOL_ID_PFX))
            throw new IllegalArgumentException("Collection ID has unexpected form: " + collectionId);
        return collectionId.substring(VOL_FORMAT_INDEX);
    }

    public String getRawText() {
        return jCas.getDocumentText();
    }

    public List<Sentence> getSentences() {
        return (List) JCasUtil.select(jCas, Sentence.class);
    }

    public String getDate() {
        Object x = ((List) JCasUtil.select(jCas, MetaDataStringField.class)).get(0);
        return ((MetaDataStringField) x).getValue();
    }

    public List<Paragraph> getParagraphs() {
        return (List) JCasUtil.select(jCas, Paragraph.class);
    }
}

