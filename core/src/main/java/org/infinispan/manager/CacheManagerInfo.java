package org.infinispan.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.commons.dataconversion.internal.JsonSerialization;
import org.infinispan.commons.util.Immutables;
import org.infinispan.commons.util.Version;
import org.infinispan.configuration.ConfigurationManager;
import org.infinispan.registry.InternalCacheRegistry;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.security.actions.SecurityActions;
import org.infinispan.topology.LocalTopologyManager;

/**
 * @since 10.0
 */
public class CacheManagerInfo implements JsonSerialization {

   public static final List<String> LOCAL_NODE = Collections.singletonList("local");
   private final DefaultCacheManager cacheManager;
   private final ConfigurationManager configurationManager;
   private final InternalCacheRegistry internalCacheRegistry;
   private final LocalTopologyManager localTopologyManager;

   public CacheManagerInfo(DefaultCacheManager cacheManager,
                           ConfigurationManager configurationManager,
                           InternalCacheRegistry internalCacheRegistry,
                           LocalTopologyManager localTopologyManager) {
      this.cacheManager = cacheManager;
      this.configurationManager = configurationManager;
      this.internalCacheRegistry = internalCacheRegistry;
      this.localTopologyManager = localTopologyManager;
   }

   public String getCoordinatorAddress() {
      Transport transport = getTransport();
      return transport == null ? "N/A" : transport.getCoordinator().toString();
   }

   public boolean isCoordinator() {
      Transport transport = getTransport();
      return transport != null && transport.isCoordinator();
   }

   private Transport getTransport() {
      return SecurityActions.getGlobalComponentRegistry(cacheManager).getComponent(Transport.class);
   }

   public String getCacheManagerStatus() {
      return cacheManager.getStatus().toString();
   }

   public Set<BasicCacheInfo> getDefinedCaches() {
      return cacheManager.getCacheNames().stream().map(c -> {
         boolean started = cacheManager.getCaches().containsKey(c);
         return new BasicCacheInfo(c, started);
      }).collect(Collectors.toSet());
   }

   public Set<String> getCacheConfigurationNames() {
      Set<String> names = new TreeSet<>(configurationManager.getDefinedConfigurations());
      internalCacheRegistry.filterPrivateCaches(names);
      if (names.isEmpty())
         return Collections.emptySet();
      else
         return Immutables.immutableSetWrap(names);
   }

   public long getCreatedCacheCount() {
      return getCacheNames().count();
   }

   public Stream<String> getCacheNames() {
      return cacheManager.getCaches().keySet().stream().filter(c -> !internalCacheRegistry.isInternalCache(c));
   }

   public long getRunningCacheCount() {
      return cacheManager.getCaches().keySet().stream().filter(c -> cacheManager.isRunning(c) && !internalCacheRegistry.isInternalCache(c)).count();
   }

   public String getVersion() {
      return Version.getVersion();
   }

   public String getName() {
      return configurationManager.getGlobalConfiguration().cacheManagerName();
   }

   public String getNodeName() {
      Transport transport = getTransport();
      if (transport == null) return getNodeAddress();
      return transport.localNodeName();
   }

   public String getNodeAddress() {
      return cacheManager.getLogicalAddressString();
   }

   public String getPhysicalAddresses() {
      Transport transport = getTransport();
      if (transport == null) return "local";
      var address = transport.getPhysicalAddresses();
      return address == null ? "local" : address.toString();
   }

   public List<String> getPhysicalAddressesRaw() {
      Transport transport = getTransport();
      if (transport == null) return LOCAL_NODE;
      var address = transport.getPhysicalAddresses();
      return address == null
            ? LOCAL_NODE
            : address.stream().map(Object::toString).collect(Collectors.toList());
   }

   public List<String> getClusterMembers() {
      Transport transport = getTransport();
      if (transport == null) return LOCAL_NODE;
      return transport.getMembers().stream().map(Objects::toString).collect(Collectors.toList());
   }

   public List<String> getClusterMembersPhysicalAddresses() {
      Transport transport = getTransport();
      if (transport == null) return LOCAL_NODE;
      return transport.getMembersPhysicalAddresses()
            .stream()
            .map(Objects::toString)
            .toList();
   }

   public int getClusterSize() {
      Transport transport = getTransport();
      if (transport == null) return 1;
      return transport.getMembers().size();
   }

   public String getClusterName() {
      return configurationManager.getGlobalConfiguration().transport().clusterName();
   }

   public String getLocalSite() {
      Transport transport = getTransport();
      if (transport == null) return "local";
      return transport.localSiteName();
   }

   public Collection<String> getSites() {
      Transport transport = getTransport();
      return Optional.ofNullable(transport)
            .map(Transport::getSitesView)
            .orElseGet(Collections::emptySet);
   }

   public boolean isRelayNode() {
      Transport transport = getTransport();
      return transport != null && transport.isSiteCoordinator();
   }

   public Boolean isRebalancingEnabled() {
      try {
         return localTopologyManager.isRebalancingEnabled();
      } catch (Exception e) {
         // Ignore the error
         return null;
      }
   }

   public Collection<String> getRelayNodesAddress() {
      Transport transport = getTransport();
      if (transport == null) {
         return LOCAL_NODE;
      }
      return transport.getRelayNodesAddress().stream().map(Objects::toString).collect(Collectors.toList());
   }

   public boolean isTracingEnabled() {
      return cacheManager.getConfigurationManager()
                 .getGlobalConfiguration().tracing().enabled();
   }

   @Override
   public Json toJson() {
     Json result = Json.object()
            .set("version", getVersion())
            .set("name", getName())
            .set("coordinator", isCoordinator())
            .set("cache_configuration_names", Json.make(getCacheConfigurationNames()))
            .set("cluster_name", getClusterName())
            .set("physical_addresses", getPhysicalAddresses())
            .set("coordinator_address", getCoordinatorAddress())
            .set("cache_manager_status", getCacheManagerStatus())
            .set("created_cache_count", getCreatedCacheCount())
            .set("running_cache_count", getRunningCacheCount())
            .set("node_address", getNodeAddress())
            .set("cluster_members", Json.make(getClusterMembers()))
            .set("cluster_members_physical_addresses", Json.make(getClusterMembersPhysicalAddresses()))
            .set("cluster_size", getClusterSize())
            .set("defined_caches", Json.make(getDefinedCaches()))
            .set("local_site", getLocalSite())
            .set("relay_node", isRelayNode())
            .set("relay_nodes_address", Json.make(getRelayNodesAddress()))
            .set("sites_view", Json.make(getSites()))
            .set("tracing_enabled", isTracingEnabled());

      Boolean rebalancingEnabled = isRebalancingEnabled();
      if (rebalancingEnabled != null) {
         result.set("rebalancing_enabled", rebalancingEnabled);
      }

      return result;
   }

   static class BasicCacheInfo implements JsonSerialization {
      String name;
      boolean started;

      BasicCacheInfo(String name, boolean started) {
         this.name = name;
         this.started = started;
      }

      public String getName() {
         return name;
      }

      public boolean isStarted() {
         return started;
      }

      @Override
      public Json toJson() {
         return Json.object("name", name).set("started", started);
      }
   }
}
