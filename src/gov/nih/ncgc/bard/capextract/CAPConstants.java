package gov.nih.ncgc.bard.capextract;

import java.io.IOException;

/**
 * Useful constants for interacting with the CAP
 *
 * @author Rajarshi Guha
 */
public class CAPConstants {

    private static CAPDictionary dictionary = null;

    public static final String CAP_ROOT_MIMETYPE = "application/vnd.bard.cap+xml;type=bardexport";
    public static final String CAP_APIKEY_HEADER = "APIKEY";
    public static final String CAP_STATUS_READY = "Ready";
    public static final String CAP_STATUS_STARTED = "Started";
    public static final String CAP_STATUS_COMPLETE = "Complete";
    //solr resource keys
    public static final String SOLR_CORE_URL_KEY = "SOLR_SERVER_URL";
    public static final String SOLR_RESOURCE_KEY_ASSAY = "SOLR_ASSAY_CORE";
    public static final String SOLR_RESOURCE_KEY_PROJECT = "SOLR_PROJECT_CORE";
    public static final String SOLR_RESOURCE_KEY_EXPERIMENT = "SOLR_EXPERIMENT_CORE";
    public static final String SOLR_RESOURCE_KEY_COMPOUND = "SOLR_COMPOUND_CORE";
        
    /**
     * Get the API key.
     * <p/>
     * This shoud be defined via a system property during startup. If using the command line
     * use -DCAP_API_KEY=your-api-key. If using Tomcat, set it via an init-param.
     *
     * @return the API key required for all CAP API requests
     */
    public static String getApiKey() {
        return System.getProperty("CAP_API_KEY");
    }
    
    public static String getBardScratchDir() {
	return System.getProperty("BARD_SCRATCH_DIR");
    }
    
    public static String getBardDBJDBCUrl() {
	return System.getProperty("BARD_DB_URL");
    }

    public static String getBardBroadcastIPList() {
	return System.getProperty("BARD_BROADCAST_IP_LIST");
    }
    
    public static String getCAPRoot() {
	String capRoot = System.getProperty("CAP_ROOT");
	//if not specified, default to production CAP export
	if(capRoot == null)
	    capRoot = "https://bard.broadinstitute.org/dataExport/api";
	return capRoot;
   }
    
    /**
     * Returns the solr url with specified core. The solr core key should be specified using
     * CAPConstants static solr keys in the general form SOLR_RESOURCE_KEY_<entity> where
     * entity is one of 'ASSAY', 'PROJECT', 'EXPERIMENT', or 'COMPOUND' (or similar indexed entities).
     * @param solrCoreKey A string specifying the solr entity core (described in method description)
     * @return
     */
    public static String getSolrURL(String solrCoreKey) {
	String baseUrl = System.getProperty(SOLR_CORE_URL_KEY);
	String core;
	//if we have the base url, append the core
	if(baseUrl != null) {
	    core = System.getProperty(solrCoreKey);
	    if(core != null)
		baseUrl += core;
	    else
		baseUrl = null; //if we don't have the core, no reason to send base url
	}
	return baseUrl;
    }
    
    public static enum CapResource {
        BARDEXPORT("application/vnd.bard.cap+xml;type=bardexport"),
        DICTIONARY("application/vnd.bard.cap+xml;type=dictionary"),
        RESULT_TYPE("application/vnd.bard.cap+xml;type=resultType"),
        ELEMENT("application/vnd.bard.cap+xml;type=element"),
        STAGE("application/vnd.bard.cap+xml;type=stage"),
        ASSAYS("application/vnd.bard.cap+xml;type=assays"),
        ASSAY("application/vnd.bard.cap+xml;type=assay"),
        PROJECTS("application/vnd.bard.cap+xml;type=projects"),
        PROJECT("application/vnd.bard.cap+xml;type=project"),
        EXPERIMENTS("application/vnd.bard.cap+xml;type=experiments"),
        EXPERIMENT("application/vnd.bard.cap+xml;type=experiment"),
        RESULTS("application/vnd.bard.cap+xml;type=results"),
        RESULT("application/vnd.bard.cap+xml;type=result"),
        RESULT_JSON("application/vnd.bard.cap+json;type=results"),        
        ASSAYDOC("application/vnd.bard.cap+xml;type=assayDoc"),
        PROJECTDOC("application/vnd.bard.cap+xml;type=projectDoc"),
        EXTREF("application/vnd.bard.cap+xml;type=externalReference"),
        EXTSYS("application/vnd.bard.cap+xml;type=externalSystem");

        private String mimeType;

        private CapResource(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    /**
     * Get a CapResource based on a mime type string.
     *
     * @param m the mime type string
     * @return a CapResource if the specified mime type matches, otherwise null.
     */
    public static CapResource getResource(String m) {
        for (CapResource res : CapResource.values()) {
            if (res.mimeType.equals(m)) return res;
        }
        return null;
    }

    public static void setDictionary(CAPDictionary dict) {
        dictionary = dict;
    }

    public static CAPDictionary getDictionary() {
	if (dictionary == null) {
	    try {
		CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.DICTIONARY).
		poll(CAPConstants.getCAPRoot()+"/dictionary", CAPConstants.CapResource.DICTIONARY);
	    } catch (IOException e) {}
	}
        return dictionary;
    }
}
