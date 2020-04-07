package utils;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CasDoc {
    JCas jCas;
    TypeSystemDescription tsd;
    private static final String TSD_FILE = "src/main/resources/dkproTypeSystem.xml";
    public static final String FILE_EXT = ".xmi";
    private static final Logger logger = LogManager.getLogger(CasDoc.class);

    private CasDoc(JCas jCas, TypeSystemDescription tsd) {
        this.jCas = jCas;
        this.tsd = tsd;
        init();
    }

    public static CasDoc create() {
        XMLParser xmlp = UIMAFramework.getXMLParser();
        try {
            XMLInputSource fis = new XMLInputSource(TSD_FILE);
            TypeSystemDescription tsd = xmlp.parseTypeSystemDescription(fis);
            fis.close();
            return new CasDoc(JCasFactory.createJCas(tsd), tsd);
        } catch (InvalidXMLException e) {
            logger.fatal("Error creating CAS doc", e);
        } catch (IOException e) {
            logger.fatal("Error creating CAS doc", e);
        } catch (ResourceInitializationException e) {
            logger.fatal("Error creating CAS doc", e);
        } catch (CASException e) {
            logger.fatal("Error creating CAS doc", e);
        }
        return null;
    }

    public void read(String xmi) {
        try {
            FileInputStream fis = new FileInputStream(xmi);
            XmiCasDeserializer.deserialize(fis, jCas.getCas());
            fis.close();
        } catch (SAXException e) {
            logger.fatal("Error reading " + xmi, e);
        } catch (IOException e) {
            logger.fatal("Error reading " + xmi, e);
        }
    }

    public void write(String outXmi) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outXmi);
        } catch (FileNotFoundException e) {
            logger.fatal("Error writing to " + outXmi, e);
        }
        XMLSerializer sax2xml = new XMLSerializer(fos, true);
        XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(jCas.getTypeSystem());
        try {
            xmiCasSerializer.serialize(jCas.getCas(), sax2xml.getContentHandler());
        } catch (SAXException e) {
            logger.fatal("Error writing to " + outXmi, e);
        }
        try {
            fos.close();
        } catch (IOException e) {
            logger.fatal("Error writing to " + outXmi, e);
        }
    }

    public void init() {
        jCas.setDocumentLanguage("nl");
    }


    public void addRawText(String rawText) {
        jCas.setDocumentText(rawText);
    }

    public void addParagraphs(List<utils.Paragraph> paragraphs) {
        for (utils.Paragraph p: paragraphs) {
            int end = p.getOffset() + p.getContent().length();
            Annotation a = AnnotationFactory.createAnnotation(jCas, p.getOffset(), end, Paragraph.class);
            ((Paragraph) a).setId(p.getTeiId());
            a.addToIndexes(jCas);
        }
    }

    public void addSentences(Segments segmentedSentences) {
        for (Segment s: segmentedSentences.getSegments()) {
            Annotation a = AnnotationFactory.createAnnotation(jCas, s.getBegin(), s.getEnd(), Sentence.class);
            ((Sentence) a).setId("" + s.getIndex());
            a.addToIndexes(jCas);
        }
    }

    public void addTokens(Segments tokens) {
        for (Segment s: tokens.getSegments()) {
            Annotation a = AnnotationFactory.createAnnotation(jCas, s.getBegin(), s.getEnd(), Token.class);
            ((Token) a).setId("t" + s.getIndex());
            a.addToIndexes(jCas);
        }
    }

    public void addMetadata(Metadata metadata) {

        DocumentMetaData a = DocumentMetaData.create(jCas);
        a.setDocumentId(metadata.getDocumentId());
        a.setCollectionId(metadata.getCollectionId());
        a.setDocumentTitle(metadata.getDocumentTitle());
        a.addToIndexes(jCas);
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

    public String getRawText() {
        return jCas.getDocumentText();
    }

    public List getSentences() {
        return (List) JCasUtil.select(jCas, Sentence.class);
    }
}

