package gov.nih.ncgc.bard.capextract;

import java.util.HashMap;
import java.util.Map;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class CapResourceHandlerRegistry {
    private static CapResourceHandlerRegistry instance = null;

    private Map<CAPConstants.CapResource, ICapResourceHandler> handlerMap;

    private CapResourceHandlerRegistry() {
        handlerMap = new HashMap<CAPConstants.CapResource, ICapResourceHandler>();
    }

    public static CapResourceHandlerRegistry getInstance() {
        if (instance == null) instance = new CapResourceHandlerRegistry();
        return instance;
    }

    public void setHandler(CAPConstants.CapResource resource, ICapResourceHandler handler) {
        handlerMap.put(resource, handler);
    }

    public ICapResourceHandler getHandler(CAPConstants.CapResource resource) {
        return handlerMap.get(resource);
    }

}
