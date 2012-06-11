package gov.nih.ncgc.bard.rest;

import org.testng.annotations.BeforeClass;

/**
 * A test of the compound resource.
 *
 * @author Rajarshi Guha
 */
public class BARDCompoundResourceTest extends EntityResourceTest {

    public BARDCompoundResourceTest(String resourceName) {
        super(resourceName);
    }

    @BeforeClass
    public void beforeclass() {
        setResourceName("compounds");
    }

}
