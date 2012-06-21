package gov.nih.ncgc.bard.rest;

import org.testng.annotations.BeforeClass;

/**
 * A test of the experiments resource.
 *
 * @author Rajarshi Guha
 */
public class BARDExperimentResourceTest extends EntityResourceTest {

    public BARDExperimentResourceTest(String resourceName) {
        super(resourceName);
    }

    @BeforeClass
    public void beforeclass() {
        setResourceName("experiments");
    }

}
