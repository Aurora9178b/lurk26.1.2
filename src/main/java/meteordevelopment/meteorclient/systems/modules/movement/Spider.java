package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;

public class Spider extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> speed = this.sgGeneral
      .add(new DoubleSetting.Builder().name("climb-speed").description("The speed you go up blocks.").defaultValue(0.2).min(0.0).build());

   public Spider() {
      super(Categories.Movement, "spider", "Allows you to climb walls like a spider.");
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player.horizontalCollision) {
         Vec3 velocity = this.mc.player.getDeltaMovement();
         if (!(velocity.y >= 0.2)) {
            this.mc.player.setDeltaMovement(velocity.x, this.speed.get(), velocity.z);
         }
      }
   }
}
