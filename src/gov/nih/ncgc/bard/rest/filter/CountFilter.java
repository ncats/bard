package gov.nih.ncgc.bard.rest.filter;

import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import gov.nih.ncgc.bard.rest.BARDConstants;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Processes a request to identify whether a count is desired or not.
 * <p/>
 * If the resource path contains <code>/_count</code> at the end, a header is
 * set so that the final handling class will know whether to
 * return a count or not, for whatever resource or subresource is requested.
 * <p/>
 * Any resource should check the request headers and if the <code>x-count-entities</code>
 * is present, return a count instead of the entities.
 *
 * @author Rajarshi Guha
 */
public class CountFilter implements ContainerRequestFilter {

    public ContainerRequest filter(ContainerRequest request) {
        String path = request.getPath();
        if (path.endsWith("/_count")) {
            MultivaluedMap<String, String> headers = request.getRequestHeaders();
            headers.add(BARDConstants.REQUEST_HEADER_COUNT, "true");
            request.setHeaders((InBoundHeaders) headers);

            String uriString = request.getRequestUri().getPath().replace("/_count", "");
            try {
                URI uri = new URI(uriString);
                request.setUris(request.getBaseUri(), uri);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        }
        return request;
    }
}
