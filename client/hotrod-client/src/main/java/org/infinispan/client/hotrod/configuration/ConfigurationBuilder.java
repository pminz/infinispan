package org.infinispan.client.hotrod.configuration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.FailoverRequestBalancingStrategy;
import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.impl.TypedProperties;
import org.infinispan.client.hotrod.impl.consistenthash.ConsistentHash;
import org.infinispan.client.hotrod.impl.consistenthash.ConsistentHashV2;
import org.infinispan.client.hotrod.impl.consistenthash.SegmentConsistentHash;
import org.infinispan.client.hotrod.impl.transport.TransportFactory;
import org.infinispan.client.hotrod.impl.transport.tcp.RoundRobinBalancingStrategy;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.commons.tx.lookup.TransactionManagerLookup;
import org.infinispan.commons.util.Util;

/**
 * <p>ConfigurationBuilder used to generate immutable {@link Configuration} objects to pass to the
 * {@link RemoteCacheManager#RemoteCacheManager(Configuration)} constructor.</p>
 *
 * <p>It is also possible to configure the {@link RemoteCacheManager} via a properties file named
 * <tt>hotrod-client.properties</tt> and placed in the classpath. The following table describes the individual properties
 * and the related programmatic configuration API.</p>
 *
 * <table>
 *    <tr>
 *       <th>Name</th>
 *       <th>Type</th>
 *       <th>Default</th>
 *       <th>Description</th>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.server_list</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link ServerConfigurationBuilder#addServers(String)}</td>
 *    </tr>
 *    <tr>
 *      <td><b>infinispan.client.hotrod.marshaller</b></td>
 *       <td>String (class name)</td>
 *       <td>{@link org.infinispan.commons.marshall.jboss.GenericJBossMarshaller GenericJBossMarshaller}</td>
 *       <td>{@link #marshaller(String)}</td>
 *    </tr>
 *    <tr>
 *      <td><b>infinispan.client.hotrod.async_executor_factory</b></td>
 *       <td>String (class name)</td>
 *       <td>{@link org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory DefaultAsyncExecutorFactory}</td>
 *       <td>{@link ExecutorFactoryConfigurationBuilder#factoryClass(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.default_executor_factory.pool_size</b></td>
 *       <td>Integer</td>
 *       <td>99</td>
 *       <td>Size of the thread pool</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.client_intelligence</b></td>
 *       <td>String</td>
 *       <td>HASH_DISTRIBUTION_AWARE</td>
 *       <td>{@link #clientIntelligence(ClientIntelligence)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.tcp_no_delay</b></td>
 *       <td>Boolean</td>
 *       <td>true</td>
 *       <td>{@link #tcpNoDelay(boolean)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.tcp_keep_alive</b></td>
 *       <td>Boolean</td>
 *       <td>true</td>
 *       <td>{@link #tcpKeepAlive(boolean)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.request_balancing_strategy</b></td>
 *       <td>String (class name)</td>
 *       <td>{@link org.infinispan.client.hotrod.impl.transport.tcp.RoundRobinBalancingStrategy RoundRobinBalancingStrategy}</td>
 *       <td>{@link #balancingStrategy(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.key_size_estimate</b></td>
 *       <td>Integer</td>
 *       <td>64</td>
 *       <td>{@link #keySizeEstimate(int)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.value_size_estimate</b></td>
 *       <td>Integer</td>
 *       <td>512</td>
 *       <td>{@link #valueSizeEstimate(int)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.force_return_values</b></td>
 *       <td>Boolean</td>
 *       <td>false</td>
 *       <td>{@link #forceReturnValues(boolean)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.hash_function_impl.2</b></td>
 *       <td>String</td>
 *       <td>{@link org.infinispan.client.hotrod.impl.consistenthash.ConsistentHashV2 ConsistentHashV2}</td>
 *       <td>{@link #consistentHashImpl(int, String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.socket_timeout</b></td>
 *       <td>Integer</td>
 *       <td>60000</td>
 *       <td>{@link #socketTimeout(int)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.connect_timeout</b></td>
 *       <td>Integer</td>
 *       <td>60000</td>
 *       <td>{@link #connectionTimeout(int)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.protocol_version</b></td>
 *       <td>String</td>
 *       <td>The highest version supported by the client in use</td>
 *       <td>{@link #version(ProtocolVersion)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.use_ssl</b></td>
 *       <td>Boolean</td>
 *       <td>false</td>
 *       <td>Will be implicitly enabled if a trust store is set.<br>{@link SslConfigurationBuilder#enabled(boolean)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.key_store_file_name</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#keyStoreFileName(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.key_store_type</b></td>
 *       <td>String</td>
 *       <td>JKS</td>
 *       <td>{@link SslConfigurationBuilder#keyStoreType(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.key_store_password</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#keyStorePassword(char[])}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.sni_host_name</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#sniHostName(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.key_alias</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#keyAlias(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.key_store_certificate_password</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#keyStoreCertificatePassword(char[])}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.trust_store_file_name</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#trustStoreFileName(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.trust_store_path</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#trustStorePath(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.trust_store_type</b></td>
 *       <td>String</td>
 *       <td>JKS</td>
 *       <td>{@link SslConfigurationBuilder#trustStoreType(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.trust_store_password</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#trustStorePassword(char[])}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.ssl_protocol</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link SslConfigurationBuilder#protocol(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.ssl_context</b></td>
 *       <td>javax.net.ssl.SSLContext</td>
 *       <td>N/A</td>
 *       <td>Can only be set programmatically.<br>{@link SslConfigurationBuilder#sslContext(javax.net.ssl.SSLContext)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.max_retries</b></td>
 *       <td>Integer</td>
 *       <td>10</td>
 *       <td>{@link #maxRetries(int)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.use_auth</b></td>
 *       <td>Boolean</td>
 *       <td>Implicitly enabled by other authentication properties</td>
 *       <td>{@link AuthenticationConfigurationBuilder#enabled(boolean)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.sasl_mechanism</b></td>
 *       <td>String</td>
 *       <td>DIGEST-MD5 if username and password are set<br>EXTERNAL if a key store is set</td>
 *       <td>{@link AuthenticationConfigurationBuilder#saslMechanism(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.auth_callback_handler</b></td>
 *       <td>String</td>
 *       <td>Chosen automatically based on selected SASL mech</td>
 *       <td>{@link AuthenticationConfigurationBuilder#callbackHandler(javax.security.auth.callback.CallbackHandler)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.auth_server_name</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link AuthenticationConfigurationBuilder#serverName(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.auth_username</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link AuthenticationConfigurationBuilder#username(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.auth_password</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link AuthenticationConfigurationBuilder#password(char[])}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.auth_realm</b></td>
 *       <td>String</td>
 *       <td>ApplicationRealm</td>
 *       <td>{@link AuthenticationConfigurationBuilder#realm(String)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.auth_client_subject</b></td>
 *       <td>javax.security.auth.Subject</td>
 *       <td>N/A</td>
 *       <td>Can only be set programmatically.<br>{@link AuthenticationConfigurationBuilder#clientSubject(javax.security.auth.Subject)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.sasl_properties.*</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>One per SASL property.<br>{@link AuthenticationConfigurationBuilder#saslProperties(java.util.Map)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.java_serial_whitelist</b></td>
 *       <td>String</td>
 *       <td>N/A</td>
 *       <td>{@link #addJavaSerialWhiteList(String...)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.batch_size</b></td>
 *       <td>Integer</td>
 *       <td>10000</td>
 *       <td>{@link #batchSize(int)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.transaction.transaction_manager_lookup</b></td>
 *       <td>String (class name)</td>
 *       <td>{@link TransactionConfigurationBuilder#defaultTransactionManagerLookup()}</td>
 *       <td>{@link TransactionConfigurationBuilder#transactionManagerLookup(TransactionManagerLookup)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.transaction.transaction_mode</b></td>
 *       <td>String ({@link TransactionMode} enum name)</td>
 *       <td>{@link TransactionMode#NONE NONE}</td>
 *       <td>{@link TransactionConfigurationBuilder#transactionMode(TransactionMode)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.transaction.timeout</b></td>
 *       <td>Long</td>
 *       <td>{@link TransactionConfigurationBuilder#DEFAULT_TIMEOUT 60000}</td>
 *       <td>{@link TransactionConfigurationBuilder#timeout(long, TimeUnit)}</td>
 *    </tr>
 *       <td><b>infinispan.client.hotrod.near_cache.mode</b></td>
 *       <td>String ({@link NearCacheMode} enum name)</td>
 *       <td>{@link NearCacheMode#DISABLED DISABLED}</td>
 *       <td>{@link NearCacheConfigurationBuilder#mode(NearCacheMode)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.near_cache.max_entries</b></td>
 *       <td>Integer</td>
 *       <td>-1 (no limit)</td>
 *       <td>{@link NearCacheConfigurationBuilder#maxEntries(int)}</td>
 *    </tr>
 *    <tr>
 *       <td><b>infinispan.client.hotrod.near_cache.name_pattern</b></td>
 *        <td>String (regex pattern, see {@link Pattern})</td>
 *        <td>null (matches all cache names)</td>
 *        <td>{@link NearCacheConfigurationBuilder#cacheNamePattern(String)}</td>
 *    </tr>
 * </table>
 *
 * @author Tristan Tarrant
 * @since 5.3
 */
public class ConfigurationBuilder implements ConfigurationChildBuilder, Builder<Configuration> {

   private static final Log log = LogFactory.getLog(ConfigurationBuilder.class, Log.class);

   // Match IPv4 (host:port) or IPv6 ([host]:port) addresses
   private static final Pattern ADDRESS_PATTERN = Pattern
         .compile("(\\[([0-9A-Fa-f:]+)\\]|([^:/?#]*))(?::(\\d*))?");

   private WeakReference<ClassLoader> classLoader;
   private final ExecutorFactoryConfigurationBuilder asyncExecutorFactory;
   private Supplier<FailoverRequestBalancingStrategy> balancingStrategyFactory = RoundRobinBalancingStrategy::new;
   private ClientIntelligence clientIntelligence = ClientIntelligence.getDefault();
   private final ConnectionPoolConfigurationBuilder connectionPool;
   private int connectionTimeout = ConfigurationProperties.DEFAULT_CONNECT_TIMEOUT;
   @SuppressWarnings("unchecked")
   private final Class<? extends ConsistentHash> consistentHashImpl[] = new Class[] {
         null, ConsistentHashV2.class, SegmentConsistentHash.class
   };
   private boolean forceReturnValues;
   private int keySizeEstimate = ConfigurationProperties.DEFAULT_KEY_SIZE;
   private Class<? extends Marshaller> marshallerClass;
   private Marshaller marshaller;
   private ProtocolVersion protocolVersion = ProtocolVersion.DEFAULT_PROTOCOL_VERSION;
   private final List<ServerConfigurationBuilder> servers = new ArrayList<>();
   private int socketTimeout = ConfigurationProperties.DEFAULT_SO_TIMEOUT;
   private final SecurityConfigurationBuilder security;
   private boolean tcpNoDelay = true;
   private boolean tcpKeepAlive = false;
   private int valueSizeEstimate = ConfigurationProperties.DEFAULT_VALUE_SIZE;
   private int maxRetries = ConfigurationProperties.DEFAULT_MAX_RETRIES;
   private final NearCacheConfigurationBuilder nearCache;
   private final List<String> whiteListRegExs = new ArrayList<>();
   private int batchSize = ConfigurationProperties.DEFAULT_BATCH_SIZE;
   private final TransactionConfigurationBuilder transaction;

   private final List<ClusterConfigurationBuilder> clusters = new ArrayList<>();

   public ConfigurationBuilder() {
      this.classLoader = new WeakReference<>(Thread.currentThread().getContextClassLoader());
      this.connectionPool = new ConnectionPoolConfigurationBuilder(this);
      this.asyncExecutorFactory = new ExecutorFactoryConfigurationBuilder(this);
      this.security = new SecurityConfigurationBuilder(this);
      this.nearCache = new NearCacheConfigurationBuilder(this);
      this.transaction = new TransactionConfigurationBuilder(this);
   }

   @Override
   public ServerConfigurationBuilder addServer() {
      ServerConfigurationBuilder builder = new ServerConfigurationBuilder(this);
      this.servers.add(builder);
      return builder;
   }

   @Override
   public ClusterConfigurationBuilder addCluster(String clusterName) {
      ClusterConfigurationBuilder builder = new ClusterConfigurationBuilder(this, clusterName);
      this.clusters.add(builder);
      return builder;
   }

   @Override
   public ConfigurationBuilder addServers(String servers) {
      for (String server : servers.split(";")) {
         Matcher matcher = ADDRESS_PATTERN.matcher(server.trim());
         if (matcher.matches()) {
            String v6host = matcher.group(2);
            String v4host = matcher.group(3);
            String host = v6host != null ? v6host : v4host;
            String portString = matcher.group(4);
            int port = portString == null
                  ? ConfigurationProperties.DEFAULT_HOTROD_PORT
                  : Integer.parseInt(portString);
            this.addServer().host(host).port(port);
         } else {
            throw log.parseErrorServerAddress(server);
         }

      }
      return this;
   }

   @Override
   public ExecutorFactoryConfigurationBuilder asyncExecutorFactory() {
      return this.asyncExecutorFactory;
   }

   @Override
   public ConfigurationBuilder balancingStrategy(String balancingStrategy) {
      this.balancingStrategyFactory = () -> Util.getInstance(balancingStrategy, this.classLoader());
      return this;
   }

   @Deprecated
   @Override
   public ConfigurationBuilder balancingStrategy(FailoverRequestBalancingStrategy balancingStrategy) {
      this.balancingStrategyFactory = () -> balancingStrategy;
      return this;
   }

   @Override
   public ConfigurationBuilder balancingStrategy(Supplier<FailoverRequestBalancingStrategy> balancingStrategyFactory) {
      this.balancingStrategyFactory = balancingStrategyFactory;
      return this;
   }

   @Override
   public ConfigurationBuilder balancingStrategy(Class<? extends FailoverRequestBalancingStrategy> balancingStrategy) {
      this.balancingStrategyFactory = () -> Util.getInstance(balancingStrategy);
      return this;
   }

   @Override
   public ConfigurationBuilder classLoader(ClassLoader cl) {
      this.classLoader = new WeakReference<>(cl);
      return this;
   }

   ClassLoader classLoader() {
      return classLoader != null ? classLoader.get() : null;
   }

   @Override
   public ConfigurationBuilder clientIntelligence(ClientIntelligence clientIntelligence) {
      this.clientIntelligence = clientIntelligence;
      return this;
   }

   @Override
   public ConnectionPoolConfigurationBuilder connectionPool() {
      return connectionPool;
   }

   @Override
   public ConfigurationBuilder connectionTimeout(int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
   }

   @Override
   public ConfigurationBuilder consistentHashImpl(int version, Class<? extends ConsistentHash> consistentHashClass) {
      if (version == 1) {
         log.warn("Hash function version 1 is no longer supported.");
      } else {
         this.consistentHashImpl[version - 1] = consistentHashClass;
      }
      return this;
   }

   @Override
   public ConfigurationBuilder consistentHashImpl(int version, String consistentHashClass) {
      if (version == 1) {
         log.warn("Hash function version 1 is no longer supported.");
      } else {
         this.consistentHashImpl[version - 1] = Util.loadClass(consistentHashClass, classLoader());
      }
      return this;
   }

   @Override
   public ConfigurationBuilder forceReturnValues(boolean forceReturnValues) {
      this.forceReturnValues = forceReturnValues;
      return this;
   }

   @Override
   public ConfigurationBuilder keySizeEstimate(int keySizeEstimate) {
      this.keySizeEstimate = keySizeEstimate;
      return this;
   }

   @Override
   public ConfigurationBuilder marshaller(String marshaller) {
      this.marshallerClass = Util.loadClass(marshaller, this.classLoader());
      return this;
   }

   @Override
   public ConfigurationBuilder marshaller(Class<? extends Marshaller> marshaller) {
      this.marshallerClass = marshaller;
      return this;
   }

   @Override
   public ConfigurationBuilder marshaller(Marshaller marshaller) {
      this.marshaller = marshaller;
      return this;
   }

   public NearCacheConfigurationBuilder nearCache() {
      return nearCache;
   }

   /**
    * @deprecated Use {@link ConfigurationBuilder#version(ProtocolVersion)} instead.
    */
   @Deprecated
   @Override
   public ConfigurationBuilder protocolVersion(String protocolVersion) {
      this.protocolVersion = ProtocolVersion.parseVersion(protocolVersion);
      return this;
   }

   @Override
   public ConfigurationBuilder version(ProtocolVersion protocolVersion) {
      this.protocolVersion = protocolVersion;
      return this;
   }

   @Override
   public SecurityConfigurationBuilder security() {
      return security;
   }

   @Override
   public ConfigurationBuilder socketTimeout(int socketTimeout) {
      this.socketTimeout = socketTimeout;
      return this;
   }

   @Override
   public ConfigurationBuilder tcpNoDelay(boolean tcpNoDelay) {
      this.tcpNoDelay = tcpNoDelay;
      return this;
   }

   @Override
   public ConfigurationBuilder tcpKeepAlive(boolean keepAlive) {
      this.tcpKeepAlive = keepAlive;
      return this;
   }

   @Override
   public ConfigurationBuilder transportFactory(String transportFactory) {
      log.transportFactoryDeprecated();
      return this;
   }

   @Override
   public ConfigurationBuilder transportFactory(Class<? extends TransportFactory> transportFactory) {
      log.transportFactoryDeprecated();
      return this;
   }

   @Override
   public ConfigurationBuilder valueSizeEstimate(int valueSizeEstimate) {
      this.valueSizeEstimate = valueSizeEstimate;
      return this;
   }

   @Override
   public ConfigurationBuilder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
   }

   @Override
   public ConfigurationBuilder addJavaSerialWhiteList(String... regEx) {
      this.whiteListRegExs.addAll(Arrays.asList(regEx));
      return this;
   }

   @Override
   public ConfigurationBuilder batchSize(int batchSize) {
      if (batchSize <= 0) {
         throw new IllegalArgumentException("batchSize must be greater than 0");
      }
      this.batchSize = batchSize;
      return this;
   }

   @Override
   public TransactionConfigurationBuilder transaction() {
      return transaction;
   }

   @Override
   public ConfigurationBuilder withProperties(Properties properties) {
      TypedProperties typed = TypedProperties.toTypedProperties(properties);

      if (typed.containsKey(ConfigurationProperties.ASYNC_EXECUTOR_FACTORY)) {
         this.asyncExecutorFactory().factoryClass(typed.getProperty(ConfigurationProperties.ASYNC_EXECUTOR_FACTORY, null, true));
      }
      this.asyncExecutorFactory().withExecutorProperties(typed);
      this.balancingStrategy(typed.getProperty(ConfigurationProperties.REQUEST_BALANCING_STRATEGY, balancingStrategyFactory.get().getClass().getName(), true));
      this.clientIntelligence(typed.getEnumProperty(ConfigurationProperties.CLIENT_INTELLIGENCE, ClientIntelligence.class, ClientIntelligence.getDefault(), true));
      this.connectionPool.withPoolProperties(typed);
      this.connectionTimeout(typed.getIntProperty(ConfigurationProperties.CONNECT_TIMEOUT, connectionTimeout, true));
      if (typed.containsKey(ConfigurationProperties.HASH_FUNCTION_PREFIX + ".1")) {
         log.warn("Hash function version 1 is no longer supported");
      }
      for (int i = 0; i < consistentHashImpl.length; i++) {
         if (consistentHashImpl[i] != null) {
            int version = i + 1;
            this.consistentHashImpl(version,
                  typed.getProperty(ConfigurationProperties.HASH_FUNCTION_PREFIX + "." + version,
                        consistentHashImpl[i].getName(), true));
         }
      }
      this.forceReturnValues(typed.getBooleanProperty(ConfigurationProperties.FORCE_RETURN_VALUES, forceReturnValues, true));
      this.keySizeEstimate(typed.getIntProperty(ConfigurationProperties.KEY_SIZE_ESTIMATE, keySizeEstimate, true));
      if (typed.containsKey(ConfigurationProperties.MARSHALLER)) {
         this.marshaller(typed.getProperty(ConfigurationProperties.MARSHALLER, null, true));
      }
      this.version(ProtocolVersion.parseVersion(typed.getProperty(ConfigurationProperties.PROTOCOL_VERSION, protocolVersion.toString(), true)));
      String serverList = typed.getProperty(ConfigurationProperties.SERVER_LIST, null, true);
      if (serverList != null) {
         this.servers.clear();
         this.addServers(serverList);
      }
      this.socketTimeout(typed.getIntProperty(ConfigurationProperties.SO_TIMEOUT, socketTimeout, true));
      this.tcpNoDelay(typed.getBooleanProperty(ConfigurationProperties.TCP_NO_DELAY, tcpNoDelay, true));
      this.tcpKeepAlive(typed.getBooleanProperty(ConfigurationProperties.TCP_KEEP_ALIVE, tcpKeepAlive, true));
      if (typed.containsKey(ConfigurationProperties.TRANSPORT_FACTORY)) {
         this.transportFactory(typed.getProperty(ConfigurationProperties.TRANSPORT_FACTORY, null, true));
      }
      this.valueSizeEstimate(typed.getIntProperty(ConfigurationProperties.VALUE_SIZE_ESTIMATE, valueSizeEstimate, true));
      this.maxRetries(typed.getIntProperty(ConfigurationProperties.MAX_RETRIES, maxRetries, true));
      this.security.ssl().withProperties(properties);
      this.security.authentication().withProperties(properties);

      String serialWhitelist = typed.getProperty(ConfigurationProperties.JAVA_SERIAL_WHITELIST);
      if (serialWhitelist != null) {
         String[] classes = serialWhitelist.split(",");
         Collections.addAll(this.whiteListRegExs, classes);
      }

      this.batchSize(typed.getIntProperty(ConfigurationProperties.BATCH_SIZE, batchSize, true));
      transaction.withTransactionProperties(properties);

      if (typed.containsKey(ConfigurationProperties.NEAR_CACHE_MAX_ENTRIES)) {
         this.nearCache().maxEntries(typed.getIntProperty(ConfigurationProperties.NEAR_CACHE_MAX_ENTRIES, -1));
      }
      if (typed.containsKey(ConfigurationProperties.NEAR_CACHE_MODE)) {
         this.nearCache().mode(NearCacheMode.valueOf(typed.getProperty(ConfigurationProperties.NEAR_CACHE_MODE)));
      }
      if (typed.containsKey(ConfigurationProperties.NEAR_CACHE_NAME_PATTERN)) {
         this.nearCache().cacheNamePattern(typed.getProperty(ConfigurationProperties.NEAR_CACHE_NAME_PATTERN));
      }
      return this;
   }

   @Override
   public void validate() {
      connectionPool.validate();
      asyncExecutorFactory.validate();
      security.validate();
      nearCache.validate();
      transaction.validate();
      if (maxRetries < 0) {
         throw log.invalidMaxRetries(maxRetries);
      }
      Set<String> clusterNameSet = new HashSet<>(clusters.size());
      for (ClusterConfigurationBuilder clusterConfigBuilder : clusters) {
         if (!clusterNameSet.add(clusterConfigBuilder.getClusterName())) {
            throw log.duplicateClusterDefinition(clusterConfigBuilder.getClusterName());
         }
         clusterConfigBuilder.validate();
      }
   }

   @Override
   public Configuration create() {
      List<ServerConfiguration> servers = new ArrayList<>();
      if (this.servers.size() > 0)
         for (ServerConfigurationBuilder server : this.servers) {
            servers.add(server.create());
         }
      else {
         servers.add(new ServerConfiguration("127.0.0.1", ConfigurationProperties.DEFAULT_HOTROD_PORT));
      }

      List<ClusterConfiguration> serverClusterConfigs = clusters.stream()
         .map(ClusterConfigurationBuilder::create).collect(Collectors.toList());
      if (marshaller == null && marshallerClass == null) {
         marshallerClass = GenericJBossMarshaller.class;
      }

      return new Configuration(asyncExecutorFactory.create(), balancingStrategyFactory, classLoader == null ? null : classLoader.get(), clientIntelligence, connectionPool.create(), connectionTimeout,
            consistentHashImpl, forceReturnValues, keySizeEstimate, marshaller, marshallerClass, protocolVersion, servers, socketTimeout, security.create(), tcpNoDelay, tcpKeepAlive,
            valueSizeEstimate, maxRetries, nearCache.create(), serverClusterConfigs, whiteListRegExs, batchSize, transaction.create());
   }

   @Override
   public Configuration build() {
      return build(true);
   }

   public Configuration build(boolean validate) {
      if (validate) {
         validate();
      }
      return create();
   }

   @Override
   public ConfigurationBuilder read(Configuration template) {
      this.classLoader = new WeakReference<>(template.classLoader());
      this.asyncExecutorFactory.read(template.asyncExecutorFactory());
      this.balancingStrategyFactory = template.balancingStrategyFactory();
      this.connectionPool.read(template.connectionPool());
      this.connectionTimeout = template.connectionTimeout();
      for (int i = 0; i < consistentHashImpl.length; i++) {
         this.consistentHashImpl[i] = template.consistentHashImpl(i + 1);
      }
      this.forceReturnValues = template.forceReturnValues();
      this.keySizeEstimate = template.keySizeEstimate();
      this.marshaller = template.marshaller();
      this.marshallerClass = template.marshallerClass();
      this.protocolVersion = template.version();
      this.servers.clear();
      for (ServerConfiguration server : template.servers()) {
         this.addServer().host(server.host()).port(server.port());
      }
      this.clusters.clear();
      template.clusters().forEach(cluster -> this.addCluster(cluster.getClusterName()).read(cluster));
      this.socketTimeout = template.socketTimeout();
      this.security.read(template.security());
      this.tcpNoDelay = template.tcpNoDelay();
      this.tcpKeepAlive = template.tcpKeepAlive();
      this.valueSizeEstimate = template.valueSizeEstimate();
      this.maxRetries = template.maxRetries();
      this.nearCache.read(template.nearCache());
      this.whiteListRegExs.addAll(template.serialWhitelist());
      this.transaction.read(template.transaction());

      return this;
   }
}
