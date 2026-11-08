package meteordevelopment.meteorclient.events.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;

public class HeldItemRendererEvent {
   private static final HeldItemRendererEvent INSTANCE = new HeldItemRendererEvent();
   public InteractionHand hand;
   public PoseStack matrix;

   public static HeldItemRendererEvent get(InteractionHand hand, PoseStack matrices) {
      INSTANCE.hand = hand;
      INSTANCE.matrix = matrices;
      return INSTANCE;
   }
}
