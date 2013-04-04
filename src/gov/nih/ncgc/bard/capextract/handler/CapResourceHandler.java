package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.SslHttpClient;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.io.BufferedReader;
//import java.io.InputStream;

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
		@SuppressWarnings("unchecked")
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
        HttpResponse response;
        try {
            response = httpClient.execute(get);
        } catch (HttpHostConnectException ex) {
            ex.printStackTrace();
            try {
        	Thread.sleep(5000);
            } catch (InterruptedException ie) {ie.printStackTrace();}
            httpClient = SslHttpClient.getHttpClient();
            response = httpClient.execute(get);
        }
        if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 206)
            throw new IOException("Got a HTTP " + response.getStatusLine().getStatusCode() + " for " + resource + ": " + url);

        if (response.getStatusLine().getStatusCode() == 206)
            log.info("Got a 206 (partial content) ... make sure this is handled appropriately for " + resource + ": " + url);

        // For debugging
//        if (url.endsWith("assays/1640")) {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//            BufferedWriter writer = new BufferedWriter(new FileWriter("assay-1640.xml"));
//            String line;
//            while ((line = reader.readLine()) != null) writer.write(line+"\n");
//            writer.close();
//            System.exit(-1);
////        }

        // dump all documents for an entity type
//        if (url.contains("/projects/") && !url.endsWith("/projects/")) {
//            String[] toks = url.split("/");
//            String fname = "tmp-project/"+toks[toks.length-1]+".xml";
//            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
//            String line;
//            while ((line = reader.readLine()) != null) writer.write(line+"\n");
//            writer.close();
//            return null;
//        }

        Unmarshaller unmarshaller;
        try {
            unmarshaller = jc.createUnmarshaller();
            Reader reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
            Object o = unmarshaller.unmarshal(reader);
            @SuppressWarnings("unchecked")
            T t = (T)o;
            return t;
        } catch (JAXBException e) {
            throw new IOException("Error unmarshalling document from " + url, e);
        }
    }
    
    /**
     * Sets the extraction status for a given CAP resource.
     * 
     * @param status Status String, set from CAPConstants.(CAP_STATUS_READY | CAP_STATUS_STARTED | CAP_STATUS_COMPLETE) 
     * @param url The URL for the CAP resource
     * @param resource  The CAP resource 
     * @param entityVersionEtagId CAP resources are versioned, a header field labeled 'Etag' provides the current version
     * @return on success returns the new entity version (Etag), else returns -1
     */
    public int setExtractionStatus(String status, String url, CAPConstants.CapResource resource) {
	int etag = -1;
	String etagStr = null;
	try {
	    httpClient = SslHttpClient.getHttpClient();

	    //need to get the current CAP etag for the header, arggg
	    HttpGet get =  new HttpGet(url);
	    get.setHeader("Accept", resource.getMimeType());
	    get.setHeader(CAPConstants.CAP_APIKEY_HEADER, CAPConstants.getApiKey());

	    HttpResponse baseResponse = httpClient.execute(get);
	    if(baseResponse != null && baseResponse.getFirstHeader("Etag").getValue() != null) {
		etag = Integer.parseInt(baseResponse.getFirstHeader("Etag").getValue());

		HttpPut put = new HttpPut(url);
		put.setHeader("Accept", resource.getMimeType());
		put.setHeader(CAPConstants.CAP_APIKEY_HEADER, CAPConstants.getApiKey());
		put.setHeader("If-Match", Integer.toString(etag));  //use the etag here

		//set the status
		put.setEntity(new StringEntity(status));

		HttpResponse response = httpClient.execute(put);
		if(response.getStatusLine().getStatusCode() == 200) {
		    log.info("Changed CAP Entity status to "+status+ " for resource URL:"+url);
		    etag = Integer.parseInt(response.getFirstHeader("Etag").getValue());
		}
	    }
	} catch (ClientProtocolException e) {
	    log.warn("Error setting CAP extraction status ("+status+")");
	    e.printStackTrace();
	} catch (IOException e) {
	    log.warn("Error setting CAP extraction status ("+status+")");
	    e.printStackTrace();
	} 
	return etag;
    }
    
    
}
