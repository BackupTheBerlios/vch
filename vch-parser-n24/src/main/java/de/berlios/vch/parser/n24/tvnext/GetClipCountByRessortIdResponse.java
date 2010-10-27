
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
 *         &lt;element name="getClipCountByRessortIdResult" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "getClipCountByRessortIdResult"
})
@XmlRootElement(name = "getClipCountByRessortIdResponse")
public class GetClipCountByRessortIdResponse {

    protected int getClipCountByRessortIdResult;

    /**
     * Gets the value of the getClipCountByRessortIdResult property.
     * 
     */
    public int getGetClipCountByRessortIdResult() {
        return getClipCountByRessortIdResult;
    }

    /**
     * Sets the value of the getClipCountByRessortIdResult property.
     * 
     */
    public void setGetClipCountByRessortIdResult(int value) {
        this.getClipCountByRessortIdResult = value;
    }

}
