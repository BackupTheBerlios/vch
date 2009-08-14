
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
 *         &lt;element name="getRessortsResult" type="{http://schemas.exozet.com/tvnext/services/core/}ArrayOfN24_Ressort"/>
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
    "getRessortsResult"
})
@XmlRootElement(name = "getRessortsResponse")
public class GetRessortsResponse {

    @XmlElement(required = true)
    protected ArrayOfN24Ressort getRessortsResult;

    /**
     * Gets the value of the getRessortsResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfN24Ressort }
     *     
     */
    public ArrayOfN24Ressort getGetRessortsResult() {
        return getRessortsResult;
    }

    /**
     * Sets the value of the getRessortsResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfN24Ressort }
     *     
     */
    public void setGetRessortsResult(ArrayOfN24Ressort value) {
        this.getRessortsResult = value;
    }

}
