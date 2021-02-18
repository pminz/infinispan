package org.infinispan.spring.embedded.session;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.spring.common.provider.SpringCache;
import org.infinispan.spring.common.session.AbstractInfinispanSessionRepository;
import org.infinispan.spring.common.session.PrincipalNameResolver;
import org.springframework.session.MapSession;

/**
 * Session Repository for Infinispan in Embedded mode.
 *
 * @author Sebastian Łaskawiec
 * @since 9.0
 */
public class InfinispanEmbeddedSessionRepository extends AbstractInfinispanSessionRepository {

   /**
    * Creates new repository based on {@link SpringCache}
    *
    * @param cache Cache which shall be used for session repository.
    */
   public InfinispanEmbeddedSessionRepository(SpringCache cache) {
      super(cache, new EmbeddedApplicationPublishedBridge(cache));
   }

   @Override
   protected void removeFromCacheWithoutNotifications(String originalId) {
      Cache nativeCache = (Cache) cache.getNativeCache();
      nativeCache.getAdvancedCache().withFlags(Flag.SKIP_LISTENER_NOTIFICATION).remove(originalId);
   }

   @Override
   public Map<String, MapSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
      if (!PRINCIPAL_NAME_INDEX_NAME.equals(indexName)) {
         return Collections.emptyMap();
      }

      Cache<String, MapSession> embeddedCache = (Cache<String, MapSession>) cache.getNativeCache();
      return embeddedCache.values().stream()
                          .filter(session -> indexValue.equals(PrincipalNameResolver.getInstance().resolvePrincipal(session)))
                          .collect(() -> Collectors.toMap(MapSession::getId, Function.identity()));
   }
}
