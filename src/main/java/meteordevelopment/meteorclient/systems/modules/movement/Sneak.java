package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Sneak extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Sneak.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Which method to sneak."))
               .defaultValue(Sneak.Mode.Vanilla))
            .build()
      );

   public Sneak() {
      super(Categories.Movement, "sneak", "Sneaks for you");
   }

   public boolean doPacket() {
      return this.isActive() && !this.mc.player.getAbilities().flying && this.mode.get() == Sneak.Mode.Packet;
   }

   public boolean doVanilla() {
      return this.isActive() && !this.mc.player.getAbilities().flying && this.mode.get() == Sneak.Mode.Vanilla;
   }

   public enum Mode {
      Packet,
      Vanilla;
   }
}
