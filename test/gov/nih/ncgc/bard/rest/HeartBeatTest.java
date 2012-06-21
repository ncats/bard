package gov.nih.ncgc.bard.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Connects to each resource, to make sure they are up.
 * <p/>
 * Doesn't actually check that they are returning the appropriate values.
 *
 * @author Rajarshi Guha
 */
public class HeartBeatTest {
    Client client;
    String prefix = "http://assay.nih.gov/bard/rest/v1/";

    public HeartBeatTest() {
        client = Client.create();
    }

    @DataProvider
    public Object[][] resourceNameProvider() {
        return new Object[][]{
                {"assays", 200},
                {"compounds", 200},
                {"substances", 200},
                {"experiments", 200},
                {"exptdata", 200},
                {"documents", 200},
                {"projects", 200},
                {"targets", 200}
        };
    }

    @Test(groups = "heartbeat", dataProvider = "resourceNameProvider")
    public void connectToResource(String resourceName, Integer expectedStatus) {
        String url = prefix + resourceName + "/_info";
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        Integer status = response.getStatus();
        Assert.assertEquals(status, expectedStatus);
    }

    // actually tests a QSL query
    @Test(groups = "heartbeat", dataProvider = "resourceNameProvider")
    public void connectToCountResource(String resourceName, Integer expectedStatus) {
        String url = prefix + resourceName + "/_count";
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        Integer status = response.getStatus();
        Assert.assertEquals(status, expectedStatus);
    }
}
