package gov.nih.ncgc.bard.capextract.handler;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ClientHelper;
import gov.nih.ncgc.bard.capextract.SslHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public abstract class CapResourceHandler {
    private Client client;
    protected Logger log;

    private JAXBContext jc;
    private HttpClient httpClient;

    protected CapResourceHandler() {
        client = ClientHelper.createClient();
        httpClient = SslHttpClient.getHttpClient();
        log = LoggerFactory.getLogger(this.getClass());
        try {
            jc = JAXBContext.newInstance("gov.nih.ncgc.bard.capextract.jaxb");
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    protected ClientResponse getResponse(String url, CAPConstants.CapResource resource) {
        WebResource wr = client.resource(url);
        return wr.accept(resource.getMimeType()).
                header(CAPConstants.CAP_APIKEY_HEADER, CAPConstants.getApiKey()).
                get(ClientResponse.class);
    }

    protected <T> T getResponse2(String url, CAPConstants.CapResource resource) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", resource.getMimeType());
        get.setHeader(CAPConstants.CAP_APIKEY_HEADER, CAPConstants.getApiKey());
        HttpResponse response = httpClient.execute(get);
        if (response.getStatusLine().getStatusCode() != 200)
            throw new IOException("Got a HTTP " + response.getStatusLine().getStatusCode() + " for " + resource);

        Unmarshaller unmarshaller = null;
        try {
            unmarshaller = jc.createUnmarshaller();
            Object o = unmarshaller.unmarshal(response.getEntity().getContent());
            return (T) o;
        } catch (JAXBException e) {
            throw new IOException("Error unmarshalling document from " + url);
        }
    }
}
