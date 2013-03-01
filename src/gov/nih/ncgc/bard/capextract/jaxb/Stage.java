//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.03.01 at 12:02:20 PM EST 
//


package gov.nih.ncgc.bard.capextract.jaxb;

import javax.xml.bind.annotation.*;


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
 *         &lt;element name="stageName">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="128"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element ref="{}description" minOccurs="0"/>
 *         &lt;element ref="{}link" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="stageElement" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="parentStageName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "stageName",
    "description",
    "link"
})
@XmlRootElement(name = "stage")
public class Stage {

    @XmlElement(required = true)
    protected String stageName;
    @XmlElement(nillable = true)
    protected String description;
    protected Link link;
    @XmlAttribute(name = "stageElement", required = true)
    protected String stageElement;
    @XmlAttribute(name = "parentStageName")
    protected String parentStageName;

    /**
     * Gets the value of the stageName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStageName() {
        return stageName;
    }

    /**
     * Sets the value of the stageName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStageName(String value) {
        this.stageName = value;
    }

    /**
     * 
     *                         
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the link property.
     * 
     * @return
     *     possible object is
     *     {@link Link }
     *     
     */
    public Link getLink() {
        return link;
    }

    /**
     * Sets the value of the link property.
     * 
     * @param value
     *     allowed object is
     *     {@link Link }
     *     
     */
    public void setLink(Link value) {
        this.link = value;
    }

    /**
     * Gets the value of the stageElement property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStageElement() {
        return stageElement;
    }

    /**
     * Sets the value of the stageElement property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStageElement(String value) {
        this.stageElement = value;
    }

    /**
     * Gets the value of the parentStageName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getParentStageName() {
        return parentStageName;
    }

    /**
     * Sets the value of the parentStageName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setParentStageName(String value) {
        this.parentStageName = value;
    }

}
