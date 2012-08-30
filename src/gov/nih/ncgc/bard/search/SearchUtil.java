package gov.nih.ncgc.bard.search;

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
}
