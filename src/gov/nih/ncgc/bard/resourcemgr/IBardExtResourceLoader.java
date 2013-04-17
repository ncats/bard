package gov.nih.ncgc.bard.resourcemgr;

import java.util.Properties;

public interface IBardExtResourceLoader {

    /**
     * Fulfills the load or update process
     * @return
     */
    public boolean load();
    
    /**
     * Returns a brief text report on load status
     * @return
     */
    public String getLoadStatusReport();
    
    /**
     * Sets the loader properties to support env needs
     * @param loaderProps loader Properties
     */
    public void setLoaderProps(Properties loaderProps);

    /**
     * Sets the service to be provided
     * @param service The BardResourceService to run
     */
    public void setService(BardResourceService service);
}
