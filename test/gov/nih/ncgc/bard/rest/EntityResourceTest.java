package gov.nih.ncgc.bard.rest;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public abstract class EntityResourceTest {
    String resourceName = null;
    //    String prefix = "http://assay.nih.gov/bard/rest/v1/";
    String prefix = "http://localhost:8080/bard/rest/v1";

    Client client;

    public EntityResourceTest(String resourceName) {
        this.resourceName = resourceName;
        client = Client.create();
    }

    protected void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Test
    public void testResourceName() {
        Assert.assertNotNull(resourceName);
        Assert.assertFalse(resourceName.equals(""));
    }

    @Test
    public void test_info() {
        String url = prefix + resourceName + "/_info";
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");
    }

    @Test
    public void test_count() {
        String url = prefix + resourceName + "/_count";
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");

        Integer count = Integer.valueOf(response.getEntity(String.class));
        Assert.assertTrue(count > 0, resourceName + " had zero instances from _count");
    }
}
