package gov.nih.ncgc.bard.tools;

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
        CAPExtractor extractor = new CAPExtractor();
        extractor.run();
    }
}
