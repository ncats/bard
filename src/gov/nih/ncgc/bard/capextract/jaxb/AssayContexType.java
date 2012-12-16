//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.15 at 04:20:25 PM EST 
//


package gov.nih.ncgc.bard.capextract.jaxb;

import javax.xml.bind.annotation.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for assayContexType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="assayContexType">
 *   &lt;complexContent>
 *     &lt;extension base="{}abstractContextType">
 *       &lt;sequence>
 *         &lt;element ref="{}assayContextItems" minOccurs="0"/>
 *         &lt;element name="measureRefs" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="measureRef" type="{}bardID" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="assayContextId" use="required" type="{}bardID" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "assayContexType", propOrder = {
    "assayContextItems",
    "measureRefs"
})
public class AssayContexType
    extends AbstractContextType
{

    protected AssayContextItems assayContextItems;
    protected AssayContexType.MeasureRefs measureRefs;
    @XmlAttribute(name = "assayContextId", required = true)
    protected BigInteger assayContextId;

    /**
     * Gets the value of the assayContextItems property.
     * 
     * @return
     *     possible object is
     *     {@link AssayContextItems }
     *     
     */
    public AssayContextItems getAssayContextItems() {
        return assayContextItems;
    }

    /**
     * Sets the value of the assayContextItems property.
     * 
     * @param value
     *     allowed object is
     *     {@link AssayContextItems }
     *     
     */
    public void setAssayContextItems(AssayContextItems value) {
        this.assayContextItems = value;
    }

    /**
     * Gets the value of the measureRefs property.
     * 
     * @return
     *     possible object is
     *     {@link AssayContexType.MeasureRefs }
     *     
     */
    public AssayContexType.MeasureRefs getMeasureRefs() {
        return measureRefs;
    }

    /**
     * Sets the value of the measureRefs property.
     * 
     * @param value
     *     allowed object is
     *     {@link AssayContexType.MeasureRefs }
     *     
     */
    public void setMeasureRefs(AssayContexType.MeasureRefs value) {
        this.measureRefs = value;
    }

    /**
     * Gets the value of the assayContextId property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getAssayContextId() {
        return assayContextId;
    }

    /**
     * Sets the value of the assayContextId property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setAssayContextId(BigInteger value) {
        this.assayContextId = value;
    }


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
     *         &lt;element name="measureRef" type="{}bardID" maxOccurs="unbounded"/>
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
        "measureRef"
    })
    public static class MeasureRefs {

        @XmlElement(required = true)
        protected List<BigInteger> measureRef;

        /**
         * Gets the value of the measureRef property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the measureRef property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getMeasureRef().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link BigInteger }
         * 
         * 
         */
        public List<BigInteger> getMeasureRef() {
            if (measureRef == null) {
                measureRef = new ArrayList<BigInteger>();
            }
            return this.measureRef;
        }

    }

}
