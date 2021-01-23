package utils.naf;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * CDATA adapter for Raw layer XML serialization
 *
 * see:
 * https://theopentutorials.com/tutorials/java/jaxb/jaxb-marshalling-and-unmarshalling-cdata-block/
 */
public class CDataAdapter extends XmlAdapter<String, String> {

    @Override
    public String marshal(String v) throws Exception {
        return "<![CDATA[" + v + "]]>";
    }

    @Override
    public String unmarshal(String v) throws Exception {
        return v;
    }

}