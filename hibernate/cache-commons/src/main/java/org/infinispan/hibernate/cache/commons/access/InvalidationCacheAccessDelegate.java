package org.infinispan.hibernate.cache.commons.access;

import org.hibernate.cache.CacheException;
import org.infinispan.hibernate.cache.commons.InfinispanDataRegion;
import org.infinispan.hibernate.cache.commons.util.Caches;
import org.infinispan.hibernate.cache.commons.util.InfinispanMessageLogger;

import org.infinispan.AdvancedCache;

/**
 *
 * @author Brian Stansberry
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class InvalidationCacheAccessDelegate implements AccessDelegate {
	protected static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( InvalidationCacheAccessDelegate.class );
	protected final AdvancedCache cache;
	protected final InfinispanDataRegion region;
	protected final PutFromLoadValidator putValidator;
	protected final AdvancedCache<Object, Object> writeCache;

   /**
    * Create a new transactional access delegate instance.
    *
    * @param region to control access to
    * @param validator put from load validator
    */
	@SuppressWarnings("unchecked")
	protected InvalidationCacheAccessDelegate(InfinispanDataRegion region, PutFromLoadValidator validator) {
		this.region = region;
		this.cache = region.getCache();
		this.putValidator = validator;
		this.writeCache = Caches.ignoreReturnValuesCache( cache );
	}

   /**
    * Attempt to retrieve an object from the cache.
    *
    *
	 * @param session
	 * @param key The key of the item to be retrieved
    * @param txTimestamp a timestamp prior to the transaction start time
    * @return the cached object or <code>null</code>
    * @throws CacheException if the cache retrieval failed
    */
	@Override
	@SuppressWarnings("UnusedParameters")
	public Object get(Object session, Object key, long txTimestamp) throws CacheException {
		if ( !region.checkValid() ) {
			return null;
		}
		final Object val = cache.get( key );
		if (val == null && session != null) {
			putValidator.registerPendingPut(session, key, txTimestamp );
		}
		return val;
	}

	@Override
	public boolean putFromLoad(Object session, Object key, Object value, long txTimestamp, Object version) {
		return putFromLoad(session, key, value, txTimestamp, version, false );
	}

   /**
    * Attempt to cache an object, after loading from the database, explicitly
    * specifying the minimalPut behavior.
    *
	 * @param session Current session
	 * @param key The item key
    * @param value The item
    * @param txTimestamp a timestamp prior to the transaction start time
    * @param version the item version number
    * @param minimalPutOverride Explicit minimalPut flag
    * @return <code>true</code> if the object was successfully cached
    * @throws CacheException if storing the object failed
    */
	@Override
	@SuppressWarnings("UnusedParameters")
	public boolean putFromLoad(Object session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		if ( !region.checkValid() ) {
			if (log.isTraceEnabled()) {
				log.tracef( "Region %s not valid", region.getName() );
			}
			return false;
		}

		// In theory, since putForExternalRead is already as minimal as it can
		// get, we shouldn't be need this check. However, without the check and
		// without https://issues.jboss.org/browse/ISPN-1986, it's impossible to
		// know whether the put actually occurred. Knowing this is crucial so
		// that Hibernate can expose accurate statistics.
		if ( minimalPutOverride && cache.containsKey( key ) ) {
			return false;
		}

		PutFromLoadValidator.Lock lock = putValidator.acquirePutFromLoadLock(session, key, txTimestamp);
		if ( lock == null) {
			if (log.isTraceEnabled()) {
				log.tracef( "Put from load lock not acquired for key %s", key );
			}
			return false;
		}

		try {
			writeCache.putForExternalRead( key, value );
		}
		finally {
			putValidator.releasePutFromLoadLock( key, lock);
		}

		return true;
	}

	@Override
	public void remove(Object session, Object key) throws CacheException {
		// We update whether or not the region is valid. Other nodes
		// may have already restored the region so they need to
		// be informed of the change.
		writeCache.remove(key);
	}

	@Override
	public void lockAll() throws CacheException {
		if (!putValidator.beginInvalidatingRegion()) {
			log.failedInvalidateRegion(region.getName());
		}
	}

	@Override
	public void unlockAll() throws CacheException {
		putValidator.endInvalidatingRegion();
	}

	@Override
	public void removeAll() throws CacheException {
		Caches.removeAll(cache);
	}

	@Override
	public void evict(Object key) throws CacheException {
		writeCache.remove( key );
	}

	@Override
	public void evictAll() throws CacheException {
		try {
			if (!putValidator.beginInvalidatingRegion()) {
				log.failedInvalidateRegion(region.getName());
			}

			// Invalidate the local region and then go remote
			region.invalidateRegion();
			Caches.broadcastEvictAll(cache);
		}
		finally {
			putValidator.endInvalidatingRegion();
		}
	}

	@Override
	public void unlockItem(Object session, Object key) throws CacheException {
	}
}
