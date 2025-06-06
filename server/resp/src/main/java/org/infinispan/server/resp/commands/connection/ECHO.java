package org.infinispan.server.resp.commands.connection;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.Resp3Command;

import io.netty.channel.ChannelHandlerContext;

/**
 * ECHO
 *
 * @see <a href="https://redis.io/commands/echo/">ECHO</a>
 * @since 14.0
 */
public class ECHO extends RespCommand implements Resp3Command {
   public ECHO() {
      super(2, 0, 0, 0, AclCategory.FAST.mask() | AclCategory.CONNECTION.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx,
                                                                List<byte[]> arguments) {
      byte[] argument = arguments.get(0);
      handler.writer().string(argument);
      return handler.myStage();
   }
}
