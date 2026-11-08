package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoSprint extends Module {
   public AutoSprint() {
      super(Categories.Movement, "auto-sprint", "Automatically holds sprint.");
   }

   @Override
   public void onDeactivate() {
      if (this.mc.options.keySprint.isDown()) {
         this.mc.options.keySprint.setDown(false);
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.mc.options.keySprint.setDown(true);
   }
}
