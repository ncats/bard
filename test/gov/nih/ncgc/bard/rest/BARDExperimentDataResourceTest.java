package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.ExperimentData;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

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

    @DataProvider
    public Object[][] singlePointExptIdProvider() {
        return new Object[][]{
                {43351476, 0}
        };
    }

    @Test(dataProvider = "singlePointExptIdProvider")
    public void getSinglePoint(Long id, int junk) throws IOException {
        String url = prefix + resourceName + "/" + id;
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");

        MediaType type = response.getType();
        Assert.assertTrue(type.toString().equals("application/json"));

        String json = response.getEntity(String.class);
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.trim().equals(""));

        Object o = new ObjectMapper().readValue(json, ExperimentData.class);
        Assert.assertTrue(o instanceof ExperimentData);
        ExperimentData ed = (ExperimentData) o;
        Assert.assertNotNull(ed.getReadouts());
        Assert.assertTrue(ed.getReadouts().get(0).getDescription().equals("single point"));
        Assert.assertNull(ed.getReadouts().get(0).getAc50());
    }

    @Test
    public void getMultiLayer() throws IOException {
        String url = prefix + resourceName + "/" + 639196;
        WebResource resource = client.resource(url);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        Assert.assertEquals(status, 200, "Response was " + status + " rather than 200");

        MediaType type = response.getType();
        Assert.assertTrue(type.toString().equals("application/json"));

        String json = response.getEntity(String.class);
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.trim().equals(""));

        Object o = new ObjectMapper().readValue(json, ExperimentData.class);
        Assert.assertTrue(o instanceof ExperimentData);
        ExperimentData ed = (ExperimentData) o;
        Assert.assertNotNull(ed.getReadouts());
        Assert.assertEquals(3, ed.getReadouts().size());
        Assert.assertTrue(ed.getReadouts().get(0).getDescription().equals("dose.response"));
        Assert.assertNull(ed.getReadouts().get(0).getAc50());
    }
    // http://localhost:8080/bard/rest/v1/exptdata/43351476
    // http://localhost:8080/bard/rest/v1/exptdata/17539469

}
