package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.world.phys.HitResult;

public class ItemUseCrosshairTargetEvent {
   private static final ItemUseCrosshairTargetEvent INSTANCE = new ItemUseCrosshairTargetEvent();
   public HitResult target;

   public static ItemUseCrosshairTargetEvent get(HitResult target) {
      INSTANCE.target = target;
      return INSTANCE;
   }
}
