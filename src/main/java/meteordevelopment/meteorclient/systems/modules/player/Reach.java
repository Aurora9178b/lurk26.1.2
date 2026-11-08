package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;

public class Reach extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> blockReach = this.sgGeneral
      .add(new DoubleSetting.Builder().name("extra-block-reach").description("The distance to add to your block reach.").sliderMax(1.0).build());
   private final Setting<Double> entityReach = this.sgGeneral
      .add(new DoubleSetting.Builder().name("extra-entity-reach").description("The distance to add to your entity reach.").sliderMax(1.0).build());

   public Reach() {
      super(Categories.Player, "reach", "Gives you super long arms.");
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      return theme.label(
         "Note: on vanilla servers you may give yourself up to 4 blocks of additional reach for specific actions - interacting with block entities (chests, furnaces, etc.) or with vehicles. This does not work on paper servers.",
         Utils.getWindowWidth() / 3.0
      );
   }

   public double blockReach() {
      return this.isActive() ? this.blockReach.get() : 0.0;
   }

   public double entityReach() {
      return this.isActive() ? this.entityReach.get() : 0.0;
   }
}
