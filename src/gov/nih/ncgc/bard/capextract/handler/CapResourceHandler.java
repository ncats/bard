package gov.nih.ncgc.bard.capextract.handler;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ClientHelper;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public abstract class CapResourceHandler {
    private Client client;

    protected CapResourceHandler() {
        client = ClientHelper.createClient();
    }

    protected ClientResponse getResponse(String url, CAPConstants.CapResource resource) {
        WebResource wr = client.resource(url);
        return wr.accept(resource.getMimeType()).
                header(CAPConstants.CAP_APIKEY_HEADER, CAPConstants.getApiKey()).
                get(ClientResponse.class);
    }
}
