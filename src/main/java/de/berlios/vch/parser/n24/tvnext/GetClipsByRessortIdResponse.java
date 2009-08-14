
package de.berlios.vch.parser.n24.tvnext;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
 *         &lt;element name="getClipsByRessortIdResult" type="{http://schemas.exozet.com/tvnext/services/core/}ArrayOfTvNext_Clip"/>
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
    "getClipsByRessortIdResult"
})
@XmlRootElement(name = "getClipsByRessortIdResponse")
public class GetClipsByRessortIdResponse {

    @XmlElement(required = true)
    protected ArrayOfTvNextClip getClipsByRessortIdResult;

    /**
     * Gets the value of the getClipsByRessortIdResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfTvNextClip }
     *     
     */
    public ArrayOfTvNextClip getGetClipsByRessortIdResult() {
        return getClipsByRessortIdResult;
    }

    /**
     * Sets the value of the getClipsByRessortIdResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfTvNextClip }
     *     
     */
    public void setGetClipsByRessortIdResult(ArrayOfTvNextClip value) {
        this.getClipsByRessortIdResult = value;
    }

}
