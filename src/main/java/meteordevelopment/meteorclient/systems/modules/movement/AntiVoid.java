package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;

public class AntiVoid extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AntiVoid.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                     .description("The method to prevent you from falling into the void."))
                  .defaultValue(AntiVoid.Mode.Jump))
               .onChanged(var1 -> this.onActivate()))
            .build()
      );
   private boolean wasFlightEnabled;
   private boolean hasRun;

   public AntiVoid() {
      super(Categories.Movement, "anti-void", "Attempts to prevent you from falling into the void.");
   }

   @Override
   public void onActivate() {
      if (this.mode.get() == AntiVoid.Mode.Flight) {
         this.wasFlightEnabled = Modules.get().isActive(Flight.class);
      }
   }

   @Override
   public void onDeactivate() {
      if (!this.wasFlightEnabled && this.mode.get() == AntiVoid.Mode.Flight && Utils.canUpdate()) {
         Modules.get().get(Flight.class).disable();
      }
   }

   @EventHandler
   private void onPreTick(TickEvent.Pre event) {
      int minY = this.mc.level.getMinY();
      if (!(this.mc.player.getY() > minY) && !(this.mc.player.getY() < minY - 15)) {
         switch ((AntiVoid.Mode)this.mode.get()) {
            case Flight:
               Modules.get().get(Flight.class).enable();
               this.hasRun = true;
               break;
            case Jump:
               this.mc.player.jumpFromGround();
         }
      } else {
         if (this.hasRun && this.mode.get() == AntiVoid.Mode.Flight) {
            Modules.get().get(Flight.class).disable();
            this.hasRun = false;
         }
      }
   }

   public enum Mode {
      Flight,
      Jump;
   }
}
