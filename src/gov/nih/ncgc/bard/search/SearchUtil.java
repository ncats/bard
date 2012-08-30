package gov.nih.ncgc.bard.search;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
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

    public static List<String> getFieldNames(String lukeUrl) throws Exception {
        List<String> fieldNames = new ArrayList<String>();
        Client client = Client.create();
        WebResource resource = client.resource(lukeUrl);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        if (status != 200) {
            throw new Exception("There was a problem querying the Solr Luke API");
        }
        String xml = response.getEntity(String.class);
        Document doc = new Builder(false).build(xml, null);
        Nodes nodes = doc.query("/response/lst[@name='fields']");
        if (nodes.size() > 0) {
            Node node = nodes.get(0);
            for (int i = 0; i < node.getChildCount(); i++) {
                Node n = node.getChild(i);
                String name = ((Element) n).getAttribute("name").getValue();
                if (name.endsWith("text")) continue;

                // otherwise see if the field is of the desired type
                Node tnode = n.query("str[@name='type']").get(0);
                if (tnode.getValue().equals("tfloat") ||
                        tnode.getValue().equals("tint") ||
                        tnode.getValue().equals("date")) continue;
                fieldNames.add(name);
            }
        }
        client.destroy();
        return fieldNames;
    }

    /**
     * Get a list of terms from a field, based on a user supplied prefix.
     * 
     * @param url The URL to the Solr core to be queried
     * @param fieldName the field name to query
     * @param q The query
     * @param n How many terms should be returned
     * @return
     * @throws Exception
     */
    public static List<String> getTermsFromField(String url, String fieldName, String q, int n) throws Exception {
        if (n <= 0) n = 10;

        List<String> terms = new ArrayList<String>();
        Client client = Client.create();
        WebResource resource = client.resource(url + "terms?terms.fl=" + fieldName + "&terms.regex.flag=case_insensitive&terms.limit=" + n + "&terms.regex=" + URLEncoder.encode(q, "utf-8") + ".*");
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        if (status != 200) {
            throw new Exception("There was a problem querying the Solr terms resource " + url);
        }
        String xml = response.getEntity(String.class);
        Document doc = new Builder(false).build(xml, null);
        Node node = doc.query("response/lst[@name='terms']").get(0);
        Node termNode = node.getChild(0);
        for (int i = 0; i < termNode.getChildCount(); i++)
            terms.add(((Element) termNode.getChild(i)).getAttribute("name").getValue());
        return terms;
    }

    public static void main(String[] args) throws Exception {
        List<String> terms = getTermsFromField("http://protease.nhgri.nih.gov/servlet/solr/core-assay/", "gobp_term", "dna rep", 5);
        for (String term : terms) {
            System.out.println("term = " + term);
        }
    }


}
