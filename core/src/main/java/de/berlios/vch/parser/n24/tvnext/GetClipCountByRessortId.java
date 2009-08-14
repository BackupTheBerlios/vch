
package de.berlios.vch.parser.n24.tvnext;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ressortId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "ressortId"
})
@XmlRootElement(name = "getClipCountByRessortId")
public class GetClipCountByRessortId {

    protected int ressortId;

    /**
     * Gets the value of the ressortId property.
     * 
     */
    public int getRessortId() {
        return ressortId;
    }

    /**
     * Sets the value of the ressortId property.
     * 
     */
    public void setRessortId(int value) {
        this.ressortId = value;
    }

}
