package gov.nih.ncgc.bard.resourcemgr.util;

public class BardResourceLoaderException extends Exception {
       
    private static final long serialVersionUID = 1365195638L;

    public BardResourceLoaderException(String msg) {
	super(msg);
    }
    
    public BardResourceLoaderException(String msg, Throwable coreException) {
	super(msg, coreException);
    }
}
