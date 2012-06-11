package gov.nih.ncgc.bard.rest;

import org.testng.annotations.BeforeClass;

/**
 * A test of the project resource.
 *
 * @author Rajarshi Guha
 */
public class BARDProjectResourceTest extends EntityResourceTest {

    public BARDProjectResourceTest(String resourceName) {
        super(resourceName);
    }

    @BeforeClass
    public void beforeclass() {
        setResourceName("projects");
    }

}
