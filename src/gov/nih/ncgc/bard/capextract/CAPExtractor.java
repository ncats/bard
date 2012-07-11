package gov.nih.ncgc.bard.capextract;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import gov.nih.ncgc.bard.capextract.jaxb.Bardexport;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Example code to play with the Broad CAP Data Export API.
 *
 * @author Rajarshi Guha
 */
public class CAPExtractor {

    public CAPExtractor() {
    }

    public void run() throws IOException, NoSuchAlgorithmException {
        Client client = ClientHelper.createClient();
        client.addFilter(new LoggingFilter());

        WebResource resource = client.resource(CAPConstants.CAP_ROOT);
        ClientResponse response = resource.accept(CAPConstants.CAP_ROOT_MIMETYPE).
                header(CAPConstants.CAP_APIKEY_HEADER, CAPConstants.getApiKey()).
                get(ClientResponse.class);

        int status = response.getStatus();
        if (status != 200) throw new IOException("Got HTTP " + status + " from data export API");

        Bardexport s = response.getEntity(Bardexport.class);

    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        CAPExtractor c = new CAPExtractor();
        c.run();
    }


}
