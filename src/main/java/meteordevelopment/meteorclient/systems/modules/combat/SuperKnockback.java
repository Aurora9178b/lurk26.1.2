package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.LivingEntity;

public class SuperKnockback extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<SuperKnockback.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Knockback sprint reset mode."))
               .defaultValue(SuperKnockback.Mode.Normal))
            .build()
      );
   private final Setting<Integer> hurtTimer = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("hurt-timer")
            .description("Only triggers when the target hurt time is at or below this value.")
            .defaultValue(10)
            .range(0, 10)
            .sliderRange(0, 10)
            .build()
      );
   private int intaveTicks;

   public SuperKnockback() {
      super(Categories.Combat, "SuperKnockback", "Performs more knockback when you hit your target.");
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.intaveTicks > 0) {
         this.intaveTicks--;
      }
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      if (this.mc.player != null && this.mc.getConnection() != null) {
         if (event.entity instanceof LivingEntity entity) {
            if (entity.hurtTime <= this.hurtTimer.get()) {
               switch ((SuperKnockback.Mode)this.mode.get()) {
                  case Normal:
                     this.sendSprintPacket(Action.START_SPRINTING);
                     this.sendSprintPacket(Action.STOP_SPRINTING);
                     this.sendSprintPacket(Action.START_SPRINTING);
                     this.sendSprintPacket(Action.STOP_SPRINTING);
                     this.mc.player.setSprinting(true);
                     break;
                  case Intave:
                     if (this.intaveTicks == 0) {
                        this.intaveTicks = 1;
                        this.mc.player.setSprinting(false);
                        this.mc.options.keySprint.setDown(false);
                     }
                     break;
                  case SprintReset:
                     this.sendSprintPacket(Action.STOP_SPRINTING);
               }
            }
         }
      }
   }

   private void sendSprintPacket(Action action) {
      this.mc.getConnection().send(new ServerboundPlayerCommandPacket(this.mc.player, action));
   }

   private enum Mode {
      Normal,
      Intave,
      SprintReset;
   }
}
