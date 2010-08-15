
package de.berlios.vch.parser.n24.tvnext;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for N24_NavigationStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="N24_NavigationStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="navigation_type_label" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="url_target_label" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="video_id" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="livedate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="killdate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="wakeuptime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sleeptime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="created_timestamp" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="modified_timestamp" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="state" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="dataset_name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="clickcounter_counter" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="clickcounter_correction" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="clickcounter_corrected" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="keywords" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="copyright" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="cr_source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="label" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="url" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="set_left" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="set_right" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="set_level" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="livestream_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="agofid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="soi_subsite" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="soi_subsite2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="soi_subsite3" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "N24_NavigationStructure", propOrder = {
    "navigationTypeLabel",
    "urlTargetLabel",
    "videoId",
    "id",
    "livedate",
    "killdate",
    "wakeuptime",
    "sleeptime",
    "createdTimestamp",
    "modifiedTimestamp",
    "state",
    "datasetName",
    "clickcounterCounter",
    "clickcounterCorrection",
    "clickcounterCorrected",
    "keywords",
    "copyright",
    "crSource",
    "label",
    "url",
    "setLeft",
    "setRight",
    "setLevel",
    "livestreamId",
    "agofid",
    "soiSubsite",
    "soiSubsite2",
    "soiSubsite3"
})
public class N24NavigationStructure {

    @XmlElementRef(name = "navigation_type_label", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> navigationTypeLabel;
    @XmlElementRef(name = "url_target_label", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> urlTargetLabel;
    @XmlElementRef(name = "video_id", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> videoId;
    @XmlElementRef(name = "id", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> id;
    @XmlElementRef(name = "livedate", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> livedate;
    @XmlElementRef(name = "killdate", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> killdate;
    @XmlElementRef(name = "wakeuptime", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> wakeuptime;
    @XmlElementRef(name = "sleeptime", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> sleeptime;
    @XmlElementRef(name = "created_timestamp", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> createdTimestamp;
    @XmlElementRef(name = "modified_timestamp", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> modifiedTimestamp;
    @XmlElementRef(name = "state", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> state;
    @XmlElementRef(name = "dataset_name", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> datasetName;
    @XmlElementRef(name = "clickcounter_counter", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> clickcounterCounter;
    @XmlElementRef(name = "clickcounter_correction", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> clickcounterCorrection;
    @XmlElementRef(name = "clickcounter_corrected", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> clickcounterCorrected;
    @XmlElementRef(name = "keywords", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> keywords;
    @XmlElementRef(name = "copyright", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> copyright;
    @XmlElementRef(name = "cr_source", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> crSource;
    @XmlElementRef(name = "label", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> label;
    @XmlElementRef(name = "url", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> url;
    @XmlElementRef(name = "set_left", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> setLeft;
    @XmlElementRef(name = "set_right", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> setRight;
    @XmlElementRef(name = "set_level", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<Integer> setLevel;
    @XmlElementRef(name = "livestream_id", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> livestreamId;
    @XmlElementRef(name = "agofid", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> agofid;
    @XmlElementRef(name = "soi_subsite", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> soiSubsite;
    @XmlElementRef(name = "soi_subsite2", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> soiSubsite2;
    @XmlElementRef(name = "soi_subsite3", namespace = "http://schemas.exozet.com/tvnext/services/core/", type = JAXBElement.class)
    protected JAXBElement<String> soiSubsite3;

    /**
     * Gets the value of the navigationTypeLabel property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getNavigationTypeLabel() {
        return navigationTypeLabel;
    }

    /**
     * Sets the value of the navigationTypeLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setNavigationTypeLabel(JAXBElement<String> value) {
        this.navigationTypeLabel = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the urlTargetLabel property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getUrlTargetLabel() {
        return urlTargetLabel;
    }

    /**
     * Sets the value of the urlTargetLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setUrlTargetLabel(JAXBElement<String> value) {
        this.urlTargetLabel = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the videoId property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getVideoId() {
        return videoId;
    }

    /**
     * Sets the value of the videoId property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setVideoId(JAXBElement<Integer> value) {
        this.videoId = ((JAXBElement<Integer> ) value);
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
     * Gets the value of the livedate property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getLivedate() {
        return livedate;
    }

    /**
     * Sets the value of the livedate property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setLivedate(JAXBElement<String> value) {
        this.livedate = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the killdate property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getKilldate() {
        return killdate;
    }

    /**
     * Sets the value of the killdate property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setKilldate(JAXBElement<String> value) {
        this.killdate = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the wakeuptime property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getWakeuptime() {
        return wakeuptime;
    }

    /**
     * Sets the value of the wakeuptime property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setWakeuptime(JAXBElement<String> value) {
        this.wakeuptime = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the sleeptime property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSleeptime() {
        return sleeptime;
    }

    /**
     * Sets the value of the sleeptime property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSleeptime(JAXBElement<String> value) {
        this.sleeptime = ((JAXBElement<String> ) value);
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
     * Gets the value of the state property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setState(JAXBElement<Integer> value) {
        this.state = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the datasetName property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getDatasetName() {
        return datasetName;
    }

    /**
     * Sets the value of the datasetName property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setDatasetName(JAXBElement<String> value) {
        this.datasetName = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the clickcounterCounter property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getClickcounterCounter() {
        return clickcounterCounter;
    }

    /**
     * Sets the value of the clickcounterCounter property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setClickcounterCounter(JAXBElement<Integer> value) {
        this.clickcounterCounter = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the clickcounterCorrection property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getClickcounterCorrection() {
        return clickcounterCorrection;
    }

    /**
     * Sets the value of the clickcounterCorrection property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setClickcounterCorrection(JAXBElement<Integer> value) {
        this.clickcounterCorrection = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the clickcounterCorrected property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getClickcounterCorrected() {
        return clickcounterCorrected;
    }

    /**
     * Sets the value of the clickcounterCorrected property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setClickcounterCorrected(JAXBElement<Integer> value) {
        this.clickcounterCorrected = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the keywords property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getKeywords() {
        return keywords;
    }

    /**
     * Sets the value of the keywords property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setKeywords(JAXBElement<String> value) {
        this.keywords = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the copyright property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getCopyright() {
        return copyright;
    }

    /**
     * Sets the value of the copyright property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setCopyright(JAXBElement<String> value) {
        this.copyright = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the crSource property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getCrSource() {
        return crSource;
    }

    /**
     * Sets the value of the crSource property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setCrSource(JAXBElement<String> value) {
        this.crSource = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setLabel(JAXBElement<String> value) {
        this.label = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the url property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setUrl(JAXBElement<String> value) {
        this.url = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the setLeft property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getSetLeft() {
        return setLeft;
    }

    /**
     * Sets the value of the setLeft property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setSetLeft(JAXBElement<Integer> value) {
        this.setLeft = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the setRight property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getSetRight() {
        return setRight;
    }

    /**
     * Sets the value of the setRight property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setSetRight(JAXBElement<Integer> value) {
        this.setRight = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the setLevel property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getSetLevel() {
        return setLevel;
    }

    /**
     * Sets the value of the setLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setSetLevel(JAXBElement<Integer> value) {
        this.setLevel = ((JAXBElement<Integer> ) value);
    }

    /**
     * Gets the value of the livestreamId property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getLivestreamId() {
        return livestreamId;
    }

    /**
     * Sets the value of the livestreamId property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setLivestreamId(JAXBElement<String> value) {
        this.livestreamId = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the agofid property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getAgofid() {
        return agofid;
    }

    /**
     * Sets the value of the agofid property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setAgofid(JAXBElement<String> value) {
        this.agofid = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the soiSubsite property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSoiSubsite() {
        return soiSubsite;
    }

    /**
     * Sets the value of the soiSubsite property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSoiSubsite(JAXBElement<String> value) {
        this.soiSubsite = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the soiSubsite2 property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSoiSubsite2() {
        return soiSubsite2;
    }

    /**
     * Sets the value of the soiSubsite2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSoiSubsite2(JAXBElement<String> value) {
        this.soiSubsite2 = ((JAXBElement<String> ) value);
    }

    /**
     * Gets the value of the soiSubsite3 property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSoiSubsite3() {
        return soiSubsite3;
    }

    /**
     * Sets the value of the soiSubsite3 property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSoiSubsite3(JAXBElement<String> value) {
        this.soiSubsite3 = ((JAXBElement<String> ) value);
    }

}
