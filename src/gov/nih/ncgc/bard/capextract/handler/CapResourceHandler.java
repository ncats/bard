package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.SslHttpClient;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public abstract class CapResourceHandler {
    protected Logger log;
    private JAXBContext jc;
    private HttpClient httpClient;

    protected CapResourceHandler() {
        httpClient = SslHttpClient.getHttpClient();
        log = LoggerFactory.getLogger(this.getClass());
        try {
            jc = JAXBContext.newInstance("gov.nih.ncgc.bard.capextract.jaxb");
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public <T> Vector<T> poll(String url, CAPConstants.CapResource resource) throws IOException {
	return poll(url, resource, false);
    }
    
    public <T> Vector<T> poll(String url, CAPConstants.CapResource resource, boolean skipPartial) throws IOException {
	Vector<T> vec = new Vector<T>();
	while (url != null) {
	    T t = getResponse(url, resource);
	    vec.add(t);
	    url = null;
	    try {
		Method getLinkList = t.getClass().getMethod("getLink", (Class<?>[])null);
		List<Link> links = (List<Link>)getLinkList.invoke(t, (Object[])null);
		for (Link link: links)
		    if (link.getRel().equals("next") && !skipPartial)
			url = link.getHref();
	    } catch (Exception e) {;}
	}
	return vec;
    }
    
    protected <T> T getResponse(String url, CAPConstants.CapResource resource) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", resource.getMimeType());
        get.setHeader(CAPConstants.CAP_APIKEY_HEADER, CAPConstants.getApiKey());
        HttpResponse response = httpClient.execute(get);
        if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 206)
            throw new IOException("Got a HTTP " + response.getStatusLine().getStatusCode() + " for " + resource + ": " + url);

        if (response.getStatusLine().getStatusCode() == 206)
            log.info("Got a 206 (partial content) ... make sure this is handled appropriately for " + resource + ": " + url);
            
//        if (resource == CAPConstants.CapResource.ASSAY) {
//            String xml = read(response.getEntity().getContent());
//            BufferedWriter writer = new BufferedWriter(new FileWriter("1640.xml"));
//            writer.write(xml);
//            writer.close();
//            System.exit(-1);
//        }

        Unmarshaller unmarshaller;
        try {
            unmarshaller = jc.createUnmarshaller();
            Reader reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
            Object o = unmarshaller.unmarshal(reader);
            return (T) o;
        } catch (JAXBException e) {
            throw new IOException("Error unmarshalling document from " + url, e);
        }
    }

    // for debug purposes
    private String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
        int n = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            n++;
            sb.append(line);
        }
        in.close();
        return sb.toString();

    }

}
