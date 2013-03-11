//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.03.01 at 12:02:20 PM EST 
//


package gov.nih.ncgc.bard.capextract.jaxb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
 *         &lt;element name="resultContextItem" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="attribute" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element ref="{}link" minOccurs="0"/>
 *                           &lt;/sequence>
 *                           &lt;attribute name="label" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                   &lt;element name="valueControlled" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element ref="{}link" minOccurs="0"/>
 *                           &lt;/sequence>
 *                           &lt;attribute name="label" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                   &lt;element name="extValueId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="resultContextItemId" use="required" type="{}bardID" />
 *                 &lt;attribute name="parentGroup" type="{}bardID" />
 *                 &lt;attribute name="qualifier" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="valueDisplay" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="valueNum" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                 &lt;attribute name="valueMin" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                 &lt;attribute name="valueMax" type="{http://www.w3.org/2001/XMLSchema}double" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
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
    "resultContextItem"
})
@XmlRootElement(name = "resultContextItems")
public class ResultContextItems {

    @XmlElement(required = true)
    protected List<ResultContextItems.ResultContextItem> resultContextItem;

    /**
     * Gets the value of the resultContextItem property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the resultContextItem property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getResultContextItem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ResultContextItems.ResultContextItem }
     * 
     * 
     */
    public List<ResultContextItems.ResultContextItem> getResultContextItem() {
        if (resultContextItem == null) {
            resultContextItem = new ArrayList<ResultContextItems.ResultContextItem>();
        }
        return this.resultContextItem;
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
     *         &lt;element name="attribute" minOccurs="0">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element ref="{}link" minOccurs="0"/>
     *                 &lt;/sequence>
     *                 &lt;attribute name="label" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *         &lt;element name="valueControlled" minOccurs="0">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element ref="{}link" minOccurs="0"/>
     *                 &lt;/sequence>
     *                 &lt;attribute name="label" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *         &lt;element name="extValueId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     *       &lt;/sequence>
     *       &lt;attribute name="resultContextItemId" use="required" type="{}bardID" />
     *       &lt;attribute name="parentGroup" type="{}bardID" />
     *       &lt;attribute name="qualifier" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="valueDisplay" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="valueNum" type="{http://www.w3.org/2001/XMLSchema}double" />
     *       &lt;attribute name="valueMin" type="{http://www.w3.org/2001/XMLSchema}double" />
     *       &lt;attribute name="valueMax" type="{http://www.w3.org/2001/XMLSchema}double" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "attribute",
        "valueControlled",
        "extValueId"
    })
    public static class ResultContextItem {

        protected ResultContextItems.ResultContextItem.Attribute attribute;
        protected ResultContextItems.ResultContextItem.ValueControlled valueControlled;
        protected String extValueId;
        @XmlAttribute(name = "resultContextItemId", required = true)
        protected BigInteger resultContextItemId;
        @XmlAttribute(name = "parentGroup")
        protected BigInteger parentGroup;
        @XmlAttribute(name = "qualifier")
        protected String qualifier;
        @XmlAttribute(name = "valueDisplay")
        protected String valueDisplay;
        @XmlAttribute(name = "valueNum")
        protected Double valueNum;
        @XmlAttribute(name = "valueMin")
        protected Double valueMin;
        @XmlAttribute(name = "valueMax")
        protected Double valueMax;

        /**
         * Gets the value of the attribute property.
         * 
         * @return
         *     possible object is
         *     {@link ResultContextItems.ResultContextItem.Attribute }
         *     
         */
        public ResultContextItems.ResultContextItem.Attribute getAttribute() {
            return attribute;
        }

        /**
         * Sets the value of the attribute property.
         * 
         * @param value
         *     allowed object is
         *     {@link ResultContextItems.ResultContextItem.Attribute }
         *     
         */
        public void setAttribute(ResultContextItems.ResultContextItem.Attribute value) {
            this.attribute = value;
        }

        /**
         * Gets the value of the valueControlled property.
         * 
         * @return
         *     possible object is
         *     {@link ResultContextItems.ResultContextItem.ValueControlled }
         *     
         */
        public ResultContextItems.ResultContextItem.ValueControlled getValueControlled() {
            return valueControlled;
        }

        /**
         * Sets the value of the valueControlled property.
         * 
         * @param value
         *     allowed object is
         *     {@link ResultContextItems.ResultContextItem.ValueControlled }
         *     
         */
        public void setValueControlled(ResultContextItems.ResultContextItem.ValueControlled value) {
            this.valueControlled = value;
        }

        /**
         * Gets the value of the extValueId property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getExtValueId() {
            return extValueId;
        }

        /**
         * Sets the value of the extValueId property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setExtValueId(String value) {
            this.extValueId = value;
        }

        /**
         * Gets the value of the resultContextItemId property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getResultContextItemId() {
            return resultContextItemId;
        }

        /**
         * Sets the value of the resultContextItemId property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setResultContextItemId(BigInteger value) {
            this.resultContextItemId = value;
        }

        /**
         * Gets the value of the parentGroup property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getParentGroup() {
            return parentGroup;
        }

        /**
         * Sets the value of the parentGroup property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setParentGroup(BigInteger value) {
            this.parentGroup = value;
        }

        /**
         * Gets the value of the qualifier property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getQualifier() {
            return qualifier;
        }

        /**
         * Sets the value of the qualifier property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setQualifier(String value) {
            this.qualifier = value;
        }

        /**
         * Gets the value of the valueDisplay property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getValueDisplay() {
            return valueDisplay;
        }

        /**
         * Sets the value of the valueDisplay property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setValueDisplay(String value) {
            this.valueDisplay = value;
        }

        /**
         * Gets the value of the valueNum property.
         * 
         * @return
         *     possible object is
         *     {@link Double }
         *     
         */
        public Double getValueNum() {
            return valueNum;
        }

        /**
         * Sets the value of the valueNum property.
         * 
         * @param value
         *     allowed object is
         *     {@link Double }
         *     
         */
        public void setValueNum(Double value) {
            this.valueNum = value;
        }

        /**
         * Gets the value of the valueMin property.
         * 
         * @return
         *     possible object is
         *     {@link Double }
         *     
         */
        public Double getValueMin() {
            return valueMin;
        }

        /**
         * Sets the value of the valueMin property.
         * 
         * @param value
         *     allowed object is
         *     {@link Double }
         *     
         */
        public void setValueMin(Double value) {
            this.valueMin = value;
        }

        /**
         * Gets the value of the valueMax property.
         * 
         * @return
         *     possible object is
         *     {@link Double }
         *     
         */
        public Double getValueMax() {
            return valueMax;
        }

        /**
         * Sets the value of the valueMax property.
         * 
         * @param value
         *     allowed object is
         *     {@link Double }
         *     
         */
        public void setValueMax(Double value) {
            this.valueMax = value;
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
         *         &lt;element ref="{}link" minOccurs="0"/>
         *       &lt;/sequence>
         *       &lt;attribute name="label" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "link"
        })
        public static class Attribute {

            protected Link link;
            @XmlAttribute(name = "label", required = true)
            protected String label;

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
             * Gets the value of the label property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getLabel() {
                return label;
            }

            /**
             * Sets the value of the label property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setLabel(String value) {
                this.label = value;
            }

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
         *         &lt;element ref="{}link" minOccurs="0"/>
         *       &lt;/sequence>
         *       &lt;attribute name="label" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "link"
        })
        public static class ValueControlled {

            protected Link link;
            @XmlAttribute(name = "label", required = true)
            protected String label;

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
             * Gets the value of the label property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getLabel() {
                return label;
            }

            /**
             * Sets the value of the label property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setLabel(String value) {
                this.label = value;
            }

        }

    }

}
