package gov.nih.ncgc.bard.rest;

import chemaxon.struc.Molecule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.NotFoundException;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Biology;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.TargetClassification;
import gov.nih.ncgc.bard.search.Facet;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.OrderedSearchResultHandler;
import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.search.MoleculeService;
import gov.nih.ncgc.search.SearchParams;
import gov.nih.ncgc.search.SearchService2;
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
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/compounds")
public class BARDCompoundResource extends BARDResource<Compound> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    /**
     * Normally using newCachedThreadPool is not recommended since it's
     * an unbounded pool. However, given that our search handler is really
     * meant to be short-lived, it's an ideal candidate for this particular
     * thread pool. If this isn't the case, then a more refined instance
     * of ThreadPoolExecutor should be used instead!
     */
    static final ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * these parameters should be configurable somewhere; having a static
     * is really a bad idea here. We should hook this into the lifecycle
     * of the web container so as to properly performing clean up!
     */
    /*
    static final ExecutorService threadPool = 
        new ThreadPoolExecutor (10, // core size
                                50, // max pool size
                                60, // idle timeout 1m
                                TimeUnit.SECONDS,
                                new ArrayBlockingQueue<Runnable>(20));
    */
    public Class<Compound> getEntityClass() {
        return Compound.class;
    }

    public String getResourceBase() {
        return BARDConstants.API_BASE + "/compounds";
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns compound information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        return msg.toString();
    }

    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        return getResources(null, filter, expand);
    }

    @GET
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

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        if (filter == null) {
            if (countRequested)
                response = Response.ok(String.valueOf(db.getEntityCount(Compound.class))).build();
            else {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";

                String linkString = null;
                start = System.currentTimeMillis();
                if (skip + top <= db.getEntityCount(Compound.class))
                    linkString = BARDConstants.API_BASE + "/compounds?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
                end = System.currentTimeMillis();
                System.out.println("TIME entity count: " + ((end - start) * 1e-3));

                start = System.currentTimeMillis();
                List<Compound> compounds = db.searchForEntity(filter, skip, top, Compound.class);
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
                    for (Compound a : compounds) links.add(a.getResourcePath());
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                }
            }
        } else {   // do a filtered search

            // examine the filter argument to see if we should do a structure search
            if (filter.contains("[structure]")) {
                filter = filter.trim().replace("[structure]", "");
                response = doStructureSearch(filter, type, top, skip, cutoff, rankBy, db, expandEntries, annot);
            } else if (filter.contains("[tested]") || filter.contains("[active]")) {
                if (countRequested && filter.contains("[tested]"))
                    response = Response.ok(String.valueOf(db.getCompoundTestCount())).build();
                else if (countRequested && filter.contains("[active]"))
                    response = Response.ok(String.valueOf(db.getCompoundActiveCount())).build();
                else {
                    if ((top == -1)) { // top was not specified, so we start from the beginning
                        top = BARDConstants.MAX_COMPOUND_COUNT;
                    }
                    if (skip == -1) skip = 0;
                    String expandClause = "expand=false";
                    if (expandEntries) expandClause = "expand=true";

                    String annotString = "annot=false";
                    if (annotString != null) annotString = "annot=" + annot;

                    String linkString = null;
                    if (skip + top <= db.getCompoundTestCount())
                        linkString = BARDConstants.API_BASE + "/compounds?skip=" + (skip + top) + "&top=" + top + "&" + expandClause + "&" + annotString + "&filter=[tested]";


                    List<Compound> compounds = db.searchForCompounds(filter, skip, top, annot != null && annot.toLowerCase().equals("true"));
                    if (expandEntries) {
                        BardLinkedEntity linkedEntity = new BardLinkedEntity(compounds, linkString);
                        response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                    } else {
                        List<String> links = new ArrayList<String>();
                        for (Compound a : compounds) links.add(a.getResourcePath());
                        BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                        response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                    }
                }
            }
        }
        db.closeConnection();
        return response;
    }

    protected Response doStructureSearch(String query, String type, int top, int skip, Double cutoff, String rankBy,
                                         DBUtils db, boolean expandEntries, String annot) throws SQLException, IOException {
        return doStructureSearch(query, type, top, skip, cutoff, rankBy, db, expandEntries, annot, false);
    }

    protected Response doStructureSearch(String query, String type, int top, int skip, Double cutoff, String rankBy,
                                       DBUtils db, boolean expandEntries, String annot, boolean probeSubset) throws SQLException, IOException {

        Response response;
        SearchService2 search = null;
        try {
            search = Util.getSearchService();
        } catch (Exception e) {
            throw new WebApplicationException(new Exception("Error in getting a search service instance", e), 500);
        }

        SearchParams params;
        if (type != null) {
            if (type.startsWith("sub")) {
                params = SearchParams.substructure();
            } else if (type.startsWith("super")) {
                params = SearchParams.superstructure();
            } else if (type.startsWith("sim")) {
                params = SearchParams.similarity();
                if (cutoff != null) {
                    try {
                        params.setSimilarity(cutoff);
                    } catch (NumberFormatException e) {
                        throw new BadRequestException("Bogus similarity value specified");
                    }
                } else
                    throw new BadRequestException("If similarity search is requested must specify the cutoff");
            } else if (type.startsWith("exact")) {
                params = SearchParams.exact();
            } else {
                params = SearchParams.substructure();
            }
        } else {
            params = SearchParams.substructure();
        }

        if (rankBy != null) params.setRankBy(rankBy);
        System.out.println("## structure search: query:"
                + query + " type:" + type + " rank:" + rankBy);

        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        if (skip == -1) skip = 0;
        if (top == -1) top = 100;

        OrderedSearchResultHandler handler =
                new OrderedSearchResultHandler(params, pw, skip, top);
        if (countRequested) {
            int n = search.count(query, params);
            response = Response.ok(String.valueOf(n)).build();
        } else {
            handler.start(threadPool);
            search.search(query, params, handler);
            handler.complete();

            // TODO we should be directly getting a List of cid's rather than parsing a string
            String cidsStr = writer.getBuffer().toString();
            String[] cidStrs = cidsStr.split("\n");
            List<Long> cids = new ArrayList<Long>();
            Map<Long, String> highlights = new HashMap<Long, String>();
            for (String cidstr : cidStrs) {
                String[] toks = cidstr.split("\t");
                String hl = null;
                if (toks.length == 2) {
                    cidstr = toks[0];
                    hl = toks[1];
                }

                if (cidstr.equals("")) continue;
                Long cid = Long.parseLong(cidstr);

                // TODO there should be a faster way to search only within the probe subset
                if (probeSubset) {
                    Compound c = db.getCompoundsByCid(cid).get(0);
                    if (c.getProbeId() == null) continue;
                }

                // see if we should keep the cid, based on absence/presence of annotations
                if (annot != null && annot.toLowerCase().equals("true")) {
                    if (db.getCompoundAnnotations(cid).get("anno_key").length > 0) cids.add(cid);
                } else cids.add(cid);

                if (hl != null) {
                    highlights.put(cid, hl);
                }
            }

            Long[] ids = cids.toArray(new Long[0]);
            EntityTag etag = newETag(db, ids);

//                List<Long> cids = handler.getCids();
            if (expandEntries) {
                List<Compound> cs = db.getCompoundsByCid(ids);
                for (Compound c : cs) {
                    String hl = highlights.get(c.getCid());
                    c.setHighlight(hl);
                }
                response = Response.ok(Util.toJson(cs), MediaType.APPLICATION_JSON).tag(etag).build();
            } else {
                List<String> paths = new ArrayList<String>();
                for (Long cid : cids) {
                    Compound c = new Compound();
                    c.setCid(cid);
                    paths.add(c.getResourcePath());
                }
                String json = Util.toJson(paths);
                response = Response.ok(json, MediaType.APPLICATION_JSON).header("content-length", json.length()).tag(etag).build();
            }
        }
        return response;
    }


    private Response getCompoundResponse(String id, String type, List<MediaType> mediaTypes, boolean expand) throws SQLException, IOException {

        List<String> validTypes = Arrays.asList("cid", "sid", "probeid", "name");

        boolean isIdList = id.contains(",");

        if (!validTypes.contains(type)) return null;
        List<Compound> c = new ArrayList<Compound>();

        try {
            if (!isIdList) {
                if (type.equals("cid") || type.equals("sid")) {
                    try {
                        Long.parseLong(id);
                    } catch (NumberFormatException e) {
                        throw new BadRequestException
                                ("Invalid format for a CID or SID: " + id);
                    }
                }

                if (type.equals("cid")) c.addAll(db.getCompoundsByCid(Long.parseLong(id)));
                else if (type.equals("probeid")) c.addAll(db.getCompoundsByProbeId(id));
                else if (type.equals("sid")) c.addAll(db.getCompoundsBySid(Long.parseLong(id)));
                else if (type.equals("name")) c.addAll(db.getCompoundsByName(id));
            } else {
                String[] s = id.split(",");
                if (type.equals("cid") || type.equals("sid")) {
                    Long[] ids = new Long[s.length];
                    for (int i = 0; i < s.length; i++) ids[i] = Long.parseLong(s[i].trim());
                    if (type.equals("cid")) c.addAll(db.getCompoundsByCid(ids));
                    else if (type.equals("sid")) c.addAll(db.getCompoundsBySid(ids));
                } else if (type.equals("probeid")) c.addAll(db.getCompoundsByProbeId(s));
                else if (type.equals("name")) c.addAll(db.getCompoundsByProbeId(s));
            }

            if (c.size() == 0) throw new WebApplicationException(404);
            if (countRequested) return Response.ok(String.valueOf(c.size()), MediaType.TEXT_PLAIN).build();

            Long[] ids = new Long[c.size()];
            EntityTag etag = newETag(db, ids);

            if (mediaTypes.contains(BARDConstants.MIME_SMILES)) {
                StringBuilder s = new StringBuilder();
                for (Compound ac : c) s.append(ac.getSmiles() + "\t" + ac.getCid());
                return Response.ok(s, BARDConstants.MIME_SMILES)
                        .tag(etag).build();
            } else if (mediaTypes.contains(BARDConstants.MIME_SDF)) {   // TODO handle multi-molecule SDFs
                throw new WebApplicationException(406);
                //            Molecule mol = MolImporter.importMol(c.getSmiles());
                //            mol.setProperty("cid", String.valueOf(c.getCid()));
                //            mol.setProperty("probeId", c.getProbeId());
                //            mol.setProperty("url", c.getUrl());
                //            mol.setProperty("resourecePath", c.getResourcePath());
                //            String sdf = mol.exportToFormat("sdf");
                //            return Response.ok(sdf, BARDConstants.MIME_SDF).build();
            } else {
                String json;
                ObjectMapper mapper = new ObjectMapper();
                if (!type.equals("name") && c.size() == 1) {
                    if (type.equals("probeid")) {
                        List<Project> projects = db.getProjectByProbeId(c.get(0).getProbeId());
                        ArrayNode anode = mapper.createArrayNode();
                        ObjectNode onode = mapper.valueToTree(c.get(0));
                        if (projects.size() == 1) {
                            onode.put("bardProjectId", projects.get(0).getBardProjectId());
                            onode.put("capProjectId", projects.get(0).getCapProjectId());
                        } else if (projects.size() == 0) {
                            onode.put("bardProjectId", -1);
                            onode.put("capProjectId", -1);
                        }
                        anode.add(onode);
                        json = mapper.writeValueAsString(anode);
                    } else json = Util.toJson(c);
                } else {
                    if (expand) {
                        json = toJson(db, c, type, false);
                    } else {
                        List<String> links = new ArrayList<String>();
                        for (Compound ac : c) links.add(ac.getResourcePath());
                        json = Util.toJson(links);
                    }
                }

                return Response.ok(json, MediaType.APPLICATION_JSON)
                        .tag(etag).build();
            }
        } finally {
            db.closeConnection();
        }
    }

    EntityTag newETag(DBUtils db, Long... ids) throws SQLException {
        EntityTag etag = new EntityTag
                (db.newETag(getRequestURI(), Compound.class.getName()));

        int cnt = db.putETag(etag.getValue(), ids);
        log("** ETag: " + etag.getValue() + " " + cnt);

        List<String> parents = new ArrayList<String>();
        for (EntityTag e : getETagsRequested()) {
            cnt = db.putETag(e.getValue(), ids);
            parents.add(e.getValue());
            log(" ** Updating " + e.getValue() + ": " + cnt);
        }

        if (!parents.isEmpty()) {
            db.createETagLinks(etag.getValue(),
                    parents.toArray(new String[0]));
        }
        return etag;
    }

    String toJson(DBUtils db, List<Compound> compounds, String type,
                  boolean annotation) throws SQLException, IOException {

        if (!annotation && !type.equals("probeid")) {
            return Util.toJson(compounds);
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode node = (ArrayNode) mapper.valueToTree(compounds);
        for (int i = 0; i < node.size(); ++i) {
            ObjectNode n = (ObjectNode) node.get(i);

            if (type.equals("probeid")) {
                String probeId = node.get("probeId").asText();
                List<Project> projects = db.getProjectByProbeId(probeId);
                if (projects.size() == 1) {
                    if (annotation) n.put("bardProjectId", mapper.valueToTree(projects.get(0)));
                    else n.put("bardProjectId", projects.get(0).getBardProjectId());
                } else logger.warning("There were " + projects.size() + " projects for probe " + probeId);
            }

            Map anno = db.getCompoundAnnotations(n.get("cid").asLong());
            if (anno.isEmpty()) {
                n.putNull("anno_key");
                n.putNull("anno_val");
            } else {
                for (Object key : anno.entrySet()) {
                    Map.Entry me = (Map.Entry) key;
                    n.putPOJO((String) me.getKey(), me.getValue());
                }
            }
        }
        return mapper.writeValueAsString(node);
    }

    @GET
    @Path("/{cid}/image")
    public Response getImage(@PathParam("cid") String resourceId,
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

    @GET
    @Path("/{cid}/{format}")
    public Response getImage(@PathParam("cid") String resourceId,
                             @PathParam("format") String format) {
        try {
            MoleculeService molsrv =
                    (MoleculeService) Util.getMoleculeService();
            Molecule molecule = molsrv.getMol(resourceId);
            if (molecule == null)
                throw new NotFoundException
                        ("No molecule for CID = " + resourceId);

            String molstr = "";
            String type = "text/plain";
            if (format.startsWith("mol")) {
                molstr = molecule.toFormat("mol");
                type = "chemical/x-mdl-mol";
            } else if (format.startsWith("sdf")) {
                molstr = molecule.toFormat("sdf");
                type = "chemical/x-mdl-sdffile";
            } else if (format.startsWith("smi")) {
                molstr = molecule.toFormat("smiles:q");
                type = "chemical/x-daylight-smiles";
            }
            return Response.ok(molstr).type(type).build();
        } catch (Exception e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{hash}/h{id}")
    public Response getCompoundsByHash(@PathParam("hash") String hv,
                                       @PathParam("id") String id,
                                       @QueryParam("expand") String expand) {

        
        try {
            if (id == null || id.length() == 0) {
                throw new IllegalArgumentException("No hash id specified!");
            }

            if ((hv.length() % 6) != 0) {
                // not multiple of 6
                throw new IllegalArgumentException
                        ("Hash value is not multiple of 6");
            }

            /*
             * {hash} is the hash value which can be of length 
             * 6, 12, 18, or 24 depending on {id}.
             * {id} must can be any combination of {1,2,3,4}; e.g.,
             * /Gwdyar/h1
             * /GwdyarAykhoO/h13
             * /AykhoOGwdyar/h31
             * /GwdyarvpREqoAykhoO/h123
             * /GwdyarvpREqoAykhoOoTARYx/h1234
             */
            String[] hash = new String[4];
            for (int i = 0, pos = 0;
                 i < id.length() && pos < hv.length(); ++i) {
                String key = hv.substring(pos, pos + 6);

                switch (id.charAt(i)) {
                    case '1':
                        hash[0] = key;
                        break;
                    case '2':
                        hash[1] = key;
                        break;
                    case '3':
                        hash[2] = key;
                        break;
                    case '4':
                        hash[3] = key;
                        break;
                    default:
                        // ignore bogus character
                }
                pos += 6;
            }

            log("{hash} = " + hv + "; {id} = " + id + "; h1=" + hash[0] + " h2=" + hash[1]
                    + " h3=" + hash[2] + " h4=" + hash[3]);

            List<Compound> compounds =
                    db.getCompoundsByHash(hash[0], hash[1], hash[2], hash[3]);
            Response response;
            if (expand != null && (expand.equalsIgnoreCase("true")
                    || expand.equalsIgnoreCase("yes"))) {
                response = Response.ok(Util.toJson(compounds),
                        MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Compound a : compounds)
                    links.add(a.getResourcePath());
                response = Response.ok(Util.toJson(links),
                        MediaType.APPLICATION_JSON).build();
            }

            return response;
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

    @GET
    @Path("/{cid}/eqv")
    public Response getEqvCompounds(@PathParam("cid") String cid,
                                    @QueryParam("expand") String expand) {
        
        // return equivalence class based on hash keys
        try {
            List<Compound> compounds =
                    db.getEqvCompounds(Long.parseLong(cid));

            Response response;
            if (expand != null && (expand.equalsIgnoreCase("true")
                    || expand.equalsIgnoreCase("yes"))) {
                response = Response.ok(Util.toJson(compounds),
                        MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Compound a : compounds)
                    links.add(a.getResourcePath());
                response = Response.ok(Util.toJson(links),
                        MediaType.APPLICATION_JSON).build();
            }

            return response;
        } catch (Exception ex) {
            throw new WebApplicationException(ex, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
            }
        }
    }

    @GET
    @Path("/{cid}")
    public Response getResources(@PathParam("cid") String resourceId,
                                 @QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand) {
        try {
            Response response = getCompoundResponse(resourceId, "cid", headers.getAcceptableMediaTypes(), expand != null && expand.toLowerCase().equals("true"));
            return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getResources(@FormParam("ids") String cids, @QueryParam("expand") String expand) {
        try {

            if (cids == null)
                throw new WebApplicationException(new Exception("POST request must specify the cids form parameter, which should be a comma separated string of CIDs"), 400);
            Response response = getCompoundResponse(cids, "cid", headers.getAcceptableMediaTypes(), expand != null && expand.toLowerCase().equals("true"));
            return response;
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
            List<Compound> c = db.getCompoundsByETag
                    (skip != null ? skip : -1, top != null ? top : -1, resourceId);
            String json = toJson(db, c, "", expand != null && expand.toLowerCase().equals("true"));

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

    @GET
    @Path("/etag/{etag}/assays")
    public Response getAssaysByETag(@PathParam("etag") String resourceId,
                                    @QueryParam("filter") String filter,
                                    @QueryParam("expand") String expand,
                                    @QueryParam("skip") Integer skip,
                                    @QueryParam("top") Integer top) {
        
        try {
            List<Compound> c = db.getCompoundsByETag
                    (skip != null ? skip : -1, top != null ? top : -1, resourceId);

            Map<Long, List> ret = new HashMap<Long, List>();
            for (Compound ac : c) {
                Long cid = ac.getCid();

                List<Assay> p;
                if (filter == null || !filter.trim().toLowerCase().equals("active"))
                    p = db.getEntitiesByCid(cid, Assay.class, 0, -1);
                else p = db.getEntitiesByActiveCid(cid, Assay.class, 0, -1);

                if (p == null) p = new ArrayList<Assay>();
                if (expandEntries(expand)) ret.put(cid, p);
                else {
                    List<String> links = Functional.Apply(p, new IApplyFunction<Assay, String>() {
                        public String eval(Assay assay) {
                            return assay.getResourcePath();
                        }
                    });
                    ret.put(cid, links);
                }
            }
            String json = Util.toJson(ret);
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

    @Override
    @GET
    @Path("/etag/{etag}/facets")
    public Response getFacets(@PathParam("etag") String resourceId) {
        
        try {
            List<Facet> facets = db.getCompoundFacets(resourceId);
            return Response.ok(Util.toJson(facets),
                    MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            throw new WebApplicationException(ex, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @GET
    @Path("/sid/{sid}")
    public Response getCompoundBySid(@PathParam("sid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            Response response = getCompoundResponse(resourceId, "sid", headers.getAcceptableMediaTypes(), expand != null && expand.toLowerCase().equals("true"));
            return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @POST
    @Path("/sid/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getCompoundBySid(@FormParam("sids") String sids, @QueryParam("expand") String expand) {
        try {
            if (sids == null)
                throw new WebApplicationException(new Exception("POST request must specify the sids form parameter, which should be a comma separated string of SIDs"), 400);
            Response response = getCompoundResponse(sids, "sid", headers.getAcceptableMediaTypes(), expand != null && expand.toLowerCase().equals("true"));
            return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/probeid/{pid}")
    public Response getCompoundByProbeid(@PathParam("pid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            Response response = getCompoundResponse(resourceId, "probeid", headers.getAcceptableMediaTypes(), expand != null && expand.toLowerCase().equals("true"));
            return response;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebApplicationException(e, 500);
        }
    }

    @POST
    @Path("/probeid/{pid}")
    @Consumes("application/x-www-form-urlencoded")
    public Response getCompoundByProbeid(@FormParam("pids") String pids, @QueryParam("expand") String expand) {
        try {
            if (pids == null)
                throw new WebApplicationException(new Exception("POST request must specify the pids form parameter, which should be a comma separated string of probe id's"), 400);
            Response response = getCompoundResponse(pids, "probeid", headers.getAcceptableMediaTypes(), expand != null && expand.toLowerCase().equals("true"));
            return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/name/{name}")
    public Response getCompoundByName(@PathParam("name") String name, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            Response response = getCompoundResponse(name, "name", headers.getAcceptableMediaTypes(), expand != null && expand.toLowerCase().equals("true"));
            return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @POST
    @Path("/name/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getCompoundByNameList(@FormParam("names") String names, @QueryParam("expand") String expand) {
        if (names == null || names.trim().equals("")) throw new WebApplicationException(400);
        String[] toks = names.trim().split(",");
        Map<String, List<Compound>> map = new HashMap<String, List<Compound>>();
        
        Response response = null;
        try {
            for (String tok : toks) map.put(tok.trim(), db.getCompoundsByName(tok.trim()));
            db.closeConnection();
            if (expandEntries(expand)) response = Response.ok(Util.toJson(map), MediaType.APPLICATION_JSON).build();
            else {
                Map<String, List<String>> lmap = new HashMap<String, List<String>>();
                for (String key : map.keySet()) {
                    List<Compound> compounds = map.get(key);
                    List<String> links = new ArrayList<String>();
                    for (Compound c : compounds) links.add(c.getResourcePath());
                    lmap.put(key, links);
                }
                response = Response.ok(Util.toJson(lmap), MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
        return response;
    }

    @GET
    @Path("/{cid}/projects")
    public Response getProjectsForCompound(@PathParam("cid") Long cid,
                                           @QueryParam("expand") String expand,
                                           @QueryParam("skip") Integer skip,
                                           @QueryParam("top") Integer top) throws SQLException, IOException {
        
        Response response;
        String linkString = null;

        if (top == null || top == -1) { // top was not specified, so we start from the beginning
            top = BARDConstants.MAX_DATA_COUNT;
        }
        if (skip == null || skip == -1) skip = 0;

        List<Project> p = db.getEntitiesByCid(cid, Project.class, skip, top);
        if (countRequested) response = Response.ok(String.valueOf(p.size())).type(MediaType.TEXT_PLAIN).build();

        if (p.size() > BARDConstants.MAX_DATA_COUNT) {
            String expandClause = "expand=false";
            if (expandEntries(expand)) expandClause = "expand=true";
            if (skip + top <= p.size())
                linkString = BARDConstants.API_BASE + "/compounds/" + cid + "/projects?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
        }

        if (!expandEntries(expand)) {
            List<String> links = Functional.Apply(p, new IApplyFunction<Project, String>() {
                public String eval(Project project) {
                    return project.getResourcePath();
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

    @GET
    @Path("/{cid}/annotations")
    public Response getCompoundAnnotations(@PathParam("cid") Long cid)
            throws SQLException, IOException {
        
        try {
            Map anno = db.getCompoundAnnotations(cid);
            return Response.ok(Util.toJson(anno))
                    .type(MediaType.APPLICATION_JSON).build();
        } finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{cid}/sids")
    public Response getSidsForCompound(@PathParam("cid") Long cid)
            throws SQLException, IOException {
        
        try {
            List<Long> sids = db.getSidsByCid(cid);
            return Response.ok(Util.toJson(sids))
                    .type(MediaType.APPLICATION_JSON).build();
        } finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{cid}/synonyms")
    public Response getSynonymsForCompound(@PathParam("cid") Long cid)
            throws SQLException, IOException {
        
        try {
            List<String> syns = db.getCompoundSynonyms(cid);
            return Response.ok(Util.toJson(syns))
                    .type(MediaType.APPLICATION_JSON).build();
        } finally {
            db.closeConnection();
        }
    }

    @POST
    @Path("/assays")
    @Consumes("application/x-www-form-urlencoded")
    public Response getAssaysForCompounds(@FormParam("ids") String ids,
                                          @QueryParam("expand") String expand,
                                          @QueryParam("filter") String filter,
                                          @QueryParam("skip") Integer skip,
                                          @QueryParam("top") Integer top) throws SQLException, IOException {
        
        Response response;
        String linkString = null;

        String[] toks = ids.split(",");
        Long[] cids = new Long[toks.length];
        for (int i = 0; i < toks.length; i++) cids[i] = Long.parseLong(toks[i].trim());

        Map<Long, List> ret = new HashMap<Long, List>();
        for (Long cid : cids) {
            List<Assay> p;

            if (filter == null || !filter.trim().toLowerCase().equals("active"))
                p = db.getEntitiesByCid(cid, Assay.class, skip, top);
            else p = db.getEntitiesByActiveCid(cid, Assay.class, skip, top);

            if (p == null) p = new ArrayList<Assay>();
            if (expandEntries(expand)) ret.put(cid, p);
            else {
                List<String> links = Functional.Apply(p, new IApplyFunction<Assay, String>() {
                    public String eval(Assay assay) {
                        return assay.getResourcePath();
                    }
                });
                ret.put(cid, links);
            }
        }

        response = Response.ok(Util.toJson(ret)).type(MediaType.APPLICATION_JSON).build();
        db.closeConnection();
        return response;
    }


    @GET
    @Path("/{cid}/assays")
    public Response getAssaysForCompound(@PathParam("cid") Long cid,
                                         @QueryParam("expand") String expand,
                                         @QueryParam("filter") String filter,
                                         @QueryParam("skip") Integer skip,
                                         @QueryParam("top") Integer top) throws SQLException, IOException {
        
        Response response;
        String linkString = null;
        List<Assay> p;

        if (filter == null || !filter.trim().toLowerCase().equals("active"))
            p = db.getEntitiesByCid(cid, Assay.class, skip, top);
        else p = db.getEntitiesByActiveCid(cid, Assay.class, skip, top);

        if (countRequested) return Response.ok(String.valueOf(p.size())).type(MediaType.TEXT_PLAIN).build();
        if (top == null) top = -1;
        if (skip == null) skip = -1;
        if (p.size() > BARDConstants.MAX_DATA_COUNT) {
            if (top == -1) { // top was not specified, so we start from the beginning
                top = BARDConstants.MAX_DATA_COUNT;
            }
            if (skip == -1) skip = 0;
            String expandClause = "expand=false";
            if (expandEntries(expand)) expandClause = "expand=true";
            if (skip + top <= p.size())
                linkString = BARDConstants.API_BASE + "/compounds/" + cid + "/assays?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
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


    // return all experiment data for this CID
    @GET
    @Path("/{cid}/exptdata")
    public Response getExperimentData(@PathParam("cid") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {

        
        String linkString = null;

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        try {
            List<String> edids = db.getCompoundDataIds(Long.valueOf(resourceId), skip, top, filter);

//            // set up skip and top params
//            if (experiemnt.getSubstances() > BARDConstants.MAX_DATA_COUNT) {
//                if ((top == -1)) { // top was not specified, so we start from the beginning
//                    top = BARDConstants.MAX_DATA_COUNT;
//                }
//                if (skip == -1) skip = 0;
//                String expandClause = "expand=false";
//                if (expandEntries(expand)) expandClause = "expand=true";
//                if (skip + top <= experiemnt.getSubstances())
//                    linkString = BARDConstants.API_BASE + "/compounds/" + resourceId + "/exptdata?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
//            }

            String json;
            if (!expandEntries(expand)) {
                if (countRequested) json = String.valueOf(edids.size());
                else {
                    List<String> links = Functional.Apply(edids, new IApplyFunction<String, String>() {
                        public String eval(String aString) {
                            ExperimentData ed = new ExperimentData();
                            ed.setExptDataId(aString);
                            return ed.getResourcePath();
                        }
                    });
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                }
            } else {
                List<ExperimentData> data = db.getExperimentDataByDataId(edids);
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
    @Path("/{cid}/experiments")
    public Response getExperiments(@PathParam("cid") String resourceId,
                                   @QueryParam("filter") String filter,
                                   @QueryParam("expand") String expand,
                                   @QueryParam("skip") Integer skip,
                                   @QueryParam("top") Integer top) {
        
        String linkString = null;

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        try {
            // set up skip and top params
            if ((top == -1)) { // top was not specified, so we start from the beginning
                top = BARDConstants.MAX_DATA_COUNT;
            }
            if (skip == -1) skip = 0;
            String expandClause = "expand=false";
            if (expandEntries(expand)) expandClause = "expand=true";
            linkString = BARDConstants.API_BASE + "/compounds/" + resourceId + "/experiments?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;

            String json;
            List<Experiment> data;
            if (filter == null || !filter.trim().toLowerCase().equals("active"))
                data = db.getEntitiesByCid(Long.valueOf(resourceId), Experiment.class, skip, top);
            else data = db.getEntitiesByActiveCid(Long.valueOf(resourceId), Experiment.class, skip, top);

            if (!expandEntries(expand)) {
                List<Long> eids = new ArrayList<Long>();
                for (Experiment expt : data) eids.add(expt.getBardExptId());
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

    private Map<String, Integer> getTargetClassCount(List<String> acc, int level) throws SQLException {
        
        List<String> tclasses = new ArrayList<String>(); //db.getChemblTargetClasses(acc, level);
        for (String anAcc : acc) {
            List<TargetClassification> pclasses = db.getPantherClassesForAccession(anAcc);
            for (TargetClassification pclass : pclasses) tclasses.add(pclass.getName());
        }
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (String tclass : tclasses) {
            if (tclass == null) continue;
            if (map.containsKey(tclass)) map.put(tclass, map.get(tclass) + 1);
            else map.put(tclass, 0);
        }
        db.closeConnection();
        return map;
    }

    @GET
    @Path("/{cid}/summary")
    public Response getSummary(@PathParam("cid") Long cid,
                               @QueryParam("expand") String expand,
                               @QueryParam("skip") Integer skip,
                               @QueryParam("top") Integer top) throws IOException, SQLException {
        Map<String, Object> s = new HashMap<String, Object>();
        

        if (skip == null) skip = -1;
        if (skip == null) skip = -1;
        if (top == null) top = -1;

        List<ExperimentData> data = db.getEntitiesByCid(cid, ExperimentData.class, skip, top);

        // make an array of experiment ids then pull all expts
        // in bulk and set up local cache
        List<Long> tmpEids = new ArrayList<Long>();
        for (ExperimentData edata : data) {
            if (edata != null) tmpEids.add(edata.getBardExptId());
        }
        List<Experiment> experiments = db.getExperimentsByExptIds(tmpEids.toArray(new Long[0]));
        Map<Long, Experiment> localExptCache = new HashMap<Long, Experiment>();
        for (Experiment expt : experiments) localExptCache.put(expt.getBardExptId(), expt);

        // do the same thing for the assays
        List<Long> tmpAids = new ArrayList<Long>();
        for (Experiment expt : experiments) {
            if (expt.getBardAssayId() != null) tmpAids.add(expt.getBardAssayId());
        }
        List<Assay> assays = db.getAssays(tmpAids.toArray(new Long[0]));
        Map<Long, Assay> localAssayCache = new HashMap<Long, Assay>();
        for (Assay assay : assays) localAssayCache.put(assay.getBardAssayId(), assay);

        List<ExperimentData> hitData = new ArrayList<ExperimentData>();
        int nhit = 0;
        List<String> hitExpts = new ArrayList<String>();
        Set<Assay> hitAssays = new HashSet<Assay>();

        Set<Assay> testedAssays = new HashSet<Assay>();
        List<Experiment> testedExperiments = new ArrayList<Experiment>();

        for (ExperimentData ed : data) {
            if (ed == null) {
                logger.warning("Should not have a null ExperimentData object for compound " + cid + ". Skipping");
                continue;
            }

            Long eid = ed.getBardExptId();
            Experiment expt = localExptCache.get(eid);
            if (expt == null) continue;
            Long aid = expt.getBardAssayId();

            testedExperiments.add(expt);
            if (aid != null) {
                Assay assay = localAssayCache.get(aid);
                testedAssays.add(assay);

                // if cid was active in experiment_data (outcome = 2) and experiment was a confirmatory screen, we call it a hit
                if (ed.getOutcome() == 2) {
                    nhit++;
                    hitExpts.add(expt.getResourcePath());
                    hitAssays.add(assay);
                    hitData.add(ed);
                }
            } else {
                logger.warning("Something is rotten in the state of Denmark! "
                        + "No assay found for eid=" + eid + " exptid="
                        + ed.getExptDataId() + " bardexptid="
                        + ed.getBardExptId() + "!");
            }
        }

        s.put("ntest", data.size());
        s.put("nhit", nhit);
//        s.put("hitExperiments", hitExpts);
        s.put("hitAssays", hitAssays);

        s.put("testedExptdata", data);
        s.put("hitExptdata", hitData);

        // get target class counts
        List<String> accs = new ArrayList<String>();
        for (Assay a : testedAssays) {
            List<Biology> bios = db.getBiologyByEntity("assay", a.getBardAssayId());
            for (Biology bio : bios) {
                if (bio.getBiology().equals(Biology.BiologyType.PROTEIN) && bio.getDictId() == 1398) // Uniprot ID
                    accs.add(bio.getExtId());
            }
        }

        if (accs.size() > 0)
            s.put("testedTargets", accs);
        else s.put("testedTarget", null);

        accs = new ArrayList<String>();
        for (Assay a : hitAssays) {
            List<Biology> bios = db.getBiologyByEntity("assay", a.getBardAssayId());
            for (Biology bio : bios) {
                if (bio.getBiology().equals(Biology.BiologyType.PROTEIN) && bio.getDictId() == 1398) // Uniprot ID
                    accs.add(bio.getExtId());
            }
        }
        if (accs.size() > 0)
            s.put("hitTarget", accs);
        else s.put("hitTarget", null);


        if (expand != null && expand.trim().toLowerCase().equals("true")) {
//            s.put("testedExperiments", testedExperiments);
            s.put("testedAssays", testedAssays);
            s.put("hitAssays", hitAssays);
        } else {
            List<String> l = new ArrayList<String>();
            for (Experiment e : testedExperiments) {
                if (e.getBardExptId() != null)
                    l.add(e.getResourcePath());
            }
//            s.put("testedExperiments", l);
            l = new ArrayList<String>();
            for (Assay a : testedAssays) {
                if (a != null) l.add(a.getResourcePath());
                else logger.warning("Should not have a null assay for compound " + cid + ". Skipping");
            }
            s.put("testedAssays", l);

            l = new ArrayList<String>();
            for (Assay a : hitAssays) {
                if (a != null) l.add(a.getResourcePath());
                else logger.warning("Should not have a null assay for compound " + cid + ". Skipping");
            }
            s.put("hitAssays", l);
        }
        db.closeConnection();
        return Response.ok(Util.toJson(s), MediaType.APPLICATION_JSON).build();

    }
}