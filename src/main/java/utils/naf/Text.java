
package utils.naf;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "text")
public class Text {

    @XmlElement(name = "wf", required = true)
    protected List<Wf> wves;

    /**
     * Gets the value of the wves property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the wves property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWves().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Wf }
     * 
     * 
     */
    public List<Wf> getWves() {
        if (wves == null) {
            wves = new ArrayList<Wf>();
        }
        return this.wves;
    }

}
