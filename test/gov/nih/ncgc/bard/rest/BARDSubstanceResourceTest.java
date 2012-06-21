package gov.nih.ncgc.bard.rest;

import org.testng.annotations.BeforeClass;

/**
 * A test of the substance resource.
 *
 * @author Rajarshi Guha
 */
public class BARDSubstanceResourceTest extends EntityResourceTest {

    public BARDSubstanceResourceTest(String resourceName) {
        super(resourceName);
    }

    @BeforeClass
    public void beforeclass() {
        setResourceName("substances");
    }

}
