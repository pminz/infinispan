package org.infinispan.server.resp.commands.string;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commons.time.TimeService;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.operation.GetexOperation;
import org.infinispan.server.resp.serialization.ResponseWriter;

import io.netty.channel.ChannelHandlerContext;

/**
 * GETEX
 *
 * @see <a href="https://redis.io/commands/getex/">GETEX</a>
 * @since 15.0
 */
public class GETEX extends RespCommand implements Resp3Command {
   public GETEX() {
      super(-2, 1, 1, 1, AclCategory.WRITE.mask() | AclCategory.STRING.mask() | AclCategory.FAST.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
         ChannelHandlerContext ctx,
         List<byte[]> arguments) {
      if (arguments.size() > 1) {
         return handler
               .stageToReturn(performOperation(handler.cache(), arguments, handler.respServer().getTimeService()), ctx,
                     ResponseWriter.BULK_STRING_BYTES);
      }
      byte[] keyBytes = arguments.get(0);
      return handler.stageToReturn(handler.cache().getAsync(keyBytes), ctx, ResponseWriter.BULK_STRING_BYTES);
   }

   private static CompletionStage<byte[]> performOperation(AdvancedCache<byte[], byte[]> cache,
         List<byte[]> arguments, TimeService timeService) {
      long expirationMs = GetexOperation.parseExpiration(arguments, timeService);
      return getex(cache, arguments.get(0), expirationMs);
   }

   static CompletionStage<byte[]> getex(Cache<byte[], byte[]> cache, byte[] key, long expirationMs) {
      return cache.getAsync(key)
            .thenCompose(currentValueBytes -> {
               if (currentValueBytes == null) {
                  return CompletableFutures.completedNull();
               }
               return cache.replaceAsync(key, currentValueBytes, currentValueBytes, expirationMs, TimeUnit.MILLISECONDS)
                     .thenCompose(replaced -> {
                        if (replaced) {
                           return CompletableFuture.completedFuture(currentValueBytes);
                        }
                        return getex(cache, key, expirationMs);
                     });
            });
   }
}
