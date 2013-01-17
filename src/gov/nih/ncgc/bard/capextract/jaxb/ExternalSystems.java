//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.15 at 04:20:25 PM EST 
//


package gov.nih.ncgc.bard.capextract.jaxb;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


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
 *         &lt;element name="externalSystem" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="systemUrl" type="{http://www.w3.org/2001/XMLSchema}anyURI"/>
 *                   &lt;element ref="{}link"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="owner" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="count" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "externalSystem"
})
@XmlRootElement(name = "externalSystems")
public class ExternalSystems {

    @XmlElement(required = true)
    protected List<ExternalSystems.ExternalSystem> externalSystem;
    @XmlAttribute(name = "count", required = true)
    protected int count;

    /**
     * Gets the value of the externalSystem property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the externalSystem property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExternalSystem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ExternalSystems.ExternalSystem }
     * 
     * 
     */
    public List<ExternalSystems.ExternalSystem> getExternalSystem() {
        if (externalSystem == null) {
            externalSystem = new ArrayList<ExternalSystems.ExternalSystem>();
        }
        return this.externalSystem;
    }

    /**
     * Gets the value of the count property.
     * 
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the value of the count property.
     * 
     */
    public void setCount(int value) {
        this.count = value;
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
     *         &lt;element name="systemUrl" type="{http://www.w3.org/2001/XMLSchema}anyURI"/>
     *         &lt;element ref="{}link"/>
     *       &lt;/sequence>
     *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="owner" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "systemUrl",
        "link"
    })
    @XmlRootElement(name = "externalSystem")
    public static class ExternalSystem {

        @XmlElement(required = true)
        @XmlSchemaType(name = "anyURI")
        protected String systemUrl;
        @XmlElement(required = true)
        protected Link link;
        @XmlAttribute(name = "name", required = true)
        protected String name;
        @XmlAttribute(name = "owner")
        protected String owner;

        /**
         * Gets the value of the systemUrl property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getSystemUrl() {
            return systemUrl;
        }

        /**
         * Sets the value of the systemUrl property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setSystemUrl(String value) {
            this.systemUrl = value;
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
         * Gets the value of the name property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the value of the name property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Gets the value of the owner property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getOwner() {
            return owner;
        }

        /**
         * Sets the value of the owner property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setOwner(String value) {
            this.owner = value;
        }

    }

}
