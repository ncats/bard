package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
import gov.nih.ncgc.bard.entity.DummyEntity;
import gov.nih.ncgc.bard.tools.Util;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A resource to expose CAP information.
 * <p/>
 * Currently the this resource only has one subresource that exposes
 * the CAP dictionary. This is is exposed a direct conversion from the
 * internal data structure ({@link CAPDictionary}) to a JSON representation,
 * so it could be optimized in the future.
 *
 * @author Rajarshi Guha
 */
@Path("/cap")
public class BARDCapResource extends BARDResource<DummyEntity> {

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Provides access to CAP data and metadata\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/cap/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCount() throws SQLException, ClassNotFoundException, IOException {
        if (countRequested) {
            
            CAPDictionary dict = db.getCAPDictionary();
            if (dict == null) throw new WebApplicationException(500);
            return Response.ok(String.valueOf(dict.size()), MediaType.TEXT_PLAIN).build();
        } else return Response.status(405).build();
    }

    @GET
    @Path("/dictionary")
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
        
        try {
            CAPDictionary dict = db.getCAPDictionary();
            String json = Util.toJson(dict);
            db.closeConnection();
            return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        }
    }

    @GET
    @Path("/dictionary/{id}")
    public Response getDictElement(@PathParam("id") String dictId) {
        
        try {
            CAPDictionary dict = db.getCAPDictionary();
            CAPDictionaryElement elem;
            if (Util.isNumber(dictId)) elem = dict.getNode(new BigInteger(dictId));
            else elem = dict.getNode(dictId);
            if (elem == null) throw new NotFoundException("No CAP dictionary element for " + dictId, Response.status(404).entity("").build());
            return Response.ok(Util.toJson(elem)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        }
    }

    @GET
    @Path("/dictionary/{id}/children")
    public Response getDictElementChildren(@PathParam("id") String dictId) {
        
        try {
            CAPDictionary dict = db.getCAPDictionary();
            Set<CAPDictionaryElement> elem;
            if (Util.isNumber(dictId)) elem = dict.getChildren(new BigInteger(dictId));
            else elem = dict.getChildren(dictId);
            return Response.ok(Util.toJson(elem)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        }
    }

    @GET
    @Path("/dictionary/{id}/parents")
    public Response getDictElementParents(@PathParam("id") String dictId) {
        
        try {
            CAPDictionary dict = db.getCAPDictionary();
            Set<CAPDictionaryElement> elem;
            if (Util.isNumber(dictId)) elem = dict.getParents(new BigInteger(dictId));
            else elem = dict.getParents(dictId);
            return Response.ok(Util.toJson(elem)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        }
    }

    @GET
    @Path("/dictionary/roots")
    public Response getDictionaryRoots() {
        
        try {

            // root elements are those elements without children
            List<CAPDictionaryElement> rootElems = new ArrayList<CAPDictionaryElement>();

            CAPDictionary dict = db.getCAPDictionary();
            Set<CAPDictionaryElement> elems = dict.getNodes();
            for (CAPDictionaryElement elem : elems) {
                Set<CAPDictionaryElement> parents = dict.getParents(elem);
                if (parents.size() == 0) rootElems.add(elem);
            }
            return Response.ok(Util.toJson(rootElems)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        }
    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Class<DummyEntity> getEntityClass() {
        return DummyEntity.class;
    }

    @Override
    public String getResourceBase() {
        return BARDConstants.API_BASE + "/cap";
    }
}
