package org.infinispan.distribution.ch;

import java.util.function.ToIntFunction;

import org.infinispan.commons.configuration.attributes.Matchable;
import org.infinispan.configuration.cache.HashConfiguration;

/**
 * Map keys to segments.
 *
 * @author Dan Berindei
 * @since 8.2
 */
public interface KeyPartitioner extends Matchable<KeyPartitioner>, ToIntFunction<Object> {
   /**
    * Initialization.
    *
    * <p>The partitioner can also use injection to access other cache-level or global components.
    * This method will be called before any other injection methods.</p>
    *
    * <p>Does not need to be thread-safe (Infinispan safely publishes the instance after initialization).</p>
    * @param configuration
    */
   default void init(HashConfiguration configuration) {
      // Do nothing
   }

   default void init(KeyPartitioner other) {
      // Do nothing
   }

   @Override
   default int applyAsInt(Object value) {
      return getSegment(value);
   }

   /**
    * Obtains the segment for a key.
    * Must be thread-safe.
    */
   int getSegment(Object key);

   @Override
   default boolean matches(KeyPartitioner other) {
      if (this == other)
         return true;
      return other != null && getClass() == other.getClass();
   }
}
