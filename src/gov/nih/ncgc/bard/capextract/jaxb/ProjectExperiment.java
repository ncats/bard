//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.15 at 04:20:25 PM EST 
//


package gov.nih.ncgc.bard.capextract.jaxb;

import javax.xml.bind.annotation.*;
import java.math.BigInteger;


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
 *         &lt;element name="experimentRef">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{}link"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="label" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="stageRef" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{}link"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="label" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{}contexts" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="projectExperimentId" use="required" type="{}bardID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "experimentRef",
    "stageRef",
    "contexts"
})
@XmlRootElement(name = "projectExperiment")
public class ProjectExperiment {

    @XmlElement(required = true)
    protected ProjectExperiment.ExperimentRef experimentRef;
    protected ProjectExperiment.StageRef stageRef;
    protected Contexts contexts;
    @XmlAttribute(name = "projectExperimentId", required = true)
    protected BigInteger projectExperimentId;

    /**
     * Gets the value of the experimentRef property.
     * 
     * @return
     *     possible object is
     *     {@link ProjectExperiment.ExperimentRef }
     *     
     */
    public ProjectExperiment.ExperimentRef getExperimentRef() {
        return experimentRef;
    }

    /**
     * Sets the value of the experimentRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProjectExperiment.ExperimentRef }
     *     
     */
    public void setExperimentRef(ProjectExperiment.ExperimentRef value) {
        this.experimentRef = value;
    }

    /**
     * Gets the value of the stageRef property.
     * 
     * @return
     *     possible object is
     *     {@link ProjectExperiment.StageRef }
     *     
     */
    public ProjectExperiment.StageRef getStageRef() {
        return stageRef;
    }

    /**
     * Sets the value of the stageRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProjectExperiment.StageRef }
     *     
     */
    public void setStageRef(ProjectExperiment.StageRef value) {
        this.stageRef = value;
    }

    /**
     * Gets the value of the contexts property.
     * 
     * @return
     *     possible object is
     *     {@link Contexts }
     *     
     */
    public Contexts getContexts() {
        return contexts;
    }

    /**
     * Sets the value of the contexts property.
     * 
     * @param value
     *     allowed object is
     *     {@link Contexts }
     *     
     */
    public void setContexts(Contexts value) {
        this.contexts = value;
    }

    /**
     * Gets the value of the projectExperimentId property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getProjectExperimentId() {
        return projectExperimentId;
    }

    /**
     * Sets the value of the projectExperimentId property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setProjectExperimentId(BigInteger value) {
        this.projectExperimentId = value;
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
     *         &lt;element ref="{}link"/>
     *       &lt;/sequence>
     *       &lt;attribute name="label" type="{http://www.w3.org/2001/XMLSchema}string" />
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
    public static class ExperimentRef {

        @XmlElement(required = true)
        protected Link link;
        @XmlAttribute(name = "label")
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
     *         &lt;element ref="{}link"/>
     *       &lt;/sequence>
     *       &lt;attribute name="label" type="{http://www.w3.org/2001/XMLSchema}string" />
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
    public static class StageRef {

        @XmlElement(required = true)
        protected Link link;
        @XmlAttribute(name = "label")
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
