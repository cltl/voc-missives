package utils.tei;

import utils.common.AbnormalProcessException;
import xjc.teiAll.TEI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.function.Function;

public class TeiReader {
    TEI tei;
    Function<TEI,ATeiTree> treeFactory;

    public TeiReader(String file, Function<TEI, ATeiTree> treeFactory) throws AbnormalProcessException {
        this.tei = load(file);
        this.treeFactory = treeFactory;
    }

    public Metadata getMetadata() throws AbnormalProcessException {
        return Metadata.create(tei);
    }

    public ATeiTree getTeiTree() {
        return treeFactory.apply(tei);
    }

    public static TEI load(String xml) throws AbnormalProcessException {
        File file = new File(xml);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(TEI.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            TEI tei = (TEI) jaxbUnmarshaller.unmarshal(file);
            return tei;
        } catch (UnmarshalException e) {
            throw new AbnormalProcessException(file.toString(), e);
        } catch (JAXBException e) {
            throw new AbnormalProcessException(file.toString(), e);
        }
    }
}
