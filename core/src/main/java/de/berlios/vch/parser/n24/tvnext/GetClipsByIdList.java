
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
 *         &lt;element name="idList" type="{http://schemas.exozet.com/tvnext/services/core/}ArrayOfint" minOccurs="0"/>
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
    "idList"
})
@XmlRootElement(name = "getClipsByIdList")
public class GetClipsByIdList {

    protected ArrayOfint idList;

    /**
     * Gets the value of the idList property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfint }
     *     
     */
    public ArrayOfint getIdList() {
        return idList;
    }

    /**
     * Sets the value of the idList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfint }
     *     
     */
    public void setIdList(ArrayOfint value) {
        this.idList = value;
    }

}
