//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.15 at 04:20:25 PM EST 
//


package gov.nih.ncgc.bard.capextract.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.math.BigInteger;


/**
 * <p>Java class for experimentContextItemType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="experimentContextItemType">
 *   &lt;complexContent>
 *     &lt;extension base="{}abstractContextItemType">
 *       &lt;attribute name="experimentContextItemId" use="required" type="{}bardID" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "experimentContextItemType")
public class ExperimentContextItemType
    extends AbstractContextItemType
{

    @XmlAttribute(name = "experimentContextItemId", required = true)
    protected BigInteger experimentContextItemId;

    /**
     * Gets the value of the experimentContextItemId property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getExperimentContextItemId() {
        return experimentContextItemId;
    }

    /**
     * Sets the value of the experimentContextItemId property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setExperimentContextItemId(BigInteger value) {
        this.experimentContextItemId = value;
    }

}
