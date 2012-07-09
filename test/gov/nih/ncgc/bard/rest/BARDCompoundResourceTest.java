package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import gov.nih.ncgc.bard.entity.Compound;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

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

    @Test
    public void getCompoundByCid() throws IOException {
        String url = prefix + resourceName + "/323";
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");

        MediaType type = response.getType();
        Assert.assertTrue(type.toString().equals("application/json"));

        String json = response.getEntity(String.class);
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.trim().equals(""));

        Object o = new ObjectMapper().readValue(json, Compound.class);
        Assert.assertTrue(o instanceof Compound);
    }

    @Test
    public void getCompoundBySid() throws IOException {
        String url = prefix + resourceName + "/sid/8144";
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");

        MediaType type = response.getType();
        Assert.assertTrue(type.toString().equals("application/json"));

        String json = response.getEntity(String.class);
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.trim().equals(""));

        Object o = new ObjectMapper().readValue(json, Compound.class);
        Assert.assertTrue(o instanceof Compound);

        Compound c = (Compound) o;
        Assert.assertEquals(new Long(323), c.getCid());
    }

    @Test
    public void compoundsAreTheSameTest() throws IOException {
        String url = prefix + resourceName + "/sid/8144";
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");

        MediaType type = response.getType();
        Assert.assertTrue(type.toString().equals("application/json"));

        String json = response.getEntity(String.class);
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.trim().equals(""));

        Object o1 = new ObjectMapper().readValue(json, Compound.class);
        Assert.assertTrue(o1 instanceof Compound);

        Compound c1 = (Compound) o1;

        url = prefix + resourceName + "/323";
        resource = client.resource(url);
        response = resource.get(ClientResponse.class);
        status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");

        type = response.getType();
        Assert.assertTrue(type.toString().equals("application/json"));

        json = response.getEntity(String.class);
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.trim().equals(""));

        Object o2 = new ObjectMapper().readValue(json, Compound.class);
        Compound c2 = (Compound) o2;

        Assert.assertTrue(c1.getCid().equals(c2.getCid()));
        Assert.assertTrue(c1.getSmiles().equals(c2.getSmiles()));
    }

}
