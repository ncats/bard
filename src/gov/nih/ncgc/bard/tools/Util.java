package gov.nih.ncgc.bard.tools;

import java.util.List;

/**
 * Miscellaneous utility functions.
 *
 * @author Rajarshi Guha
 */
public class Util {

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

}
