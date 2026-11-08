package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.player.AbstractClientPlayer;

public class PlayerRadarHud extends HudElement {
   public static final HudElementInfo<PlayerRadarHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "player-radar", "Displays players in your visual range.", PlayerRadarHud::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<Integer> limit = this.sgGeneral
      .add(new IntSetting.Builder().name("limit").description("The max number of players to show.").defaultValue(10).min(1).sliderRange(1, 20).build());
   private final Setting<Boolean> distance = this.sgGeneral
      .add(new BoolSetting.Builder().name("distance").description("Shows the distance to the player next to their name.").defaultValue(false).build());
   private final Setting<Boolean> friends = this.sgGeneral
      .add(new BoolSetting.Builder().name("display-friends").description("Whether to show friends or not.").defaultValue(true).build());
   private final Setting<Boolean> shadow = this.sgGeneral
      .add(new BoolSetting.Builder().name("shadow").description("Renders shadow behind text.").defaultValue(true).build());
   private final Setting<SettingColor> primaryColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("primary-color").description("Primary color.").defaultValue(new SettingColor()).build());
   private final Setting<SettingColor> secondaryColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("secondary-color").description("Secondary color.").defaultValue(new SettingColor(175, 175, 175)).build());
   private final Setting<Alignment> alignment = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("alignment")).description("Horizontal alignment."))
               .defaultValue(Alignment.Auto))
            .build()
      );
   private final Setting<Integer> border = this.sgGeneral
      .add(new IntSetting.Builder().name("border").description("How much space to add around the element.").defaultValue(0).build());
   private final Setting<Boolean> customScale = this.sgScale
      .add(new BoolSetting.Builder().name("custom-scale").description("Applies a custom scale to this hud element.").defaultValue(false).build());
   private final Setting<Double> scale = this.sgScale
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Custom scale.")
            .visible(this.customScale::get)
            .defaultValue(1.0)
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
   private final List<AbstractClientPlayer> players = new ArrayList<>();

   public PlayerRadarHud() {
      super(INFO);
   }

   @Override
   public void setSize(double width, double height) {
      super.setSize(width + this.border.get() * 2, height + this.border.get() * 2);
   }

   @Override
   protected double alignX(double width, Alignment alignment) {
      return this.box.alignX(this.getWidth() - this.border.get() * 2, width, alignment);
   }

   @Override
   public void tick(HudRenderer renderer) {
      double width = renderer.textWidth("Players:", this.shadow.get(), this.getScale());
      double height = renderer.textHeight(this.shadow.get(), this.getScale());
      if (MeteorClient.mc.level != null && MeteorClient.mc.player != null && MeteorClient.mc.getCameraEntity() != null) {
         for (AbstractClientPlayer entity : this.getPlayers()) {
            if (!entity.equals(MeteorClient.mc.player) && (this.friends.get() || !Friends.get().isFriend(entity))) {
               String text = entity.getName().getString();
               if (this.distance.get()) {
                  text = text + String.format("(%sm)", Math.round(MeteorClient.mc.getCameraEntity().distanceTo(entity)));
               }

               width = Math.max(width, renderer.textWidth(text, this.shadow.get(), this.getScale()));
               height += renderer.textHeight(this.shadow.get(), this.getScale()) + 2.0;
            }
         }

         this.setSize(width, height);
      } else {
         this.setSize(width, height);
      }
   }

   @Override
   public void render(HudRenderer renderer) {
      double y = this.y + this.border.get();
      if (this.background.get()) {
         renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
      }

      renderer.text(
         "Players:",
         this.x + this.border.get() + this.alignX(renderer.textWidth("Players:", this.shadow.get(), this.getScale()), this.alignment.get()),
         y,
         this.secondaryColor.get(),
         this.shadow.get(),
         this.getScale()
      );
      if (MeteorClient.mc.level != null && MeteorClient.mc.player != null && MeteorClient.mc.getCameraEntity() != null) {
         double spaceWidth = renderer.textWidth(" ", this.shadow.get(), this.getScale());

         for (AbstractClientPlayer entity : this.getPlayers()) {
            if (!entity.equals(MeteorClient.mc.player) && (this.friends.get() || !Friends.get().isFriend(entity))) {
               String text = entity.getName().getString();
               Color color = PlayerUtils.getPlayerColor(entity, this.primaryColor.get());
               String distanceText = null;
               double width = renderer.textWidth(text, this.shadow.get(), this.getScale());
               if (this.distance.get()) {
                  width += spaceWidth;
               }

               if (this.distance.get()) {
                  distanceText = String.format("(%sm)", Math.round(MeteorClient.mc.getCameraEntity().distanceTo(entity)));
                  width += renderer.textWidth(distanceText, this.shadow.get(), this.getScale());
               }

               double x = this.x + this.border.get() + this.alignX(width, this.alignment.get());
               y += renderer.textHeight(this.shadow.get(), this.getScale()) + 2.0;
               x = renderer.text(text, x, y, color, this.shadow.get());
               if (this.distance.get()) {
                  renderer.text(distanceText, x + spaceWidth, y, this.secondaryColor.get(), this.shadow.get(), this.getScale());
               }
            }
         }
      }
   }

   private List<AbstractClientPlayer> getPlayers() {
      this.players.clear();
      if (MeteorClient.mc.level != null && MeteorClient.mc.getCameraEntity() != null) {
         this.players.addAll(MeteorClient.mc.level.players());
         this.players.sort(Comparator.comparingDouble(e -> e.distanceToSqr(MeteorClient.mc.getCameraEntity())));
         if (this.players.size() > this.limit.get()) {
            this.players.subList(this.limit.get(), this.players.size()).clear();
         }

         return this.players;
      } else {
         return this.players;
      }
   }

   private double getScale() {
      return this.customScale.get() ? this.scale.get() : Hud.get().getTextScale();
   }
}
