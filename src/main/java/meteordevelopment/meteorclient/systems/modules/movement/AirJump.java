package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;

public class AirJump extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> maintainLevel = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("maintain-level").description("Maintains your current Y level when holding the jump key.").defaultValue(false).build()
      );
   private int level;

   public AirJump() {
      super(Categories.Movement, "air-jump", "Lets you jump in the air.");
   }

   @Override
   public void onActivate() {
      this.level = this.mc.player.blockPosition().getY();
   }

   @EventHandler
   private void onKey(KeyInputEvent event) {
      if (!Modules.get().isActive(Freecam.class) && this.mc.screen == null && !this.mc.player.onGround()) {
         if (event.action == KeyAction.Press) {
            if (this.mc.options.keyJump.matches(event.input)) {
               this.level = this.mc.player.blockPosition().getY();
               this.mc.player.jumpFromGround();
            } else if (this.mc.options.keyShift.matches(event.input)) {
               this.level--;
            }
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (!Modules.get().isActive(Freecam.class) && !this.mc.player.onGround()) {
         if (this.maintainLevel.get() && this.mc.player.blockPosition().getY() == this.level && this.mc.options.keyJump.isDown()) {
            this.mc.player.jumpFromGround();
         }
      }
   }
}
