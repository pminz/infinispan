package org.infinispan.remoting.transport.impl;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.transport.Address;

/**
 * A remote command invocation request.
 *
 * @author Dan Berindei
 * @since 9.1
 */
public interface Request<S, T> extends CompletionStage<T> {
   long NO_REQUEST_ID = 0;

   /**
    * @return The unique request id.
    */
   long getRequestId();

   /**
    * Called when a response is received for this response.
    */
   void onResponse(S sender, Response response);

   /**
    * Called when the node received a new cluster view.
    *
    * @return {@code true} if any of the request targets is not in the view. This value should only be used
    * before the request was actually submitted.
    */
   boolean onNewView(Set<Address> members);

   /**
    * Complete the request with an exception and release its resources.
    */
   void cancel(Exception cancellationException);
}
