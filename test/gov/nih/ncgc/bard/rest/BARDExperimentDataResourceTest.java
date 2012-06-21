package gov.nih.ncgc.bard.rest;

import org.testng.annotations.BeforeClass;

/**
 * A test of the experiment data resource.
 *
 * @author Rajarshi Guha
 */
public class BARDExperimentDataResourceTest extends EntityResourceTest {

    public BARDExperimentDataResourceTest(String resourceName) {
        super(resourceName);
    }

    @BeforeClass
    public void beforeclass() {
        setResourceName("exptdata");
    }

}
