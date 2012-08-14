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
    public static final MediaType MIME_PNG = new MediaType("image", "png");

    public static final String API_VERSION = "v1";
    public static final String API_BASE = "";
    public static final String API_EXTRA_PARAM_SPEC = "?filter=query_string[field]&expand=true|false&skip=N&top=M";

    /**
     * The maximum number of compounds to be returned by default.
     */
    public static final int MAX_COMPOUND_COUNT = 500;
    public static final int MAX_DATA_COUNT = 500;

    public static final String REQUEST_HEADER_COUNT = "x-count-entities";

}
