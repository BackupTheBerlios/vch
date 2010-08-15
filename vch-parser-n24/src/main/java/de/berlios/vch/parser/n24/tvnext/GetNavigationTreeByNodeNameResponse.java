
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
 *         &lt;element name="getNavigationTreeByNodeNameResult" type="{http://schemas.exozet.com/tvnext/services/core/}ArrayOfN24_NavigationStructure"/>
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
    "getNavigationTreeByNodeNameResult"
})
@XmlRootElement(name = "getNavigationTreeByNodeNameResponse")
public class GetNavigationTreeByNodeNameResponse {

    @XmlElement(required = true)
    protected ArrayOfN24NavigationStructure getNavigationTreeByNodeNameResult;

    /**
     * Gets the value of the getNavigationTreeByNodeNameResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfN24NavigationStructure }
     *     
     */
    public ArrayOfN24NavigationStructure getGetNavigationTreeByNodeNameResult() {
        return getNavigationTreeByNodeNameResult;
    }

    /**
     * Sets the value of the getNavigationTreeByNodeNameResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfN24NavigationStructure }
     *     
     */
    public void setGetNavigationTreeByNodeNameResult(ArrayOfN24NavigationStructure value) {
        this.getNavigationTreeByNodeNameResult = value;
    }

}
