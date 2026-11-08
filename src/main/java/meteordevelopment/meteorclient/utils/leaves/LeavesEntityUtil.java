package meteordevelopment.meteorclient.utils.leaves;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;

public class LeavesEntityUtil {
   private LeavesEntityUtil() {
   }

   public static void attackSwingHand() {
      if (MeteorClient.mc.player != null && MeteorClient.mc.getConnection() != null) {
         MeteorClient.mc.player.swing(InteractionHand.MAIN_HAND);
         MeteorClient.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      }
   }

   public static void placeSwingHand() {
      if (MeteorClient.mc.player != null && MeteorClient.mc.getConnection() != null) {
         MeteorClient.mc.player.swing(InteractionHand.MAIN_HAND);
         MeteorClient.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      }
   }
}
