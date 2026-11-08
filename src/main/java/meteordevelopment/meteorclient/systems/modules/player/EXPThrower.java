package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;

public class EXPThrower extends Module {
   public EXPThrower() {
      super(Categories.Player, "exp-thrower", "Automatically throws XP bottles from your hotbar.");
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
      if (exp.found()) {
         Rotations.rotate(this.mc.player.getYRot(), 90.0, () -> {
            if (exp.getHand() != null) {
               this.mc.gameMode.useItem(this.mc.player, exp.getHand());
            } else {
               InvUtils.swap(exp.slot(), true);
               this.mc.gameMode.useItem(this.mc.player, exp.getHand());
               InvUtils.swapBack();
            }
         });
      }
   }
}
