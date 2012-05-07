package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.Path;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Miscellaneous utility functions.
 *
 * @author Rajarshi Guha
 */
public class Util {

    public static String toJson(Object o) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Writer writer = new StringWriter();
        mapper.writeValue(writer, o);
        return writer.toString();
    }

    public static String join(Object[] x, String delim) {
        if (delim == null) delim = "";
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            buffer.append(x[i]);
            if (i != x.length - 1) buffer.append(delim);
        }
        return buffer.toString();
    }

    public static String join(List<? extends Object> x, String delim) {
        if (delim == null) delim = "";
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < x.size(); i++) {
            buffer.append(x.get(i));
            if (i != x.size() - 1) buffer.append(delim);
        }
        return buffer.toString();
    }

    /**
     * Get a list of REST resource paths provided by a class.
     * <p/>
     * This is based on the use of Jersey annotations to mark up the class
     * and methods.
     *
     * @param klass The class to analyze
     * @return A list of paths for each resource provided by the supplied class
     */
    public static List<String> getResourcePaths(Class klass) {
        List<String> ret = new ArrayList<String>();

        String root = null;
        // first get root resource
        Annotation[] classAnnots = klass.getAnnotations();
        for (Annotation annot : classAnnots) {
            if (annot instanceof Path) {
                root = ((Path) annot).value();
            }
        }

        // get method annotations
        Method[] methods = klass.getMethods();
        for (Method method : methods) {
            Annotation[] methodAnnots = method.getAnnotations();
            for (Annotation annot : methodAnnots) {
                if (annot instanceof Path) {
                    String res = ((Path) annot).value();
                    res = root + res;
                    ret.add(res);
                }
            }
        }
        return ret;
    }

    static public byte[] getMD5(String s) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(s.getBytes());
    }

}
