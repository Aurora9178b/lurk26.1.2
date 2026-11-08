package meteordevelopment.meteorclient.events.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;

public class ArmRenderEvent {
   public static ArmRenderEvent INSTANCE = new ArmRenderEvent();
   public PoseStack matrix;
   public InteractionHand hand;

   public static ArmRenderEvent get(InteractionHand hand, PoseStack matrices) {
      INSTANCE.matrix = matrices;
      INSTANCE.hand = hand;
      return INSTANCE;
   }
}
