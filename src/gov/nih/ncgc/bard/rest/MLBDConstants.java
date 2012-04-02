package gov.nih.ncgc.bard.rest;

import javax.ws.rs.core.MediaType;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class MLBDConstants {
    public static final MediaType MIME_SMILES = new MediaType("chemical", "x-daylight-smiles");
    public static final MediaType MIME_SDF = new MediaType("chemical", "x-mdl-sdfile");
    public static final MediaType MIME_MOL = new MediaType("chemical", "x-mdl-mol");
}
