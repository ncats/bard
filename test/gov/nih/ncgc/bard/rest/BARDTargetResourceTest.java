package gov.nih.ncgc.bard.rest;

import org.testng.annotations.BeforeClass;

/**
 * A test of the target resource.
 *
 * @author Rajarshi Guha
 */
public class BARDTargetResourceTest extends EntityResourceTest {

    public BARDTargetResourceTest(String resourceName) {
        super(resourceName);
    }

    @BeforeClass
    public void beforeclass() {
        setResourceName("targets");
    }

}
