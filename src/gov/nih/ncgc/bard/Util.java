package gov.nih.ncgc.bard;

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

}
