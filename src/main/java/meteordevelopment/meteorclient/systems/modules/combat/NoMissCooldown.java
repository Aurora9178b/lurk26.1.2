package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoMissCooldown extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Boolean> removeAttackCooldown = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("remove-attack-cooldown")
            .description("Disables the 10 tick cooldown after missing an attack.")
            .defaultValue(true)
            .build()
      );
   public final Setting<Boolean> cancelAttackOnMiss = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("cancel-attack-on-miss")
            .description("Cancels the attack when missing, preventing the client swing.")
            .defaultValue(false)
            .build()
      );

   public NoMissCooldown() {
      super(Categories.Combat, "no-miss-cooldown", "Disables attack miss cooldown and can cancel attacks on miss.");
   }

   @Override
   public String getInfoString() {
      return "NoMiss";
   }

   public boolean shouldRemoveAttackCooldown() {
      return this.isActive() && this.removeAttackCooldown.get();
   }

   public boolean shouldCancelAttackOnMiss() {
      return this.isActive() && this.cancelAttackOnMiss.get();
   }
}
