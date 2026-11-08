package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.HitResult.Type;

public class KeepSprint extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Integer> slowdown = this.sgGeneral
      .add(new IntSetting.Builder().name("slowdown").description("Custom slowdown after attacking.").defaultValue(0).range(0, 100).sliderRange(0, 100).build());
   private final Setting<Boolean> groundOnly = this.sgGeneral
      .add(new BoolSetting.Builder().name("ground-only").description("Only keeps sprinting on ground.").defaultValue(false).build());
   private final Setting<Boolean> prediction = this.sgGeneral
      .add(new BoolSetting.Builder().name("prediction").description("Only applies after the post tick stage.").defaultValue(false).build());
   private final Setting<Boolean> reachOnly = this.sgGeneral
      .add(new BoolSetting.Builder().name("reach-only").description("Only keeps sprinting for reach hits.").defaultValue(false).build());
   private boolean can;

   public KeepSprint() {
      super(Categories.Movement, "keep-sprint", "Keeps sprint after attacking.");
   }

   @Override
   public String getInfoString() {
      return this.slowdown.get() + "%";
   }

   @EventHandler
   private void onTickPre(TickEvent.Pre event) {
      this.can = false;
   }

   @EventHandler
   private void onTickPost(TickEvent.Post event) {
      this.can = true;
   }

   public boolean shouldKeepSprint() {
      if (!this.isActive() || this.mc.player == null) {
         return false;
      } else if (this.prediction.get() && !this.can) {
         return false;
      } else if (this.groundOnly.get() && !this.mc.player.onGround()) {
         return false;
      } else if (this.reachOnly.get()) {
         return this.mc.hitResult != null && this.mc.hitResult.getType() != Type.MISS
            ? this.mc.hitResult.getLocation().distanceTo(this.mc.player.getEyePosition()) > 3.0
            : false;
      } else {
         return true;
      }
   }
}
