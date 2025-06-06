package org.infinispan.server.resp.commands.sortedset;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.commands.sortedset.internal.POP;

/**
 * ZPOPMAX
 *
 * @see <a href="https://redis.io/commands/zpopmax/">ZPOPMAX</a>
 * @since 15.0
 */
public class ZPOPMAX extends POP {

   public ZPOPMAX() {
      super(false, AclCategory.WRITE.mask() | AclCategory.SORTEDSET.mask() | AclCategory.FAST.mask());
   }
}
