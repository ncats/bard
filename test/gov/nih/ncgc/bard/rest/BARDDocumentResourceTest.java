package gov.nih.ncgc.bard.rest;

import org.testng.annotations.BeforeClass;

/**
 * A test of the document resource.
 *
 * @author Rajarshi Guha
 */
public class BARDDocumentResourceTest extends EntityResourceTest {

    public BARDDocumentResourceTest(String resourceName) {
        super(resourceName);
    }

    @BeforeClass
    public void beforeclass() {
        setResourceName("documents");
    }

}
