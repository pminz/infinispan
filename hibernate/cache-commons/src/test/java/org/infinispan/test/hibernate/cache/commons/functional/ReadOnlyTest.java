package org.infinispan.test.hibernate.cache.commons.functional;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.hibernate.stat.Statistics;
import org.infinispan.hibernate.cache.commons.InfinispanBaseRegion;
import org.infinispan.hibernate.cache.commons.util.InfinispanMessageLogger;
import org.infinispan.test.hibernate.cache.commons.functional.entities.Item;
import org.infinispan.test.hibernate.cache.commons.util.TestRegionFactory;
import org.infinispan.commons.time.ControlledTimeService;
import org.junit.Test;

/**
 * Parent tests for both transactional and
 * read-only tests are defined in this class.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
public class ReadOnlyTest extends SingleNodeTest {
	protected static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(ReadOnlyTest.class);
	protected static final ControlledTimeService TIME_SERVICE = new ControlledTimeService();

	@Override
	public List<Object[]> getParameters() {
		return getParameters(false, false, true, true, true);
	}

	@Test
	public void testEmptySecondLevelCacheEntry() {
		sessionFactory().getCache().evictCollectionData(Item.class.getName() + ".items");
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		InfinispanBaseRegion region = TEST_SESSION_ACCESS.getRegion(sessionFactory(), Item.class.getName() + ".items");
		assertEquals(0, region.getElementCountInMemory());
	}

	@Test
	public void testInsertDeleteEntity() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final Item item = new Item( "chris", "Chris's Item" );
		withTxSession(s -> s.persist(item));

		log.info("Entry persisted, let's load and delete it.");

		withTxSession(s -> {
			Item found = s.load(Item.class, item.getId());
			log.info(stats.toString());
			assertEquals(item.getDescription(), found.getDescription());
			assertEquals(0, stats.getSecondLevelCacheMissCount());
			assertEquals(1, stats.getSecondLevelCacheHitCount());
			s.delete(found);
		});
	}

	@Test
	public void testInsertClearCacheDeleteEntity() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final Item item = new Item( "chris", "Chris's Item" );
		withTxSession(s -> s.persist(item));
		assertEquals(0, stats.getSecondLevelCacheMissCount());
		assertEquals(0, stats.getSecondLevelCacheHitCount());
		assertEquals(1, stats.getSecondLevelCachePutCount());

		log.info("Entry persisted, let's load and delete it.");

		cleanupCache();
		TIME_SERVICE.advance(1);

		withTxSession(s -> {
			Item found = s.load(Item.class, item.getId());
			log.info(stats.toString());
			assertEquals(item.getDescription(), found.getDescription());
			assertEquals(1, stats.getSecondLevelCacheMissCount());
			assertEquals(0, stats.getSecondLevelCacheHitCount());
			assertEquals(2, stats.getSecondLevelCachePutCount());
			s.delete(found);
		});
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings(settings);
		settings.put(TestRegionFactory.TIME_SERVICE, TIME_SERVICE);
	}
}
