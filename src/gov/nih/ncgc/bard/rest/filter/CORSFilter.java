package gov.nih.ncgc.bard.rest.filter;

import java.io.IOException;

import gov.nih.ncgc.bard.rest.BARDConstants;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;



/**
 * Modify responses so that they support CORS.
 * <p/>
 * This filter ensures that browsers that support CORS, will be able to access the API from hosts that are not
 * running the API itself. See <a href="http://en.wikipedia.org/wiki/Cross-origin_resource_sharing">CORS</a>
 * for a more detailed discussion.
 * <p/>
 * The filter will also modifies JSON responses to JSONP responses if indicated in the request corresponding
 * to this response.
 *
 * @author Rajarshi Guha
 */
public class CORSFilter implements ContainerResponseFilter {
    //@Override
    public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse response) {
	//String jsonp = response.getContainerRequest().getHeaderValue(BARDConstants.REQUEST_HEADER_JSONP);
        String jsonp = response.getHeaderString(BARDConstants.REQUEST_HEADER_JSONP);
        
        // if we were asked for a JSONP response, modify the content-type and the response appropriately.
        // We only do this is the response wsa going to be JSON 
        MediaType responseContentType = (MediaType) response.getHeaders().getFirst("Content-type");
        if (jsonp != null && responseContentType != null && responseContentType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            response.getHeaders().putSingle("Content-type", "application/javascript");
            Object entity = response.getEntity();
            String jsonpEntity = jsonp + "(" + entity + ")";
            response.setEntity(jsonpEntity);
        }

        response.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
        response.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        response.getHeaders().putSingle("Access-Control-Allow-Methods", "OPTIONS, GET, POST, HEAD");

//        String reqHead = containerRequest.getHeaderValue("Access-Control-Request-Headers");
        response.getHeaders().putSingle("Access-Control-Allow-Headers", "Origin, Accept, Content-Type, Depth, User-Agent, X-File-Size, X-Requested-With, If-Modified-Since, X-File-Name, Cache-Control");
        return response;
    }

    @Override
    public void filter(ContainerRequestContext arg0,
	    ContainerResponseContext arg1) throws IOException {
	// TODO Auto-generated method stub
	
    }
}
