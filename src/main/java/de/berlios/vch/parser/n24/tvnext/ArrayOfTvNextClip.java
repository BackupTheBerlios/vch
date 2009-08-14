
package de.berlios.vch.parser.n24.tvnext;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfTvNext_Clip complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfTvNext_Clip">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="TvNext_Clip" type="{http://schemas.exozet.com/tvnext/services/core/}TvNext_Clip" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfTvNext_Clip", propOrder = {
    "tvNextClip"
})
public class ArrayOfTvNextClip {

    @XmlElement(name = "TvNext_Clip", nillable = true)
    protected List<TvNextClip> tvNextClip;

    /**
     * Gets the value of the tvNextClip property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tvNextClip property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTvNextClip().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TvNextClip }
     * 
     * 
     */
    public List<TvNextClip> getTvNextClip() {
        if (tvNextClip == null) {
            tvNextClip = new ArrayList<TvNextClip>();
        }
        return this.tvNextClip;
    }

}
