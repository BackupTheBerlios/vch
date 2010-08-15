
package de.berlios.vch.parser.n24.tvnext;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Exo_CMS_ImageBinaryFilePeer complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Exo_CMS_ImageBinaryFilePeer">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="instances" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Exo_CMS_ImageBinaryFilePeer", propOrder = {
    "instances"
})
public class ExoCMSImageBinaryFilePeer {

    @XmlElementRef(name = "instances", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> instances;

    /**
     * Gets the value of the instances property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getInstances() {
        return instances;
    }

    /**
     * Sets the value of the instances property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setInstances(JAXBElement<String> value) {
        this.instances = ((JAXBElement<String> ) value);
    }

}
