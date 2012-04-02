package gov.nih.ncgc.bard.rest;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/v1/projects")
public class MLBDProjectResource implements IMLBDResource {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;
    @Context
    HttpHeaders headers;

    @GET
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        return getResources(null, filter, search, expand);
    }

    @GET
    @Path("/{name}")
    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        String ret;

        if (resourceId == null) {
            Element root = new Element("projectList");
            for (int i = 0; i < 10; i++) {
                Element e = new Element("project");
                e.addAttribute(new Attribute("assayCount", "10"));
                e.addAttribute(new Attribute("probeCount", "2"));
                root.appendChild(e);
            }
            ret = (new Document(root)).toXML();
            return Response.ok(ret, "text/xml").build();
        } else {
            Element root = new Element("project");
            root.addAttribute(new Attribute("assayCount", "10"));
            root.addAttribute(new Attribute("probeCount", "2"));
            ret = (new Document(root)).toXML();

        }
        return Response.ok(ret, "text/xml").build();
    }

    /**
     * Return compounds for a project.
     *
     * @param resourceId
     * @param filter
     * @param search
     * @param expand
     * @return String representation of compounds. Format is specified via Accepts: header and can be
     *         chemical/x-daylight-smiles or chemical/x-mdl-sdfile for SMILES or SDF formats.
     * @throws MolFormatException
     */
    @GET
    @Path("/{name}/compounds")
    public Response getCompoundsForProject(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        String ret = "";
        List<MediaType> types = headers.getAcceptableMediaTypes();

        Molecule mol;
        try {
            mol = MolImporter.importMol("C1CCCCC1");
        } catch (MolFormatException e) {
            throw new WebApplicationException(e, 500);
        }
        mol.setName("molid1");
        if (types.contains(MLBDConstants.MIME_SMILES) || types.contains(MediaType.TEXT_HTML)) {
            ret = mol.toFormat("smiles");
        } else if (types.contains(MLBDConstants.MIME_SDF)) {
            ret = mol.toFormat("sdf");
        }
        Response.ResponseBuilder builder = Response.ok(ret, types.get(0));
        return builder.build();
    }
}
