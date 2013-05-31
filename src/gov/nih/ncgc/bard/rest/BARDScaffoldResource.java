package gov.nih.ncgc.bard.rest;


import gov.nih.ncgc.bard.entity.*;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.search.MoleculeService;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.ArrayList;

import chemaxon.struc.Molecule;
import chemaxon.struc.MolAtom;
import chemaxon.util.MolHandler;


/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/scaffolds")
public class BARDScaffoldResource extends BARDResource<Scaffold> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;

    public Class<Scaffold> getEntityClass() {
        return Scaffold.class;
    }

    public String getResourceBase() {
        return BARDConstants.API_BASE + "/scaffolds";
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {

        StringBuilder msg = new StringBuilder("Returns scaffold information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        return msg.toString();
    }

    /**
     * Return a count of (possibly filtered) instances of a given resource.
     *
     * @param filter A query filter or null
     * @return the number of instances
     */
    public String count(@QueryParam("filter") String filter) {
        return null;
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        if (top == null || top < 0) {
            top = 100;
        }
        if (skip == null || skip < 0) {
            skip = 0;
        }

        
        DBUtils db = new DBUtils ();
        try {
            String linkString = null;
            if (skip + top <= db.getEntityCount(Scaffold.class))
                linkString = BARDConstants.API_BASE + "/scaffolds?skip=" 
                    + (skip + top) + "&top=" + top+"&expand="+expand;

            List<Scaffold> scaffolds = db.searchForEntity
                (filter, skip, top, Scaffold.class);

            String json;
            if (expand != null && expand.equalsIgnoreCase("true")) {
                json = Util.toJson(scaffolds);
            }
            else {
                List<String> links = new ArrayList<String>();
                for (Scaffold scaf : scaffolds) 
                    links.add(scaf.getResourcePath());
                BardLinkedEntity linkedEntity = 
                    new BardLinkedEntity(links, linkString);
                json = Util.toJson(linkedEntity);
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }
        catch (Exception ex) {
            throw new WebApplicationException
                (Response.status(500).entity(ex.getMessage()).build());
        }
        finally {
            try {
                db.closeConnection();
            }
            catch (Exception ex) {}
        }
    }

    @GET
    @Path("/{scafid}")
    public Response getResources(@PathParam("scafid") String resourceId,
                                 @QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils ();
        try {
            List<Scaffold> scaffolds = 
                db.getScaffoldsById(Long.parseLong(resourceId));

            String json;
            if (scaffolds.size() == 1) {
                json = Util.toJson(scaffolds.get(0));
            }
            else if (expand != null && expand.equalsIgnoreCase("true")) {
                json = Util.toJson(scaffolds);
            }
            else {
                List<String> links = new ArrayList<String>();
                for (Scaffold scaf : scaffolds) 
                    links.add(scaf.getResourcePath());
                json = Util.toJson(links);
            }

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            throw new WebApplicationException(e, 500);
        } 
        finally {
            try {
                db.closeConnection();
            } 
            catch (Exception ex) {}
        }
    }

    @GET
    @Path("/{scafid}/compounds")
    public Response getCompounds (@PathParam("scafid") String resourceId,
                                  @QueryParam("expand") String expand,
                                  @QueryParam("skip") Integer skip,
                                  @QueryParam("top") Integer top) {

        DBUtils db = new DBUtils ();
        try {
            if (skip == null || skip < 0) skip = 0;
            if (top == null || top <= 0) top = 100;

            List<Compound> compounds = 
                db.getCompoundsByScafId(Long.parseLong(resourceId), skip, top);

            MoleculeService molsrv = 
                (MoleculeService) Util.getMoleculeService();

            MolHandler mh = new MolHandler ();
            for (Compound c : compounds) {
                try {
                    Molecule mol = molsrv.getMol(c.getCid());
                    if (mol != null) {
                        mol = mol.cloneMolecule();
                        mol.dearomatize();
                        mh.setMolecule(c.getHighlight());
                        Molecule scaf = mh.getMolecule();
                        for (Molecule frag : scaf.convertToFrags()) {
                            for (MolAtom a : frag.getAtomArray()) {
                                int map = a.getAtomMap();
                                if (map > 0) {
                                    // highlight the scaffold within this 
                                    //   molecule
                                    mol.getAtom(map-1).setAtomMap(map);
                                }
                            }
                        }
                        // update the highlight with the 
                        c.setHighlight(mol.toFormat("smiles:q"));
                    }
                }
                catch (Exception ex) {
                    warning ("** Can't get highlight for compound "
                             +c.getCid());
                }
            }

            String json;
            if (expand != null && expand.equalsIgnoreCase("true")) {
                json = Util.toJson(compounds);
            }
            else {
                List<String> links = new ArrayList<String>();
                for (Compound c : compounds) 
                    links.add(c.getResourcePath());
                json = Util.toJson(links);
            }

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }
        catch (Exception ex) {
            throw new WebApplicationException(ex, 500);            
        }
        finally {
            try {
                db.closeConnection();
            }
            catch (Exception ex) {}
        }
    }

    @GET
    @Path("/{scafid}/exptdata")
    public Response getExperimentData (@PathParam("scafid") String resourceId,
                                       @QueryParam("expand") String expand,
                                       @QueryParam("skip") Integer skip,
                                       @QueryParam("top") Integer top,
                                       @QueryParam("outcome") Integer outcome)
    {
        return getExperimentDataResponse 
            (Long.parseLong(resourceId), -1, expand, skip, top, outcome);
    }

    @GET
    @Path("/{scafid}/exptdata/{exptid}")
    public Response getExperimentData (@PathParam("scafid") String scafId,
                                       @PathParam("exptid") String exptId,
                                       @QueryParam("expand") String expand,
                                       @QueryParam("skip") Integer skip,
                                       @QueryParam("top") Integer top,
                                       @QueryParam("outcome") Integer outcome)
    {
        return getExperimentDataResponse 
            (Long.parseLong(scafId), Long.parseLong(exptId), 
             expand, skip, top, outcome);
    }


    protected Response getExperimentDataResponse 
        (long scafId, long exptId, String expand, Integer skip, 
         Integer top, Integer outcome) {

        DBUtils db = new DBUtils ();
        try {
            if (skip == null || skip < 0) skip = 0;
            if (top == null || top <= 0) top = 100;

            List<ExperimentData> exptdata = db.getExperimentDataByScafId
                (scafId, exptId, skip, top, outcome != null ? outcome : 0);

            String json;
            if (expand != null && expand.equalsIgnoreCase("true")) {
                json = Util.toJson(exptdata);
            }
            else {
                List<String> links = new ArrayList<String>();
                for (ExperimentData ed : exptdata) 
                    links.add(ed.getResourcePath());
                json = Util.toJson(links);
            }

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }
        catch (Exception ex) {
            throw new WebApplicationException(ex, 500);            
        }
        finally {
            try {
                db.closeConnection();
            }
            catch (Exception ex) {}
        }
    }

    @Override
    @GET
    @Path("/etag/{etag}")
    public Response getEntitiesByETag(@PathParam("etag") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {
        DBUtils db = new DBUtils();
        try {
            List<Scaffold> scaffolds = db.getScaffoldsByETag
                (resourceId, skip != null ? skip : -1, top != null ? top : -1);
            String json;
            if (expand != null && expand.equalsIgnoreCase("true")) {
                json = Util.toJson(scaffolds);
            }
            else {
                List<String> links = new ArrayList<String>();
                for (Scaffold scaf : scaffolds) 
                    links.add(scaf.getResourcePath());
                json = Util.toJson(links);
            }

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } 
        catch (Exception e) {
            throw new WebApplicationException(e, 500);
        } 
        finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
