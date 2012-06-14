package gov.nih.ncgc.bard.rest;

import org.testng.annotations.BeforeClass;

/**
 * A test of the assay resource.
 *
 * @author Rajarshi Guha
 */
public class BARDAssayResourceTest extends EntityResourceTest {

    public BARDAssayResourceTest(String resourceName) {
        super(resourceName);
    }

    @BeforeClass
    public void beforeclass() {
        setResourceName("assays");
    }

}
