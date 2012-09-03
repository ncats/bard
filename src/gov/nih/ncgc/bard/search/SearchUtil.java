package gov.nih.ncgc.bard.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.TermsParams;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SearchUtil {

    /**
     * Extract query field and values from a general BARD filter query parameter.
     * <p/>
     * Solr filter queries are encoded as
     * <code>
     * fq(fieldName:fieldValue)
     * </code>
     * and multiple such specifications can be included as a comma separated list. <code>fieldValue</code>
     * can be an arbitrary string or a numeric range of the form<code>[low TO high]</code> with low and
     * high being numbers or <code>*</code>.
     * <p/>
     * Note that this method does not check whether the field names specified
     * are actually valid for a Solr document.
     *
     * @param filter The filter parameter string from a BARD request
     * @return A map whose keys are field names and values are field values.
     */
    public static List<String[]> extractFilterQueries(String filter) {
        List<String[]> ret = new ArrayList<String[]>();
        if (filter == null || filter.trim().equals("")) return ret;
        Pattern pattern = Pattern.compile("fq\\((.*?):(.*?)\\)");
        Matcher matcher = pattern.matcher(filter);
        while (matcher.find()) {
            for (int i = 1; i < matcher.groupCount(); i += 2) {
                String fname = matcher.group(i);
                String fvalue = matcher.group(i + 1).trim();
                ret.add(new String[]{fname, fvalue});
            }
        }
        return ret;
    }

//    public static List<String> getFieldNames(String lukeUrl) throws Exception {
//        List<String> fieldNames = new ArrayList<String>();
//        Client client = Client.create();
//        WebResource resource = client.resource(lukeUrl);
//        ClientResponse response = resource.get(ClientResponse.class);
//        int status = response.getStatus();
//        if (status != 200) {
//            throw new Exception("There was a problem querying " + lukeUrl);
//        }
//        String xml = response.getEntity(String.class);
//        Document doc = new Builder(false).build(xml, null);
//        Nodes nodes = doc.query("/response/lst[@name='fields']");
//        if (nodes.size() > 0) {
//            Node node = nodes.get(0);
//            for (int i = 0; i < node.getChildCount(); i++) {
//                Node n = node.getChild(i);
//                String name = ((Element) n).getAttribute("name").getValue();
//                if (name.endsWith("text")) continue;
//
//                // otherwise see if the field is of the desired type
//                Node tnode = n.query("str[@name='type']").get(0);
//                if (tnode.getValue().equals("tfloat") ||
//                        tnode.getValue().equals("tint") ||
//                        tnode.getValue().equals("date")) continue;
//                fieldNames.add(name);
//            }
//        }
//        client.destroy();
//        return fieldNames;
//    }


    /**
     *
     * @param url The Solr URL (including relevant core)
     * @param fields The fields to consider
     * @param q The query. It is assumed to be a complete regex
     * @param n The number of suggestions desired
     * @return
     * @throws MalformedURLException
     * @throws SolrServerException
     */
    public static Map<String, List<String>> getTerms(String url, SolrField[] fields, String q, Integer n) throws MalformedURLException, SolrServerException {
        SolrServer solr = new CommonsHttpSolrServer(url);
        SolrQuery query = new SolrQuery();
        query.setParam(CommonParams.QT, "/terms");
        query.setParam(TermsParams.TERMS, true);
        query.setParam(TermsParams.TERMS_LIMIT, String.valueOf(n));
        
        String[] fieldNames = new String[fields.length];
        for (int i = 0; i < fields.length; i++) fieldNames[i] = fields[i].getName();
        query.setParam(TermsParams.TERMS_FIELD, fieldNames);

        query.setParam(TermsParams.TERMS_REGEXP_FLAG, "case_insensitive");
        query.setParam(TermsParams.TERMS_REGEXP_STR, q);

        QueryResponse response = solr.query(query);
        TermsResponse termsr = response.getTermsResponse();

        Map<String, List<String>> termMap = new HashMap<String, List<String>>();
        for (SolrField field : fields) {
            List<TermsResponse.Term> terms = termsr.getTerms(field.getName());
            if (terms != null) {
                List<String> l = new ArrayList<String>();
                for (TermsResponse.Term term : terms) l.add(term.getTerm());
                if (l.size() > 0) termMap.put(field.getName(), l);
            }
        }
        return termMap;
    }

    public static void main(String[] args) throws Exception {
        SolrServer solr = new CommonsHttpSolrServer("http://protease.nhgri.nih.gov/servlet/solr/core-assay/");

        SolrQuery query = new SolrQuery();
        query.setParam(CommonParams.QT, "/terms");
        query.setParam(TermsParams.TERMS, true);
        query.setParam(TermsParams.TERMS_LIMIT, String.valueOf(10));
        query.setParam(TermsParams.TERMS_FIELD, "gobp_term");  // or whatever fields you want
        query.setParam(TermsParams.TERMS_REGEXP_FLAG, "case_insensitive");
        query.setParam(TermsParams.TERMS_REGEXP_STR, "dna re.*");

        QueryResponse response = solr.query(query);
        System.out.println("response = " + response);
        TermsResponse termsr = response.getTermsResponse();
        System.out.println("termsr = " + termsr);
        List<TermsResponse.Term> terms = termsr.getTerms("gobp_term");
        System.out.println("terms.size() = " + terms.size());
        for (TermsResponse.Term term : terms) System.out.println("term.getTerm() = " + term.getTerm());
    }


}
