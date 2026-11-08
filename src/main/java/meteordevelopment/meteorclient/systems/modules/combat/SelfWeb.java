package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;

public class SelfWeb extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<SelfWeb.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("The mode to use for selfweb."))
               .defaultValue(SelfWeb.Mode.Normal))
            .build()
      );
   private final Setting<Integer> range = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("range")
            .description("How far away the player has to be from you to place webs. Requires Mode to Smart.")
            .defaultValue(3)
            .min(1)
            .sliderRange(1, 7)
            .visible(() -> this.mode.get() == SelfWeb.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> doubles = this.sgGeneral
      .add(new BoolSetting.Builder().name("double-place").description("Places webs in your upper hitbox as well.").defaultValue(false).build());
   private final Setting<Boolean> turnOff = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-toggle").description("Toggles off after placing the webs.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Forces you to rotate downwards when placing webs.").defaultValue(true).build());

   public SelfWeb() {
      super(Categories.Combat, "self-web", "Automatically places webs on you.");
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      switch ((SelfWeb.Mode)this.mode.get()) {
         case Normal:
            this.placeWeb();
            break;
         case Smart:
            if (TargetUtils.getPlayerTarget(this.range.get().intValue(), SortPriority.LowestDistance) != null) {
               this.placeWeb();
            }
      }
   }

   private void placeWeb() {
      FindItemResult web = InvUtils.findInHotbar(Items.COBWEB);
      BlockUtils.place(this.mc.player.blockPosition(), web, this.rotate.get(), 0, false);
      if (this.doubles.get()) {
         BlockUtils.place(this.mc.player.blockPosition().offset(0, 1, 0), web, this.rotate.get(), 0, false);
      }

      if (this.turnOff.get()) {
         this.toggle();
      }
   }

   public enum Mode {
      Normal,
      Smart;
   }
}
