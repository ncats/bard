package gov.nih.ncgc.bard.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Interface for MLBD resources.
 *
 * @author Rajarshi Guha
 */
public interface IBARDResource {
    @GET
    @Produces("text/plain")
    @Path("/info")
    public String info();

    @GET
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top);

    @GET
    @Path("/{name}")
    public Response getResources(@PathParam("name") String resourceId,
                                 @QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand);
}
