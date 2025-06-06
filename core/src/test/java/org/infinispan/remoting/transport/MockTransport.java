package org.infinispan.remoting.transport;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.responses.CacheNotFoundResponse;
import org.infinispan.remoting.responses.ExceptionResponse;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.rpc.ResponseFilter;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.transport.impl.EmptyRaftManager;
import org.infinispan.remoting.transport.impl.MapResponseCollector;
import org.infinispan.remoting.transport.raft.RaftManager;
import org.infinispan.topology.HeartBeatCommand;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.infinispan.xsite.XSiteBackup;
import org.infinispan.xsite.commands.remote.XSiteRequest;

/**
 * Mock implementation of {@link Transport} that allows intercepting remote calls and replying asynchronously.
 * <p>
 * TODO Allow blocking invocations until the test explicitly unblocks them
 *
 * @author Dan Berindei
 * @since 9.2
 */
@Scope(Scopes.GLOBAL)
public class MockTransport implements Transport {
   private static final Log log = LogFactory.getLog(MockTransport.class);

   private final Address localAddress;
   private final BlockingQueue<BlockedRequest> blockedRequests = new LinkedBlockingDeque<>();

   private int viewId;
   private List<Address> members;
   private CompletableFuture<Void> nextViewFuture;

   public MockTransport(Address localAddress) {
      this.localAddress = localAddress;
   }

   public void init(int viewId, List<Address> members) {
      this.viewId = viewId;
      this.members = members;
      this.nextViewFuture = new CompletableFuture<>();
   }

   public void updateView(int viewId, List<Address> members) {
      log.debugf("Installing view %d %s", viewId, members);
      this.viewId = viewId;
      this.members = members;

      CompletableFuture<Void> nextViewFuture = this.nextViewFuture;
      this.nextViewFuture = new CompletableFuture<>();
      nextViewFuture.complete(null);
   }

   /**
    * Expect a command to be invoked remotely and send replies using the {@link BlockedRequest} methods.
    */
   public <T extends ReplicableCommand> BlockedRequest expectCommand(Class<T> expectedCommandClass)
      throws InterruptedException {
      return expectCommand(expectedCommandClass, c -> {});
   }

   /**
    * Expect a command to be invoked remotely and send replies using the {@link BlockedRequest} methods.
    */
   public <T extends ReplicableCommand> BlockedRequest expectCommand(Class<T> expectedCommandClass,
                                                                     Consumer<T> checker)
      throws InterruptedException {
      BlockedRequest request = blockedRequests.poll(10, TimeUnit.SECONDS);
      assertNotNull("Timed out waiting for invocation", request);
      T command = expectedCommandClass.cast(request.getCommand());
      checker.accept(command);
      return request;
   }

   public BlockedRequest expectHeartBeatCommand() throws InterruptedException {
      return expectCommand(HeartBeatCommand.class);
   }

   /**
    * Assert that all the commands already invoked remotely have been verified and there were no errors.
    */
   public void verifyNoErrors() {
      assertTrue("Unexpected remote invocations: " +
                    blockedRequests.stream().map(i -> i.getCommand().toString()).collect(Collectors.joining(", ")),
                 blockedRequests.isEmpty());
   }

   @Override
   public CompletableFuture<Map<Address, Response>> invokeRemotelyAsync(Collection<Address> recipients,
                                                                        ReplicableCommand rpcCommand, ResponseMode mode,
                                                                        long timeout, ResponseFilter responseFilter,
                                                                        DeliverOrder deliverOrder, boolean anycast) {
      Collection<Address> targets = recipients != null ? recipients : members;
      MapResponseCollector collector =
         mode.isSynchronous() ? MapResponseCollector.ignoreLeavers(shouldIgnoreLeavers(mode), targets.size()) : null;
      return blockRequest(recipients, rpcCommand, collector);
   }

   @Override
   public void sendTo(Address destination, ReplicableCommand rpcCommand, DeliverOrder deliverOrder) {
      blockRequest(Collections.singleton(destination), rpcCommand, null);
   }

   @Override
   public void sendToMany(Collection<Address> destinations, ReplicableCommand rpcCommand, DeliverOrder deliverOrder) {
      blockRequest(destinations, rpcCommand, null);
   }

   @Override
   public void sendToAll(ReplicableCommand rpcCommand, DeliverOrder deliverOrder) {
      blockRequest(members, rpcCommand, null);
   }

   @Override
   public <O> XSiteResponse<O> backupRemotely(XSiteBackup backup, XSiteRequest<O> rpcCommand) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isCoordinator() {
      return localAddress.equals(members.get(0));
   }

   @Override
   public Address getCoordinator() {
      return members.get(0);
   }

   @Override
   public Address getAddress() {
      return localAddress;
   }

   @Override
   public List<PhysicalAddress> getPhysicalAddresses() {
      throw new UnsupportedOperationException();
   }

   @Override
   public List<Address> getMembers() {
      return members;
   }

   @Override
   public List<PhysicalAddress> getMembersPhysicalAddresses() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isMulticastCapable() {
      return true;
   }

   @Override
   public void checkCrossSiteAvailable() throws CacheConfigurationException {

   }

   @Override
   public String localSiteName() {
      return null;
   }

   @Start
   @Override
   public void start() {

   }

   @Stop
   @Override
   public void stop() {

   }

   @Override
   public int getViewId() {
      return viewId;
   }

   @Override
   public CompletableFuture<Void> withView(int expectedViewId) {
      if (viewId <= expectedViewId) {
         return CompletableFutures.completedNull();
      }

      return nextViewFuture.thenCompose(v -> withView(expectedViewId));
   }

   @Override
   public Log getLog() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Set<String> getSitesView() {
      return null;
   }

   @Override
   public boolean isSiteCoordinator() {
      return false;
   }

   @Override
   public Collection<Address> getRelayNodesAddress() {
      return Collections.emptyList();
   }

   @Override
   public <T> CompletionStage<T> invokeCommand(Address target, ReplicableCommand command, ResponseCollector<Address, T>
      collector, DeliverOrder deliverOrder, long timeout, TimeUnit unit) {
      return blockRequest(Collections.singleton(target), command, collector);
   }

   @Override
   public <T> CompletionStage<T> invokeCommand(Collection<Address> targets, ReplicableCommand command,
                                               ResponseCollector<Address, T> collector, DeliverOrder deliverOrder, long
                                                  timeout, TimeUnit unit) {
      return blockRequest(targets, command, collector);
   }

   @Override
   public <T> CompletionStage<T> invokeCommandOnAll(ReplicableCommand command, ResponseCollector<Address, T> collector,
                                                    DeliverOrder deliverOrder, long timeout, TimeUnit unit) {
      return blockRequest(members, command, collector);
   }

   @Override
   public <T> CompletableFuture<T> invokeCommandOnAll(Collection<Address> requiredTargets, ReplicableCommand command,
                                                      ResponseCollector<Address, T> collector, DeliverOrder deliverOrder,
                                                      long timeout, TimeUnit unit) {
      return blockRequest(requiredTargets, command, collector);
   }

   @Override
   public <T> CompletionStage<T> invokeCommandStaggered(Collection<Address> targets, ReplicableCommand command,
                                                        ResponseCollector<Address, T> collector, DeliverOrder deliverOrder,
                                                        long timeout, TimeUnit unit) {
      return blockRequest(targets, command, collector);
   }

   @Override
   public <T> CompletionStage<T> invokeCommands(Collection<Address> targets, Function<Address, ReplicableCommand>
      commandGenerator, ResponseCollector<Address, T> responseCollector, DeliverOrder deliverOrder, long timeout, TimeUnit unit) {
      throw new UnsupportedOperationException();
   }

   @Override
   public RaftManager raftManager() {
      return EmptyRaftManager.INSTANCE;
   }

   private <T> CompletableFuture<T> blockRequest(Collection<Address> targets, ReplicableCommand command, ResponseCollector<Address, T> collector) {
      log.debugf("Intercepted command %s to %s", command, targets);
      BlockedRequest request = new BlockedRequest(command, collector);
      blockedRequests.add(request);
      return request.getResultFuture();
   }

   private boolean shouldIgnoreLeavers(ResponseMode mode) {
      return mode != ResponseMode.SYNCHRONOUS;
   }

   /**
    * Receive responses for a blocked remote invocation.
    * <p>
    * For example, {@code remoteInvocation.addResponse(a1, r1).addResponse(a2, r2).finish()},
    * or {@code remoteInvocation.singleResponse(a, r)}
    */
   public static class BlockedRequest {
      private final ReplicableCommand command;
      private final ResponseCollector<Address, ?> collector;
      private final CompletableFuture<Object> resultFuture = new CompletableFuture<>();

      private BlockedRequest(ReplicableCommand command, ResponseCollector<Address,?> collector) {
         this.command = command;
         this.collector = collector;
      }

      public BlockedRequest addResponse(Address sender, Response response) {
         assertFalse(isDone());

         log.debugf("Replying to remote invocation %s with %s from %s", getCommand(), response, sender);
         Object result = collector.addResponse(sender, response);
         if (result != null) {
            resultFuture.complete(result);
         }
         return this;
      }

      public BlockedRequest addLeaver(Address a) {
         return addResponse(a, CacheNotFoundResponse.INSTANCE);
      }

      public BlockedRequest addException(Address a, Exception e) {
         return addResponse(a, new ExceptionResponse(e));
      }

      public void throwException(Exception e) {
         resultFuture.completeExceptionally(e);
      }

      public void finish() {
         if (collector == null) {
            // sendToX methods do not need a finish() call, ignoring it
            return;
         }

         try {
            Object result = collector.finish();
            resultFuture.complete(result);
         } catch (Throwable t) {
            resultFuture.completeExceptionally(t);
         }
      }

      public void singleResponse(Address sender, Response response) {
         addResponse(sender, response);
         if (!isDone()) {
            finish();
         }
      }

      public ReplicableCommand getCommand() {
         return command;
      }

      boolean isDone() {
         return resultFuture.isDone();
      }

      @SuppressWarnings("unchecked")
      <U> CompletableFuture<U> getResultFuture() {
         return (CompletableFuture<U>) resultFuture;
      }
   }
}
