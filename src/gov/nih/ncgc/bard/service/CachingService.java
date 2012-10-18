package gov.nih.ncgc.bard.service;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.logging.Logger;

public class CachingService {
    static final String CACHE_MANAGER = 
        CachingService.class.getName()+"CacheManager";
    static final int MAX_CACHE_SIZE = 10000;

    static final private Logger logger = 
        Logger.getLogger(CachingService.class.getName());

    CacheManager cacheManager = CacheManager.create();

    protected int maxCacheSize;
    public CachingService () {
        this (0);
    }

    public CachingService (int maxCacheSize) {
        setMaxCacheSize (maxCacheSize);
        logger.info("## CacheManager instance "+cacheManager);
    }

    public void setMaxCacheSize (int maxCacheSize) {
        if (maxCacheSize <= 0) {
            maxCacheSize = MAX_CACHE_SIZE;
        }
        this.maxCacheSize = maxCacheSize;
    }
    public int getMaxCacheSize () { return maxCacheSize; }

    public Ehcache getCache (String name) {
        Ehcache cache = cacheManager.getEhcache(name);
        if (cache == null) {
            cache = new Cache (name, 
                               maxCacheSize, 
                               false, // overflowToDisk
                               false, // eternal (never expire)
                               10*60*60, // time to live (seconds)
                               10*60*60 // time to idle (seconds)
                               );
            cacheManager.addCacheIfAbsent(cache);
            cache.setStatisticsEnabled(true);
        }
        return cache;
    }

    public Element putCache (Ehcache cache, Object key, Object value) {
        Element el = new Element (key, value);
        cache.put(el);
        return el;
    }

    public <T> T getCacheValue (Ehcache cache, Object key) {
        Element el = cache.get(key);
        if (el != null) {
            return (T)el.getObjectValue();
        }
        return null;
    }
}
