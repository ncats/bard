package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.search.MoleculeService;
import gov.nih.ncgc.search.SearchService2;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
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

    public static boolean countRequested(HttpHeaders headers) {
        return (headers.getRequestHeaders().containsKey(BARDConstants.REQUEST_HEADER_COUNT));
    }

    public static String getETag (HttpHeaders headers) {
        List<String> etags = headers.getRequestHeader(HttpHeaders.ETAG);
        return etags != null && !etags.isEmpty() 
            ? etags.iterator().next() : null;
    }

    public static <T> List<List<T>> chunk(T[] array, int chunkSize) {
        List<List<T>> chunkList = new ArrayList<List<T>>();
        int n = 0, i = 0;
        List<T> chunk = new ArrayList<T>();
        while (n < array.length) {
            if (i < chunkSize) {
                chunk.add(array[n++]);
                i++;
            } else {
                chunkList.add(chunk);
                chunk = new ArrayList<T>();
                i = 0;
            }
        }
        if (chunk.size() > 0) chunkList.add(chunk);
        return chunkList;
    }

    public static <T> List<List<T>> chunk(List<T> array, int chunkSize) {
        List<List<T>> chunkList = new ArrayList<List<T>>();
        int n = 0, i = 0;
        List<T> chunk = new ArrayList<T>();
        while (n < array.size()) {
            if (i < chunkSize) {
                chunk.add(array.get(n++));
                i++;
            } else {
                chunkList.add(chunk);
                chunk = new ArrayList<T>();
                i = 0;
            }
        }
        if (chunk.size() > 0) chunkList.add(chunk);
        return chunkList;
    }

    public static String toJson(Object o) throws IOException {
        if (o == null) return "{}";
        ObjectMapper mapper = new ObjectMapper();
        Writer writer = new StringWriter();
        mapper.writeValue(writer, o);
        return writer.toString();
    }

    public static String toString (byte[] bytes) {
        return toString (bytes, bytes.length);
    }

    public static String toString (byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder ();
        for (int i = 0; i < length; ++i) {
            sb.append(String.format("%1$02x", bytes[i] & 0xff));
        }
        return sb.toString();
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

    /**
     * Get an instance of the searching service.
     *
     * @return an instance of {@link SearchService2}
     * @throws Exception if there was an error in getting an instance from the container.
     */
    static public SearchService2 getSearchService() throws Exception {
        try {
            InitialContext ctx = new InitialContext();
            Context env = (Context) ctx.lookup("java:/comp/env");
            return (SearchService2) env.lookup("bard/structure-search");
        } catch (Exception ex) {
            throw new Exception("Can't get the search service");
        }
    }

    /**
     * Get an instance of the molecule retrieval service.
     *
     * @return and instance of {@link gov.nih.ncgc.search.MoleculeService}
     * @throws Exception if there was an error in getting an instance from the container.
     */
    static public MoleculeService getMoleculeService() throws Exception {
        try {
            InitialContext ctx = new InitialContext();
            Context env = (Context) ctx.lookup("java:/comp/env");
            return (MoleculeService) env.lookup("bard/structure-search");
        } catch (Exception ex) {
            throw new Exception("Can't get the search service");
        }
    }

}
