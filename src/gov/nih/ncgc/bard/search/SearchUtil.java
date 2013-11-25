package gov.nih.ncgc.bard.search;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.TermsParams;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SearchUtil {

    static final String explainRegex = "\\(MATCH\\) .*weight\\((.+?):";
    static final Pattern pattern = Pattern.compile(explainRegex);

    public static List<String> getMatchingFieldNames(String explain) {
        List<String> r = new ArrayList<String>();
        Matcher matcher = pattern.matcher(explain);
        while (matcher.find()) {
            for (int i = 0; i < matcher.groupCount(); i++) {
                r.add(matcher.group(i+1));
            }
        }
        return r;
    }

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
    public static Map<String, List<String>> extractFilterQueries(String filter, List<SolrField> fields) {
        Map<String, SolrField> map = new HashMap<String, SolrField>();
        for (SolrField f : fields) map.put(f.getName(), f);

        HashMap<String, List<String>> ret = new HashMap<String, List<String>>();
        if (filter == null || filter.trim().equals("")) return ret;
        Pattern pattern = Pattern.compile("fq\\((.*?):[\"'](.*?)[\"']\\),");
        Matcher matcher = pattern.matcher(filter);
        while (matcher.find()) {
            for (int i = 1; i < matcher.groupCount(); i += 2) {
                String fname = matcher.group(i);
                String fvalue = matcher.group(i + 1).trim();

                SolrField solrField = map.get(fname);
                if (solrField == null) continue;

                String type = solrField.getType();
                boolean isNumericField = type.contains("int") || type.contains("float");

                if (!fvalue.contains("\"") && !isNumericField) fvalue = "\"" + fvalue + "\"";

                if (ret.containsKey(fname)) {
                    List<String> tmp = ret.get(fname);
                    tmp.add(fvalue);
                    ret.put(fname, tmp);
                } else {
                    List<String> tmp = new ArrayList<String>();
                    tmp.add(fvalue);
                    ret.put(fname, tmp);
                }
            }
        }
        return ret;
    }

    public static void deleteDocs(String url, String... docIds) throws IOException, SolrServerException {
        SolrServer solr = new CommonsHttpSolrServer(url);
        List<String> l = new ArrayList<String>();
        Collections.addAll(l, docIds);
        solr.deleteById(l);
        solr.commit(true, true);
    }

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

        String url = "http://carnot.ncats.nih.gov:8094/solr/core-assay-v14/";
        SearchUtil.deleteDocs(url, "1748");
        System.exit(-1);

        String s = "0.33150536 = (MATCH) max of:\n" +
                "  0.2429601 = (MATCH) weight(av_dict_label:lopac in 2985), product of:\n" +
                "    0.51730007 = queryWeight(av_dict_label:lopac), product of:\n" +
                "      7.514713 = idf(docFreq=6, maxDocs=4725)\n" +
                "      0.0688383 = queryNorm\n" +
                "    0.46966955 = (MATCH) fieldWeight(av_dict_label:lopac in 2985), product of:\n" +
                "      1.0 = tf(termFreq(av_dict_label:lopac)=1)\n" +
                "      7.514713 = idf(docFreq=6, maxDocs=4725)\n" +
                "      0.0625 = fieldNorm(field=av_dict_label, doc=2985)\n" +
                "  0.33150536 = (MATCH) weight(description:lopac in 2985), product of:\n" +
                "    0.5404622 = queryWeight(description:lopac), product of:\n" +
                "      7.851185 = idf(docFreq=4, maxDocs=4725)\n" +
                "      0.0688383 = queryNorm\n" +
                "    0.6133738 = (MATCH) fieldWeight(description:lopac in 2985), product of:\n" +
                "      1.0 = tf(termFreq(description:lopac)=1)\n" +
                "      7.851185 = idf(docFreq=4, maxDocs=4725)\n" +
                "      0.078125 = fieldNorm(field=description, doc=2985)\n";
        List<String> fn = getMatchingFieldNames(s);
        for (String afn:fn) System.out.println(afn);
        System.exit(-1);

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
