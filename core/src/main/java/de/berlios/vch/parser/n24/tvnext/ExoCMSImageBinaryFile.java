
package de.berlios.vch.parser.n24.tvnext;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Exo_CMS_ImageBinaryFile complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Exo_CMS_ImageBinaryFile">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="peer" type="{http://schemas.exozet.com/tvnext/services/core/}Exo_CMS_ImageBinaryFilePeer" minOccurs="0"/>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="filename" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="mimetype" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="filesize" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="guid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="width" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="height" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="colorspace" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="type_cleartext" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="alreadyInSave" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="alreadyInValidation" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="validationFailures" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="modifiedColumns" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Exo_CMS_ImageBinaryFile", propOrder = {
    "peer",
    "id",
    "filename",
    "mimetype",
    "filesize",
    "guid",
    "width",
    "height",
    "colorspace",
    "typeCleartext",
    "alreadyInSave",
    "alreadyInValidation",
    "validationFailures",
    "modifiedColumns"
})
public class ExoCMSImageBinaryFile {

    @XmlElementRef(name = "peer", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<ExoCMSImageBinaryFilePeer> peer;
    @XmlElementRef(name = "id", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> id;
    @XmlElementRef(name = "filename", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> filename;
    @XmlElementRef(name = "mimetype", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> mimetype;
    @XmlElementRef(name = "filesize", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> filesize;
    @XmlElementRef(name = "guid", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> guid;
    @XmlElementRef(name = "width", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> width;
    @XmlElementRef(name = "height", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> height;
    @XmlElementRef(name = "colorspace", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> colorspace;
    @XmlElementRef(name = "type_cleartext", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> typeCleartext;
    @XmlElementRef(name = "alreadyInSave", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Boolean> alreadyInSave;
    @XmlElementRef(name = "alreadyInValidation", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Boolean> alreadyInValidation;
    @XmlElementRef(name = "validationFailures", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> validationFailures;
    @XmlElementRef(name = "modifiedColumns", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> modifiedColumns;

    /**
     * Gets the value of the peer property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ExoCMSImageBinaryFilePeer }{@code >}
     *     
     */
    public JAXBElement<ExoCMSImageBinaryFilePeer> getPeer() {
        return peer;
    }

    /**
     * Sets the value of the peer property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ExoCMSImageBinaryFilePeer }{@code >}
     *     
     */
    public void setPeer(JAXBElement<ExoCMSImageBinaryFilePeer> value) {
        this.peer = ((JAXBElement<ExoCMSImageBinaryFilePeer> ) value);
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setId(JAXBElement<Integer> value) {
        this.id = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the filename property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFilename() {
        return filename;
    }

    /**
     * Sets the value of the filename property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFilename(JAXBElement<String> value) {
        this.filename = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the mimetype property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getMimetype() {
        return mimetype;
    }

    /**
     * Sets the value of the mimetype property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setMimetype(JAXBElement<String> value) {
        this.mimetype = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the filesize property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getFilesize() {
        return filesize;
    }

    /**
     * Sets the value of the filesize property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setFilesize(JAXBElement<Integer> value) {
        this.filesize = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the guid property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getGuid() {
        return guid;
    }

    /**
     * Sets the value of the guid property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setGuid(JAXBElement<String> value) {
        this.guid = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the width property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getWidth() {
        return width;
    }

    /**
     * Sets the value of the width property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setWidth(JAXBElement<Integer> value) {
        this.width = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the height property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getHeight() {
        return height;
    }

    /**
     * Sets the value of the height property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setHeight(JAXBElement<Integer> value) {
        this.height = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the colorspace property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getColorspace() {
        return colorspace;
    }

    /**
     * Sets the value of the colorspace property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setColorspace(JAXBElement<String> value) {
        this.colorspace = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the typeCleartext property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTypeCleartext() {
        return typeCleartext;
    }

    /**
     * Sets the value of the typeCleartext property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTypeCleartext(JAXBElement<String> value) {
        this.typeCleartext = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the alreadyInSave property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getAlreadyInSave() {
        return alreadyInSave;
    }

    /**
     * Sets the value of the alreadyInSave property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setAlreadyInSave(JAXBElement<Boolean> value) {
        this.alreadyInSave = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the alreadyInValidation property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getAlreadyInValidation() {
        return alreadyInValidation;
    }

    /**
     * Sets the value of the alreadyInValidation property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setAlreadyInValidation(JAXBElement<Boolean> value) {
        this.alreadyInValidation = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the validationFailures property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getValidationFailures() {
        return validationFailures;
    }

    /**
     * Sets the value of the validationFailures property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setValidationFailures(JAXBElement<String> value) {
        this.validationFailures = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the modifiedColumns property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getModifiedColumns() {
        return modifiedColumns;
    }

    /**
     * Sets the value of the modifiedColumns property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setModifiedColumns(JAXBElement<String> value) {
        this.modifiedColumns = ((JAXBElement<String> ) value);
    }

}
