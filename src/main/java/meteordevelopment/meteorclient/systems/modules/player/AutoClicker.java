package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;

public class AutoClicker extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> inScreens = this.sgGeneral
      .add(new BoolSetting.Builder().name("while-in-screens").description("Whether to click while a screen is open.").defaultValue(true).build());
   private final Setting<AutoClicker.Mode> leftClickMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode-left"))
                  .description("The method of clicking for left clicks."))
               .defaultValue(AutoClicker.Mode.Press))
            .build()
      );
   private final Setting<Integer> leftClickDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("delay-left")
            .description("The amount of delay between left clicks in ticks.")
            .defaultValue(2)
            .min(0)
            .sliderMax(60)
            .visible(() -> this.leftClickMode.get() == AutoClicker.Mode.Press)
            .build()
      );
   private final Setting<AutoClicker.Mode> rightClickMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode-right"))
                  .description("The method of clicking for right clicks."))
               .defaultValue(AutoClicker.Mode.Press))
            .build()
      );
   private final Setting<Integer> rightClickDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("delay-right")
            .description("The amount of delay between right clicks in ticks.")
            .defaultValue(2)
            .min(0)
            .sliderMax(60)
            .visible(() -> this.rightClickMode.get() == AutoClicker.Mode.Press)
            .build()
      );
   private int rightClickTimer;
   private int leftClickTimer;

   public AutoClicker() {
      super(Categories.Player, "auto-clicker", "Automatically clicks.");
   }

   @Override
   public void onActivate() {
      this.rightClickTimer = 0;
      this.leftClickTimer = 0;
      this.mc.options.keyAttack.setDown(false);
      this.mc.options.keyUse.setDown(false);
   }

   @Override
   public void onDeactivate() {
      this.mc.options.keyAttack.setDown(false);
      this.mc.options.keyUse.setDown(false);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.inScreens.get() || this.mc.screen == null) {
         switch ((AutoClicker.Mode)this.leftClickMode.get()) {
            case Disabled:
            default:
               break;
            case Hold:
               this.mc.options.keyAttack.setDown(true);
               break;
            case Press:
               this.leftClickTimer++;
               if (this.leftClickTimer > this.leftClickDelay.get()) {
                  Utils.leftClick();
                  this.leftClickTimer = 0;
               }
         }

         switch ((AutoClicker.Mode)this.rightClickMode.get()) {
            case Disabled:
            default:
               break;
            case Hold:
               this.mc.options.keyUse.setDown(true);
               break;
            case Press:
               this.rightClickTimer++;
               if (this.rightClickTimer > this.rightClickDelay.get()) {
                  Utils.rightClick();
                  this.rightClickTimer = 0;
               }
         }
      }
   }

   public enum Mode {
      Disabled,
      Hold,
      Press;
   }
}
