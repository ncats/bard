package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Miscellaneous methods to handle CAP and non-CAP annotations.
 *
 * @author Rajarshi Guha
 */
public class AnnotationUtils {

    public static Map<String, String> getMinimumRequiredAssayAnnotations(Long aid, DBUtils db) throws SQLException, ClassNotFoundException, IOException {
        List<String> annoKeys = Arrays.asList(new String[]{
                "species name", "assay format", "assay type",
                "cultured cell name", "detection method type", "detection instrument name",
                "assay footprint",
                "excitation wavelength", "emission wavelength", "absorbance wavelength", "measurement wavelength"});
        List<CAPAnnotation> annos = db.getAssayAnnotations(aid);
        CAPDictionary dict = db.getCAPDictionary();
        Map<String, String> ret = new HashMap<String, String>();
        for (CAPAnnotation anno : annos) {
            String key = anno.key;
            String value = anno.value;
            if (key != null && Util.isNumber(key)) key = dict.getNode(new BigInteger(key)).getLabel();
            if (value != null && Util.isNumber(value)) {
                CAPDictionaryElement node = dict.getNode(new BigInteger(value));
                if (node != null) value = dict.getNode(new BigInteger(value)).getLabel();
                else value = null;
            }
            if (annoKeys.contains(key) && value != null) {
                anno.key = key;
                anno.value = value;
                ret.put(key, value);
            }
        }
        return ret;
    }

    public static JsonNode getAnnotationJson(List<CAPAnnotation> a) throws ClassNotFoundException, IOException, SQLException {
        DBUtils db = new DBUtils();
        CAPDictionary dict = db.getCAPDictionary();

        // lets group these annotations and construct our JSON response
        CAPDictionaryElement node;

        Map<Integer, List<CAPAnnotation>> contexts = new HashMap<Integer, List<CAPAnnotation>>();
        for (CAPAnnotation anno : a) {
            Integer id = anno.id;
            if (id == null) id = -1; // corresponds to dynamically generated annotations (from non-CAP sources)

            // go from dict key to label
            if (anno.key != null && Util.isNumber(anno.key)) {
                node = dict.getNode(new BigInteger(anno.key));
                anno.key = node != null ? node.getLabel() : anno.key;
            }
            if (anno.value != null && Util.isNumber(anno.value)) {
                node = dict.getNode(new BigInteger(anno.value));
                anno.value = node != null ? node.getLabel() : anno.value;
            }

            if (contexts.containsKey(id)) {
                List<CAPAnnotation> la = contexts.get(id);
                la.add(anno);
                contexts.put(id, la);
            } else {
                List<CAPAnnotation> la = new ArrayList<CAPAnnotation>();
                la.add(anno);
                contexts.put(id, la);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docNode = mapper.createArrayNode();
        ArrayNode contextNode = mapper.createArrayNode();
        ArrayNode measureNode = mapper.createArrayNode();
        ArrayNode miscNode = mapper.createArrayNode();

        for (Integer contextId : contexts.keySet()) {
            List<CAPAnnotation> comps = contexts.get(contextId);
            Collections.sort(comps, new Comparator<CAPAnnotation>() {
                @Override
                public int compare(CAPAnnotation o1, CAPAnnotation o2) {
                    if (o1.displayOrder == o2.displayOrder) return 0;
                    return o1.displayOrder < o2.displayOrder ? -1 : 1;
                }
            });
            JsonNode arrayNode = mapper.valueToTree(comps);
            ObjectNode n = mapper.createObjectNode();
            n.put("id", comps.get(0).id);
            n.put("name", comps.get(0).contextRef);
            n.put("group", comps.get(0).contextGroup);
            n.put("comps", arrayNode);

            if (comps.get(0).source.equals("cap-doc")) docNode.add(n);
            else if (comps.get(0).source.equals("cap-context")) contextNode.add(n);
            else if (comps.get(0).source.equals("cap-measure")) measureNode.add(n);
            else {
                for (CAPAnnotation misca : comps) miscNode.add(mapper.valueToTree(misca));
            }
        }
        ObjectNode topLevel = mapper.createObjectNode();
        topLevel.put("contexts", contextNode);
        topLevel.put("measures", measureNode);
        topLevel.put("docs", docNode);
        topLevel.put("misc", miscNode);

        return topLevel;
    }
}
