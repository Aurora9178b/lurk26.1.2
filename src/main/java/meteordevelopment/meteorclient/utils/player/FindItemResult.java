package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.InteractionHand;

public record FindItemResult(int slot, int count) {
   public boolean found() {
      return this.slot != -1;
   }

   public InteractionHand getHand() {
      if (this.slot == 40) {
         return InteractionHand.OFF_HAND;
      } else {
         return this.slot == MeteorClient.mc.player.getInventory().getSelectedSlot() ? InteractionHand.MAIN_HAND : null;
      }
   }

   public boolean isMainHand() {
      return this.getHand() == InteractionHand.MAIN_HAND;
   }

   public boolean isOffhand() {
      return this.getHand() == InteractionHand.OFF_HAND;
   }

   public boolean isHotbar() {
      return this.slot >= 0 && this.slot <= 8;
   }

   public boolean isMain() {
      return this.slot >= 9 && this.slot <= 35;
   }

   public boolean isArmor() {
      return this.slot >= 36 && this.slot <= 39;
   }
}
