package org.infinispan.server.resp.commands.json;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.json.EmbeddedJsonCache;

/**
 * JSON.OBJLEN
 *
 * @see <a href="https://redis.io/commands/json.objlen/">JSON.OBJLEN</a>
 *
 * @since 15.2
 */
public class JSONOBJLEN extends JSONLEN {
   public JSONOBJLEN() {
      super("JSON.OBJLEN", true, AclCategory.JSON.mask() | AclCategory.READ.mask() | AclCategory.SLOW.mask());
   }

   @Override
   protected CompletionStage<List<Long>> len(EmbeddedJsonCache ejc, byte[] key, byte[] path) {
      return ejc.objLen(key, path);
   }

   @Override
   protected void raiseTypeError(byte[] path) {
      throw new RuntimeException("Path '" + RespUtil.ascii(path) + "' does not exist or not an object");
   }
}
