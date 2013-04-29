package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Arrays;
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

}
