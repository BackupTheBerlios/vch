
package de.berlios.vch.parser.n24.tvnext;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TvNext_Clip complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TvNext_Clip">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="clip_status_label" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="clip_type_label" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="streamPath" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="flv_duration" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="modified_timestamp" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="created_timestamp" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="header" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="title" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="subtitle" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="preview_offset" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="preview_duration" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="display_preroll" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="cleanfeed" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="source" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="download_podcast" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="aExo_CMS_ImageBinaryFileRelatedBySource" type="{http://schemas.exozet.com/tvnext/services/core/}Exo_CMS_ImageBinaryFile" minOccurs="0"/>
 *         &lt;element name="ressortIds" type="{http://schemas.exozet.com/tvnext/services/core/}ArrayOfint" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TvNext_Clip", propOrder = {
    "clipStatusLabel",
    "clipTypeLabel",
    "streamPath",
    "flvDuration",
    "id",
    "modifiedTimestamp",
    "createdTimestamp",
    "header",
    "title",
    "subtitle",
    "previewOffset",
    "previewDuration",
    "displayPreroll",
    "cleanfeed",
    "source",
    "downloadPodcast",
    "aExoCMSImageBinaryFileRelatedBySource",
    "ressortIds"
})
public class TvNextClip {

    @XmlElementRef(name = "clip_status_label", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> clipStatusLabel;
    @XmlElementRef(name = "clip_type_label", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> clipTypeLabel;
    @XmlElementRef(name = "streamPath", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> streamPath;
    @XmlElementRef(name = "flv_duration", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> flvDuration;
    @XmlElementRef(name = "id", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> id;
    @XmlElementRef(name = "modified_timestamp", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> modifiedTimestamp;
    @XmlElementRef(name = "created_timestamp", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> createdTimestamp;
    @XmlElementRef(name = "header", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> header;
    @XmlElementRef(name = "title", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> title;
    @XmlElementRef(name = "subtitle", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> subtitle;
    @XmlElementRef(name = "preview_offset", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Double> previewOffset;
    @XmlElementRef(name = "preview_duration", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Double> previewDuration;
    @XmlElementRef(name = "display_preroll", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Boolean> displayPreroll;
    @XmlElementRef(name = "cleanfeed", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Boolean> cleanfeed;
    @XmlElementRef(name = "source", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> source;
    @XmlElementRef(name = "download_podcast", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Boolean> downloadPodcast;
    @XmlElementRef(name = "aExo_CMS_ImageBinaryFileRelatedBySource", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<ExoCMSImageBinaryFile> aExoCMSImageBinaryFileRelatedBySource;
    @XmlElementRef(name = "ressortIds", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<ArrayOfint> ressortIds;

    /**
     * Gets the value of the clipStatusLabel property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getClipStatusLabel() {
        return clipStatusLabel;
    }

    /**
     * Sets the value of the clipStatusLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setClipStatusLabel(JAXBElement<String> value) {
        this.clipStatusLabel = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the clipTypeLabel property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getClipTypeLabel() {
        return clipTypeLabel;
    }

    /**
     * Sets the value of the clipTypeLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setClipTypeLabel(JAXBElement<String> value) {
        this.clipTypeLabel = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the streamPath property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getStreamPath() {
        return streamPath;
    }

    /**
     * Sets the value of the streamPath property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setStreamPath(JAXBElement<String> value) {
        this.streamPath = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the flvDuration property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getFlvDuration() {
        return flvDuration;
    }

    /**
     * Sets the value of the flvDuration property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setFlvDuration(JAXBElement<Integer> value) {
        this.flvDuration = ((JAXBElement<Integer> ) value);
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
     * Gets the value of the modifiedTimestamp property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    /**
     * Sets the value of the modifiedTimestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setModifiedTimestamp(JAXBElement<String> value) {
        this.modifiedTimestamp = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the createdTimestamp property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Sets the value of the createdTimestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setCreatedTimestamp(JAXBElement<String> value) {
        this.createdTimestamp = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the header property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setHeader(JAXBElement<String> value) {
        this.header = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTitle(JAXBElement<String> value) {
        this.title = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the subtitle property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSubtitle() {
        return subtitle;
    }

    /**
     * Sets the value of the subtitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSubtitle(JAXBElement<String> value) {
        this.subtitle = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the previewOffset property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public JAXBElement<Double> getPreviewOffset() {
        return previewOffset;
    }

    /**
     * Sets the value of the previewOffset property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public void setPreviewOffset(JAXBElement<Double> value) {
        this.previewOffset = ((JAXBElement<Double> ) value);
    }

    /**
     * Gets the value of the previewDuration property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public JAXBElement<Double> getPreviewDuration() {
        return previewDuration;
    }

    /**
     * Sets the value of the previewDuration property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public void setPreviewDuration(JAXBElement<Double> value) {
        this.previewDuration = ((JAXBElement<Double> ) value);
    }

    /**
     * Gets the value of the displayPreroll property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getDisplayPreroll() {
        return displayPreroll;
    }

    /**
     * Sets the value of the displayPreroll property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setDisplayPreroll(JAXBElement<Boolean> value) {
        this.displayPreroll = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the cleanfeed property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getCleanfeed() {
        return cleanfeed;
    }

    /**
     * Sets the value of the cleanfeed property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setCleanfeed(JAXBElement<Boolean> value) {
        this.cleanfeed = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the source property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setSource(JAXBElement<Integer> value) {
        this.source = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the downloadPodcast property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getDownloadPodcast() {
        return downloadPodcast;
    }

    /**
     * Sets the value of the downloadPodcast property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setDownloadPodcast(JAXBElement<Boolean> value) {
        this.downloadPodcast = ((JAXBElement<Boolean> ) value);
    }

    /**
     * Gets the value of the aExoCMSImageBinaryFileRelatedBySource property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ExoCMSImageBinaryFile }{@code >}
     *     
     */
    public JAXBElement<ExoCMSImageBinaryFile> getAExoCMSImageBinaryFileRelatedBySource() {
        return aExoCMSImageBinaryFileRelatedBySource;
    }

    /**
     * Sets the value of the aExoCMSImageBinaryFileRelatedBySource property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ExoCMSImageBinaryFile }{@code >}
     *     
     */
    public void setAExoCMSImageBinaryFileRelatedBySource(JAXBElement<ExoCMSImageBinaryFile> value) {
        this.aExoCMSImageBinaryFileRelatedBySource = ((JAXBElement<ExoCMSImageBinaryFile> ) value);
    }

    /**
     * Gets the value of the ressortIds property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfint }{@code >}
     *     
     */
    public JAXBElement<ArrayOfint> getRessortIds() {
        return ressortIds;
    }

    /**
     * Sets the value of the ressortIds property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfint }{@code >}
     *     
     */
    public void setRessortIds(JAXBElement<ArrayOfint> value) {
        this.ressortIds = ((JAXBElement<ArrayOfint> ) value);
    }

}
