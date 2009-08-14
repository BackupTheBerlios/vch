
package de.berlios.vch.parser.n24.tvnext;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfN24_NavigationStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfN24_NavigationStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="N24_NavigationStructure" type="{http://schemas.exozet.com/tvnext/services/core/}N24_NavigationStructure" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfN24_NavigationStructure", propOrder = {
    "n24NavigationStructure"
})
public class ArrayOfN24NavigationStructure {

    @XmlElement(name = "N24_NavigationStructure", nillable = true)
    protected List<N24NavigationStructure> n24NavigationStructure;

    /**
     * Gets the value of the n24NavigationStructure property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the n24NavigationStructure property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getN24NavigationStructure().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link N24NavigationStructure }
     * 
     * 
     */
    public List<N24NavigationStructure> getN24NavigationStructure() {
        if (n24NavigationStructure == null) {
            n24NavigationStructure = new ArrayList<N24NavigationStructure>();
        }
        return this.n24NavigationStructure;
    }

}
