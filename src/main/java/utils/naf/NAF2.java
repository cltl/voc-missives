package utils.naf;

import xjc.naf.NAF;
import xjc.naf.NafHeader;
import xjc.naf.Tunits;
import xjc.naf.Entities;
import xjc.naf.Raw;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "NAF")
public class NAF2 {

    public NAF2() {
        setLang("nl");
        setVersion("3.2");
    }

    @XmlElementRef(name = "nafHeader", type = NafHeader.class, required = false)
    private NafHeader nafHeader;

    //    Marshalling CDATA:
//    https://theopentutorials.com/tutorials/java/jaxb/jaxb-marshalling-and-unmarshalling-cdata-block/
    @XmlJavaTypeAdapter(value= CDataAdapter.class)
    private String raw;

    @XmlElementRef(name = "tunits", type = Tunits.class, required = false)
    private Tunits tunits;

    @XmlElementRef(name = "text", type = Text.class, required = false)
    private Text text;

    @XmlElementRef(name = "entities", type = Entities.class, required = false)
    private Entities entities;

    @XmlAttribute(name = "version")
    @XmlSchemaType(name = "anySimpleType")
    protected String version;

    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    @XmlSchemaType(name = "anySimpleType")
    protected String lang;

    public NafHeader getNafHeader() {
        return nafHeader;
    }

    public String getRaw() {
        return raw;
    }

    public Tunits getTunits() {
        return tunits;
    }

    public void setNafHeader(NafHeader nafHeader) {
        this.nafHeader = nafHeader;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public void setTunits(Tunits tunits) {
        this.tunits = tunits;
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public Entities getEntities() {
        return entities;
    }

    public void setEntities(Entities entities) {
        this.entities = entities;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public String toString() {
        return "NAF [raw=" + raw + "]";
    }
}

