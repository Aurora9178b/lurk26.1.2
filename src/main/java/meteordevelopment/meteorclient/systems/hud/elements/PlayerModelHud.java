package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class PlayerModelHud extends HudElement {
   public static final HudElementInfo<PlayerModelHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "player-model", "Displays a model of your player.", PlayerModelHud::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<Boolean> copyYaw = this.sgGeneral
      .add(new BoolSetting.Builder().name("copy-yaw").description("Makes the player model's yaw equal to yours.").defaultValue(true).build());
   private final Setting<Integer> customYaw = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("custom-yaw")
            .description("Custom yaw for when copy yaw is off.")
            .defaultValue(0)
            .range(-180, 180)
            .sliderRange(-180, 180)
            .visible(() -> !this.copyYaw.get())
            .build()
      );
   private final Setting<Boolean> copyPitch = this.sgGeneral
      .add(new BoolSetting.Builder().name("copy-pitch").description("Makes the player model's pitch equal to yours.").defaultValue(true).build());
   private final Setting<Integer> customPitch = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("custom-pitch")
            .description("Custom pitch for when copy pitch is off.")
            .defaultValue(0)
            .range(-90, 90)
            .sliderRange(-90, 90)
            .visible(() -> !this.copyPitch.get())
            .build()
      );
   private final Setting<PlayerModelHud.CenterOrientation> centerOrientation = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("center-orientation"))
                  .description("Which direction the player faces when the HUD model faces directly forward."))
               .defaultValue(PlayerModelHud.CenterOrientation.South))
            .build()
      );
   public final Setting<Boolean> customScale = this.sgScale
      .add(
         new BoolSetting.Builder()
            .name("custom-scale")
            .description("Applies a custom scale to this hud element.")
            .defaultValue(false)
            .onChanged(var1 -> this.calculateSize())
            .build()
      );
   public final Setting<Double> scale = this.sgScale
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Custom scale.")
            .visible(this.customScale::get)
            .defaultValue(2.0)
            .onChanged(var1 -> this.calculateSize())
            .min(0.5)
            .sliderRange(0.5, 3.0)
            .build()
      );
   private final Setting<Boolean> background = this.sgBackground
      .add(new BoolSetting.Builder().name("background").description("Displays background.").defaultValue(false).build());
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(this.background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
      );

   public PlayerModelHud() {
      super(INFO);
      this.calculateSize();
   }

   @Override
   public void render(HudRenderer renderer) {
      renderer.post(
         () -> {
            Player player = MeteorClient.mc.player;
            if (player != null) {
               float offsetYaw = this.centerOrientation.get() == PlayerModelHud.CenterOrientation.North ? 180.0F : 0.0F;
               float yaw = this.copyYaw.get()
                  ? Mth.wrapDegrees(
                     player.yRotO + (player.getYRot() - player.yRotO) * MeteorClient.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true) + offsetYaw
                  )
                  : this.customYaw.get().intValue();
               float pitch = this.copyPitch.get() ? player.getXRot() : this.customPitch.get().intValue();
               renderer.entity(player, this.x, this.y, this.getWidth(), this.getHeight(), -yaw, -pitch);
            }
         }
      );
      if (this.background.get()) {
         renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
      } else if (MeteorClient.mc.player == null) {
         renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
         renderer.line(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), Color.GRAY);
         renderer.line(this.x + this.getWidth(), this.y, this.x, this.y + this.getHeight(), Color.GRAY);
      }
   }

   private void calculateSize() {
      this.setSize(50.0 * this.getScale(), 75.0 * this.getScale());
   }

   private double getScale() {
      return this.customScale.get() ? this.scale.get() : this.scale.getDefaultValue();
   }

   private enum CenterOrientation {
      North,
      South;
   }
}
