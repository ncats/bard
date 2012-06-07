package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.jaxb.DescriptorType;
import gov.nih.ncgc.bard.capextract.jaxb.Dictionary;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;

/**
 * Example code to play with the Broad CAP Data Export API.
 *
 * @author Rajarshi Guha
 */
public class CAPExtractor {
    public static final String EXPORT_URL = "http://bard.nih.gov/bardexport";

    public CAPExtractor() {
    }

    public boolean run() {
        return true;
    }

    public static void main(String[] args) {
//        CAPExtractor extractor = new CAPExtractor();
//        extractor.run();

	try {
	    JAXBContext jc = JAXBContext.newInstance("gov.nih.ncgc.bard.capextract.jaxb");
	    Unmarshaller unmarshaller = jc.createUnmarshaller();
	    ValidationEventCollector vec = new ValidationEventCollector();
	    unmarshaller.setEventHandler(vec);
	    Dictionary dict = (Dictionary)unmarshaller.unmarshal(new File("resources/test/dictionary.xml"));
	
	    if (vec.hasEvents()) {
		for (ValidationEvent ve: vec.getEvents())
		    System.out.println(ve.getMessage());
	    }
	    
	    String yo = "";
	    for (DescriptorType item: dict.getAssayDescriptors().getAssayDescriptor()) yo = yo + item.getLabel() + "\n";
	    	System.out.println(yo);
	} catch (Exception ex) {ex.printStackTrace();}
    }
}
