package gov.nih.ncgc.bard.rest;

import com.sun.jersey.api.NotFoundException;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Probe;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rajarshi Guha
 */

@Path("/probes")
public class BARDProbeResource extends BARDResource<Probe> {
    @Override
    public Class<Probe> getEntityClass() {
        return Probe.class;
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns probe information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        return msg.toString();
    }

    @Override
    public String getResourceBase() {
        return BARDConstants.API_BASE + "/probes";
    }

    @GET
    @Path("/{pid}/experiments")
    public Response getExperiments(@PathParam("pid") String pid,
                                   @QueryParam("filter") String filter,
                                   @QueryParam("expand") String expand,
                                   @QueryParam("skip") Integer skip,
                                   @QueryParam("top") Integer top) throws SQLException {
        try {
            Compound probe = getProbeCompound(pid);
            if (probe == null) throw new NotFoundException("No such probe identifier: " + pid);
            BARDCompoundResource bcr = new BARDCompoundResource();
            return bcr.getExperiments(String.valueOf(probe.getCid()), filter, expand, skip, top);
        } finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{pid}/exptdata")
    public Response getExperimentData(@PathParam("pid") String pid,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) throws SQLException {
        try {
            Compound probe = getProbeCompound(pid);
            if (probe == null) throw new NotFoundException("No such probe identifier: " + pid);
            BARDCompoundResource bcr = new BARDCompoundResource();
            return bcr.getExperimentData(String.valueOf(probe.getCid()), filter, expand, skip, top);
        } finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{pid}/projects")
    public Response getProjectsForCompound(@PathParam("pid") String pid,
                                           @QueryParam("expand") String expand,
                                           @QueryParam("skip") Integer skip,
                                           @QueryParam("top") Integer top) throws SQLException, IOException {
        try {
            Compound probe = getProbeCompound(pid);
            if (probe == null) throw new NotFoundException("No such probe identifier: " + pid);
            BARDCompoundResource bcr = new BARDCompoundResource();
            return bcr.getProjectsForCompound(probe.getCid(), expand, skip, top);
        } finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{pid}/image")
    public Response getImage(@PathParam("pid") String pid,
                             @QueryParam("s") Integer s,
                             @QueryParam("c") String c,
                             @QueryParam("a") String a) throws SQLException {
        try {
            Compound probe = getProbeCompound(pid);
            if (probe == null) throw new NotFoundException("No such probe identifier: " + pid);
            BARDCompoundResource bcr = new BARDCompoundResource();
            return bcr.getImage(String.valueOf(probe.getCid()), s, c, a);
        } finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{pid}/assays")
    public Response getAssaysForCompound(@PathParam("pid") String pid,
                                         @QueryParam("expand") String expand,
                                         @QueryParam("filter") String filter,
                                         @QueryParam("skip") Integer skip,
                                         @QueryParam("top") Integer top) throws SQLException, IOException {
        try {
            Compound probe = getProbeCompound(pid);
            if (probe == null) throw new NotFoundException("No such probe identifier: " + pid);
            BARDCompoundResource bcr = new BARDCompoundResource();
            return bcr.getAssaysForCompound(probe.getCid(), expand, filter, skip, top);
        } finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{pid}/annotations")
    public Response getProbeAnnotations(@PathParam("pid") String pid)
            throws SQLException, IOException {
        try {
            Compound probe = getProbeCompound(pid);
            if (probe == null) throw new NotFoundException("No such probe identifier: " + pid);
            BARDCompoundResource bcr = new BARDCompoundResource();
            return bcr.getCompoundAnnotations(probe.getCid());
        } finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{pid}")
    public Response getResources(@PathParam("pid") String resourceId, String filter, String expand) {
        try {
            Compound probe = getProbeCompound(resourceId);
            if (probe == null) throw new NotFoundException("No such probe identifier: " + resourceId);
            BARDCompoundResource bcr = getCmpdResource();
            return bcr.getResources(String.valueOf(probe.getCid()), filter, expand);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new WebApplicationException(e);
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private BARDCompoundResource getCmpdResource() {
        BARDCompoundResource bcr = new BARDCompoundResource();
        bcr.headers = headers;
        bcr.httpServletRequest = httpServletRequest;
        return bcr;
    }

    private Compound getProbeCompound(String pid) throws SQLException {
        List<Compound> probe = db.getCompoundsByProbeId(pid);
        if (probe == null || probe.size() == 0) return null;
        else return probe.get(0);
    }

    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        return getResources(null, filter, expand);
    }

    @GET
    @Path("/")
    public Response getAll(@QueryParam("filter") String filter,
                           @QueryParam("expand") String expand,
                           @QueryParam("skip") Integer skip,
                           @QueryParam("top") Integer top,

                           @QueryParam("type") String type,
                           @QueryParam("cutoff") Double cutoff,
                           @QueryParam("rankBy") String rankBy,
                           @QueryParam("annot") String annot) throws SQLException, IOException {

        Response response = null;
        Long start, end;
        BARDCompoundResource bcr = getCmpdResource();

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        if (filter == null) {
            if (countRequested)
                response = Response.ok(String.valueOf(db.getEntityCount(Probe.class))).build();
            else {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";

                String linkString = null;
                start = System.currentTimeMillis();
                if (skip + top <= db.getEntityCount(Probe.class))
                    linkString = BARDConstants.API_BASE + "/probes?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
                end = System.currentTimeMillis();
                System.out.println("TIME entity count: " + ((end - start) * 1e-3));

                start = System.currentTimeMillis();
                List compounds = db.searchForEntity(filter, skip, top, Probe.class);
                end = System.currentTimeMillis();
                System.out.println("TIME entity search: " + ((end - start) * 1e-3));


                if (expandEntries) {
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(compounds, linkString);

                    start = System.currentTimeMillis();
                    response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                    end = System.currentTimeMillis();
                    System.out.println("TIME json generate: " + ((end - start) * 1e-3));

                } else {
                    List<String> links = new ArrayList<String>();
                    for (Object a : compounds) links.add(((Compound) a).getResourcePath());
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                }
            }
        } else {   // do a filtered search

            // examine the filter argument to see if we should do a structure search
            if (filter.contains("[structure]")) {
                filter = filter.trim().replace("[structure]", "");
                response = bcr.doStructureSearch(filter, type, top, skip, cutoff, rankBy, db, expandEntries, annot);
            }
        }
        db.closeConnection();
        return response;
    }


}
