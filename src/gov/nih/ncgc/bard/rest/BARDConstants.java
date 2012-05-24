package gov.nih.ncgc.bard.rest;

import javax.ws.rs.core.MediaType;

/**
 * Various useful constants.
 *
 * @author Rajarshi Guha
 */
public class BARDConstants {
    public static final MediaType MIME_SMILES = new MediaType("chemical", "x-daylight-smiles");
    public static final MediaType MIME_SDF = new MediaType("chemical", "x-mdl-sdfile");
    public static final MediaType MIME_MOL = new MediaType("chemical", "x-mdl-mol");

    public static final String API_VERSION = "v1";
    public static final String API_BASE = "/bard/rest/" + API_VERSION;
    public static final String API_EXTRA_PARAM_SPEC = "?search=query_string[field]&expand=true|false&skip=N&stop=M";

    /**
     * The maximum number of compounds to be returned by default.
     */
    public static final int MAX_COMPOUND_COUNT = 1000;
}
