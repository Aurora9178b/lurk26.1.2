package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.LivingEntityAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.phys.Vec3;

public class FastClimb extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> timerMode = this.sgGeneral
      .add(new BoolSetting.Builder().name("timer-mode").description("Use timer.").defaultValue(false).build());
   private final Setting<Double> speed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("climb-speed")
            .description("Your climb speed.")
            .defaultValue(0.2872)
            .min(0.0)
            .visible(() -> !this.timerMode.get())
            .build()
      );
   private final Setting<Double> timer = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("timer")
            .description("The timer value for Timer.")
            .defaultValue(1.436)
            .min(1.0)
            .sliderMin(1.0)
            .visible(this.timerMode::get)
            .build()
      );
   private boolean resetTimer;

   public FastClimb() {
      super(Categories.Movement, "fast-climb", "Allows you to climb faster.");
   }

   @Override
   public void onActivate() {
      this.resetTimer = false;
   }

   @EventHandler
   private void onPreTick(TickEvent.Pre event) {
      if (this.timerMode.get()) {
         if (this.climbing()) {
            this.resetTimer = false;
            Modules.get().get(Timer.class).setOverride(this.timer.get());
         } else if (!this.resetTimer) {
            Modules.get().get(Timer.class).setOverride(1.0);
            this.resetTimer = true;
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (!this.timerMode.get() && this.climbing()) {
         Vec3 velocity = this.mc.player.getDeltaMovement();
         this.mc.player.setDeltaMovement(velocity.x, this.speed.get(), velocity.z);
      }
   }

   private boolean climbing() {
      return (this.mc.player.horizontalCollision || ((LivingEntityAccessor)this.mc.player).meteor$isJumping())
         && (
            this.mc.player.onClimbable()
               || this.mc.player.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this.mc.player)
         );
   }
}
