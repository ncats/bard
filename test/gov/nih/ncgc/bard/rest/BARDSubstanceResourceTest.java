package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.Substance;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

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


    @Test
    public void getSubstanceBySid() throws IOException {
        String url = prefix + resourceName + "/8144";
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");

        MediaType type = response.getType();
        Assert.assertTrue(type.toString().equals("application/json"));

        String json = response.getEntity(String.class);
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.trim().equals(""));

        Object o = new ObjectMapper().readValue(json, Substance.class);
        Assert.assertTrue(o instanceof Substance);

        Substance s = (Substance) o;
        Assert.assertEquals(new Long(8144), s.getSid());
    }
}
