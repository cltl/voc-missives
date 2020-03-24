package tei2xmi;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;
import utils.Metadata;
import utils.Segment;
import utils.Segments;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CasDoc {
    JCas jCas;
    TypeSystemDescription tsd;
    private static final String TSD_FILE = "src/main/resources/dkproTypeSystem.xml";
    public static final String FILE_EXT = ".xmi";

    private CasDoc(JCas jCas, TypeSystemDescription tsd) {
        this.jCas = jCas;
        this.tsd = tsd;
        init();
    }

    public static CasDoc create() {
        XMLParser xmlp = UIMAFramework.getXMLParser();
        XMLInputSource fis = null;
        try {
            fis = new XMLInputSource(TSD_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        TypeSystemDescription tsd = null;
        try {
            tsd = xmlp.parseTypeSystemDescription(fis);
        } catch (InvalidXMLException e) {
            e.printStackTrace();
        }
        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new CasDoc(JCasFactory.createJCas(tsd), tsd);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        } catch (CASException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void write(String outXmi) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outXmi);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        XMLSerializer sax2xml = new XMLSerializer(fos, true);
        XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(jCas.getTypeSystem());
        try {
            xmiCasSerializer.serialize(jCas.getCas(), sax2xml.getContentHandler());
        } catch (SAXException e) {
            e.printStackTrace();
        }
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
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
}

