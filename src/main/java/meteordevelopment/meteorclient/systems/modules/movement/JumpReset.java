package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class JumpReset extends Module {
   public JumpReset() {
      super(Categories.Movement, "jump-reset", "Automatically jumps when taking knockback.");
   }

   @EventHandler
   private void onMoveInput(MoveInputEvent event) {
      if (this.mc.player != null && this.mc.player.onGround() && this.mc.player.hurtTime == 9) {
         event.setJump(true);
      }
   }
}
