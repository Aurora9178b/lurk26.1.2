package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoJump extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoJump.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("The method of jumping."))
               .defaultValue(AutoJump.Mode.Jump))
            .build()
      );
   private final Setting<AutoJump.JumpWhen> jumpIf = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("jump-if")).description("Jump if."))
               .defaultValue(AutoJump.JumpWhen.Always))
            .build()
      );
   private final Setting<Double> velocityHeight = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("velocity-height")
            .description("The distance that velocity mode moves you.")
            .defaultValue(0.25)
            .min(0.0)
            .sliderMax(2.0)
            .build()
      );

   public AutoJump() {
      super(Categories.Movement, "auto-jump", "Automatically jumps.");
   }

   private boolean jump() {
      return switch ((AutoJump.JumpWhen)this.jumpIf.get()) {
         case Sprinting -> this.mc.player.isSprinting() && (this.mc.player.zza != 0.0F || this.mc.player.xxa != 0.0F);
         case Walking -> this.mc.player.zza != 0.0F || this.mc.player.xxa != 0.0F;
         case Always -> true;
      };
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player.onGround() && !this.mc.player.isShiftKeyDown() && this.jump()) {
         if (this.mode.get() == AutoJump.Mode.Jump) {
            this.mc.player.jumpFromGround();
         } else {
            ((IVec3)this.mc.player.getDeltaMovement()).meteor$setY(this.velocityHeight.get());
         }
      }
   }

   public enum JumpWhen {
      Sprinting,
      Walking,
      Always;
   }

   public enum Mode {
      Jump,
      LowHop;
   }
}
