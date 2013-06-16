package gov.nih.ncgc.bard.rest.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * Modify responses so that they support CORS.
 *
 * This filter ensures that browsers that support CORS, will be able to access the API from hosts that are not
 * running the API itself. See <a href="http://en.wikipedia.org/wiki/Cross-origin_resource_sharing">CORS</a>
 * for a more detailed discussion.
 *
 * In the future this filter will also be used to modify JSON responses to JSONP responses if desired. This
 * action is dependent on a global variable set by the request filter. Not very elegant.
 *
 * @author Rajarshi Guha
 */
public class CORSFilter implements ContainerResponseFilter {
    @Override
    public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse containerResponse) {
        containerResponse.getHttpHeaders().putSingle("Access-Control-Allow-Origin", "*");
        containerResponse.getHttpHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST");

        String reqHead = containerRequest.getHeaderValue("Access-Control-Request-Headers");
        containerResponse.getHttpHeaders().putSingle("Access-Control-Allow-Headers", reqHead);

        return containerResponse;
    }
}
