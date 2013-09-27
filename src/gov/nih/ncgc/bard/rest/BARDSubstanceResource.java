package gov.nih.ncgc.bard.rest;


import chemaxon.struc.Molecule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.NotFoundException;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.entity.Substance;
import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.search.MoleculeService;
import gov.nih.ncgc.util.MolRenderer;
import gov.nih.ncgc.util.functional.Functional;
import gov.nih.ncgc.util.functional.IApplyFunction;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/substances")
public class BARDSubstanceResource extends BARDResource<Substance> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    public String getResourceBase() {
        return BARDConstants.API_BASE + "/substances";
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns substance information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        return msg.toString();
    }


    @GET
    @Path("/")
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        
        Response response = null;

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        try {
            if (countRequested) {
                if (filter != null && filter.contains("[active]"))
                    response = Response.ok(String.valueOf(db.getSubstanceActiveCount())).build();
                else if (filter != null && (filter.contains("[test]") || filter.contains("[tested]")) )
                    response = Response.ok(String.valueOf(db.getSubstanceTestCount())).build();
                else response = Response.ok(String.valueOf(db.getEntityCount(Substance.class))).build();
            } else {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                String filterClause = "";
                if (filter != null) filterClause = "&filter=" + filter;

                String linkString = null;
                if (skip + top <= db.getEntityCount(Substance.class))
                    linkString = BARDConstants.API_BASE + "/substances?skip=" + (skip + top) + "&top=" + top + "&" + expandClause + filterClause;

                List<Substance> substances;
                if (filter != null && (filter.contains("[active]") || filter.contains("[tested]"))) {
                    substances = db.searchForSubstances(filter, skip, top, false);
                } else substances = db.searchForEntity(filter, skip, top, Substance.class);

                if (expandEntries) {
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(substances, linkString);
                    response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                } else {
                    List<String> links = new ArrayList<String>();
                    for (Substance a : substances) links.add(a.getResourcePath());
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                }
            }
            db.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return response;

    }

    @GET
    @Path("/{sid}/assays")
    public Response getAssaysForSubstance(@PathParam("sid") Long sid, String expand,
                                          @QueryParam("skip") Integer skip,
                                          @QueryParam("top") Integer top) throws SQLException, IOException {
        
        Response response;
        String linkString = null;
        List<Assay> p = db.getSubstanceAssays(sid, -1, -1);
        if (p == null) p = new ArrayList<Assay>();
        if (countRequested) response = Response.ok(String.valueOf(p.size())).type(MediaType.TEXT_PLAIN).build();

        if (p.size() > BARDConstants.MAX_DATA_COUNT) {
            if ((top == -1)) { // top was not specified, so we start from the beginning
                top = BARDConstants.MAX_DATA_COUNT;
            }
            if (skip == -1) skip = 0;
            String expandClause = "expand=false";
            if (expandEntries(expand)) expandClause = "expand=true";
            if (skip + top <= p.size())
                linkString = BARDConstants.API_BASE + "/substances/" + sid + "/assays?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
        }

        if (!expandEntries(expand)) {
            List<String> links = Functional.Apply(p, new IApplyFunction<Assay, String>() {
                public String eval(Assay assay) {
                    return assay.getResourcePath();
                }
            });
            BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
            response = Response.ok(Util.toJson(linkedEntity)).type(MediaType.APPLICATION_JSON).build();
        } else {
            BardLinkedEntity linkedEntity = new BardLinkedEntity(p, linkString);
            response = Response.ok(Util.toJson(linkedEntity)).type(MediaType.APPLICATION_JSON).build();
        }

        db.closeConnection();
        return response;
    }

    private Response getCompoundResponse(String id, String type, List<MediaType> mediaTypes, boolean expand) throws SQLException, IOException {
        if (!type.equals("cid") && !type.equals("sid")) throw new WebApplicationException(400);

        
        Substance s = null;

        if (type.equals("cid")) {
            List<Long> sids = db.getSidsByCid(Long.parseLong(id));
            if (!expand) {
                List<String> paths = new ArrayList<String>();
                for (Long sid : sids) paths.add(BARDConstants.API_BASE + "/substances/" + sid);
                db.closeConnection();
                return Response.ok(Util.toJson(paths), MediaType.APPLICATION_JSON).build();
            } else { // TODO should be able to get multiple SIDs at one go
                List<Substance> slist = new ArrayList<Substance>();
                for (Long sid : sids) {
                    Substance sub = db.getSubstanceBySid(sid);
                    if (sub != null) {
                        slist.add(sub);
                    } else {
                        log("No substance found for sid " + sid);
                    }
                }
                db.closeConnection();
                return Response.ok(Util.toJson(slist), MediaType.APPLICATION_JSON).build();
            }
        } else if (type.equals("sid"))
            s = db.getSubstanceBySid(Long.parseLong(id));
        db.closeConnection();

        if (s == null || s.getSid() == null) throw new WebApplicationException(404);

        Response response = null;
        if (mediaTypes.contains(BARDConstants.MIME_SMILES)) {
//            String smiles = c.getSmiles() + "\t" + id;
//            return Response.ok(smiles, BARDConstants.MIME_SMILES).build();
        } else if (mediaTypes.contains(BARDConstants.MIME_SDF)) {
//            Molecule mol = MolImporter.importMol(c.getSmiles());
//            mol.setProperty("cid", String.valueOf(c.getCid()));
//            mol.setProperty("probeId", c.getProbeId());
//            mol.setProperty("url", c.getUrl());
//            mol.setProperty("resourecePath", c.getResourcePath());
//            String sdf = mol.exportToFormat("sdf");
//            return Response.ok(sdf, BARDConstants.MIME_SDF).build();
        } else {
            String json = s.toJson();
            response = Response.ok(json, MediaType.APPLICATION_JSON).build();
        }
        return response;
    }

    // return compound (via CID) for this SID
    @GET
    @Path("/{sid}")
    public Response getResources(@PathParam("sid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            Response response = getCompoundResponse(resourceId, "sid", headers.getAcceptableMediaTypes(), expand != null && expand.equals("true"));
            if (countRequested && response != null) return Response.ok("1", MediaType.TEXT_PLAIN).build();
            else return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    // return list of SID's for this CID
    @GET
    @Path("/cid/{cid}")
    public Response getCompoundBySid(@PathParam("cid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            Response response = getCompoundResponse(resourceId, "cid", headers.getAcceptableMediaTypes(), expand != null && expand.equals("true"));
            if (countRequested && response != null) return Response.ok("1", MediaType.TEXT_PLAIN).build();
            else return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @POST
    @Path("/cid")
    @Consumes("application/x-www-form-urlencoded")
    public Response getCompoundByCids(@FormParam("cids") String cids,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand) throws SQLException {
        if (cids == null) throw new WebApplicationException(
                new Exception("Must specify the cids form parameter as a comma separated list of CIDs"), 400);


        String[] toks = cids.split(",");
        Long[] lcids = new Long[toks.length];
        for (int i = 0; i < toks.length; i++) lcids[i] = Long.parseLong(toks[i].trim());

        
        try {
            Long[][] mapping = db.getSidsByCids(lcids);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.createObjectNode();
            ArrayNode anode = mapper.createArrayNode();
            Long oldCid = mapping[0][0];
            anode.add(mapping[0][1]);
            for (int i = 1; i < mapping.length; i++) {
                Long cid = mapping[i][0];
                Long sid = mapping[i][1];
                anode.add(sid);
                if (cid != oldCid) {
                    node.put(oldCid.toString(), anode);
                    anode = mapper.createArrayNode();
                    anode.add(sid);
                }
                oldCid = cid;
            }
            node.put(oldCid.toString(), anode);
            return Response.ok(mapper.writeValueAsString(node)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
	} finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{sid}/image")
    public Response getImage(@PathParam("sid") String resourceId,
                             @QueryParam("s") Integer s,
                             @QueryParam("c") String c,
                             @QueryParam("a") String a) {
        try {

            MoleculeService molsrv = (MoleculeService) Util.getMoleculeService();
            Molecule molecule = molsrv.getMol(resourceId);
            if (molecule == null) throw new NotFoundException("No molecule for CID = " + resourceId);

            MolRenderer renderer = new MolRenderer();

            // size
            int size = 120;

            if (s != null && s >= 16 && s <= 512) size = s;

            // atom
            if (a != null) {
                for (String idx : a.split(",")) {
                    try {
                        int i = Integer.parseInt(idx);
                        if (i > 0 && i <= molecule.getAtomCount()) {
                            molecule.getAtom(i - 1).setAtomMap(1);
                        }
                    } catch (NumberFormatException ex) {
                        throw new WebApplicationException(ex, 400);
                    }
                }
            }

            if (c != null) {
                try {
                    Color color = Color.decode(c);
                    renderer.setBackground(color);
                } catch (NumberFormatException ex) {
                    throw new WebApplicationException(ex, 400);
                }
            }

            BufferedImage img = renderer.createImage(molecule, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Response.ok(baos.toByteArray()).type("image/png").build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        } catch (Exception e) {
            throw new WebApplicationException(e, 500);
        }
    }

    // return alle xperiment data for this SID
    @GET
    @Path("/{sid}/exptdata")
    public Response getExperimentData(@PathParam("sid") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        
        String linkString = null;

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        try {
            if ((top == -1)) { // top was not specified, so we start from the beginning
                top = BARDConstants.MAX_DATA_COUNT;
            }
            if (skip == -1) skip = 0;
            String expandClause = "expand=false";
            if (expandEntries) expandClause = "expand=true";
            linkString = BARDConstants.API_BASE + "/substances/" + resourceId + "/exptdata?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;

            String json;
            if (!expandEntries) {
                List<String> edids = db.getSubstanceDataIds(Long.valueOf(resourceId), skip, top, filter);
                if (countRequested) json = String.valueOf(edids.size());
                else {
                    List<String> links = new ArrayList<String>();
                    for (String edid : edids) {
                        ExperimentData ed = new ExperimentData();
                        ed.setExptDataId(edid);
                        links.add(ed.getResourcePath());
                    }
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                }
            } else {
                List<ExperimentData> data = db.getSubstanceData(Long.valueOf(resourceId), skip, top, filter);
                if (countRequested) json = String.valueOf(data.size());
                else {
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(data, linkString);
                    json = Util.toJson(linkedEntity);
                }
            }
            db.closeConnection();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{sid}/experiments")
    public Response getExperiments(@PathParam("sid") String resourceId,
                                   @QueryParam("filter") String filter,
                                   @QueryParam("expand") String expand,
                                   @QueryParam("skip") Integer skip,
                                   @QueryParam("top") Integer top) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        
        String linkString = null;
        String json;

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        try {
            if ((top == -1)) { // top was not specified, so we start from the beginning
                top = BARDConstants.MAX_DATA_COUNT;
            }
            if (skip == -1) skip = 0;
            String expandClause = "expand=false";
            if (expandEntries) expandClause = "expand=true";
            linkString = BARDConstants.API_BASE + "/substances/" + resourceId + "/experiments?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;

            if (!expandEntries) {
                List<Long> eids = db.getSubstanceExperimentIds(Long.valueOf(resourceId), skip, top);
                if (countRequested) json = String.valueOf(eids.size());
                else {
                    List<String> links = new ArrayList<String>();
                    for (Long eid : eids) {
                        Experiment ed = new Experiment();
                        ed.setBardExptId(eid);
                        links.add(ed.getResourcePath());
                    }
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                }
            } else {
                List<Experiment> data = db.getSubstanceExperiment(Long.valueOf(resourceId), skip, top);
                if (!countRequested) {
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(data, linkString);
                    json = Util.toJson(linkedEntity);
                } else json = String.valueOf(data.size());
            }
            db.closeConnection();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
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
        
        try {
            List<Substance> substances = db.getSubstanceByETag(skip != null ? skip : -1, top != null ? top : -1, resourceId);
            String json = Util.toJson(substances);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            throw new WebApplicationException(e, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}