//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.14 at 05:13:24 AM EST 
//


package gov.nih.ncgc.bard.capextract.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
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
 *       &lt;attribute name="type" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=experiments"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=experiment"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=results"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=result"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=cap"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=resultType"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=project"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=assay"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=dictionary"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=bardexport"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=stage"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=element"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=projects"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=assayDoc"/>
 *             &lt;enumeration value="application/vnd.bard.cap+xml;type=assays"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="href" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="rel" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="related"/>
 *             &lt;enumeration value="edit"/>
 *             &lt;enumeration value="up"/>
 *             &lt;enumeration value="next"/>
 *             &lt;enumeration value="item"/>
 *             &lt;enumeration value="self"/>
 *             &lt;enumeration value="collection"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="title" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "link")
public class Link {

    @XmlAttribute(name = "type", required = true)
    protected String type;
    @XmlAttribute(name = "href", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String href;
    @XmlAttribute(name = "rel", required = true)
    protected String rel;
    @XmlAttribute(name = "title")
    protected String title;

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the href property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the value of the href property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHref(String value) {
        this.href = value;
    }

    /**
     * Gets the value of the rel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRel() {
        return rel;
    }

    /**
     * Sets the value of the rel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRel(String value) {
        this.rel = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

}
