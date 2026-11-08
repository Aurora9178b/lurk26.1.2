package meteordevelopment.meteorclient.systems.modules.render;

import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class Chams extends Module {
   private final SettingGroup sgThroughWalls = this.settings.createGroup("Through Walls");
   private final SettingGroup sgPlayers = this.settings.createGroup("Players");
   private final SettingGroup sgCrystals = this.settings.createGroup("Crystals");
   private final SettingGroup sgHand = this.settings.createGroup("Hand");
   public final Setting<Set<EntityType<?>>> entities = this.sgThroughWalls
      .add(new EntityTypeListSetting.Builder().name("entities").description("Select entities to show through walls.").onlyAttackable().build());
   public final Setting<Chams.Shader> shader = this.sgThroughWalls
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shader"))
                  .description("Renders a shader over of the entities."))
               .defaultValue(Chams.Shader.Image))
            .build()
      );
   public final Setting<SettingColor> shaderColor = this.sgThroughWalls
      .add(
         new ColorSetting.Builder()
            .name("color")
            .description("The color that the shader is drawn with.")
            .defaultValue(new SettingColor(255, 255, 255, 150))
            .visible(() -> this.shader.get() != Chams.Shader.None)
            .build()
      );
   public final Setting<Boolean> ignoreSelfDepth = this.sgThroughWalls
      .add(new BoolSetting.Builder().name("ignore-self").description("Ignores yourself drawing the player.").defaultValue(true).build());
   public final Setting<Boolean> players = this.sgPlayers
      .add(new BoolSetting.Builder().name("players").description("Enables model tweaks for players.").defaultValue(false).build());
   public final Setting<Boolean> ignoreSelf = this.sgPlayers
      .add(
         new BoolSetting.Builder()
            .name("ignore-self")
            .description("Ignores yourself when tweaking player models.")
            .defaultValue(false)
            .visible(this.players::get)
            .build()
      );
   public final Setting<Boolean> playersTexture = this.sgPlayers
      .add(new BoolSetting.Builder().name("texture").description("Enables player model textures.").defaultValue(false).visible(this.players::get).build());
   public final Setting<SettingColor> playersColor = this.sgPlayers
      .add(
         new ColorSetting.Builder()
            .name("color")
            .description("The color of player models.")
            .defaultValue(new SettingColor(198, 135, 254, 150))
            .visible(this.players::get)
            .build()
      );
   public final Setting<Double> playersScale = this.sgPlayers
      .add(new DoubleSetting.Builder().name("scale").description("Players scale.").defaultValue(1.0).min(0.0).visible(this.players::get).build());
   public final Setting<Boolean> crystals = this.sgCrystals
      .add(new BoolSetting.Builder().name("crystals").description("Enables model tweaks for end crystals.").defaultValue(false).build());
   public final Setting<Double> crystalsScale = this.sgCrystals
      .add(new DoubleSetting.Builder().name("scale").description("Crystal scale.").defaultValue(0.6).min(0.0).visible(this.crystals::get).build());
   public final Setting<Double> crystalsBounce = this.sgCrystals
      .add(new DoubleSetting.Builder().name("bounce").description("How high crystals bounce.").defaultValue(0.6).min(0.0).visible(this.crystals::get).build());
   public final Setting<Double> crystalsRotationSpeed = this.sgCrystals
      .add(
         new DoubleSetting.Builder()
            .name("rotation-speed")
            .description("Multiplies the rotation speed of the crystal.")
            .defaultValue(0.3)
            .min(0.0)
            .visible(this.crystals::get)
            .build()
      );
   public final Setting<Boolean> crystalsTexture = this.sgCrystals
      .add(
         new BoolSetting.Builder()
            .name("texture")
            .description("Whether to render crystal model textures.")
            .defaultValue(true)
            .visible(this.crystals::get)
            .build()
      );
   public final Setting<SettingColor> crystalsColor = this.sgCrystals
      .add(
         new ColorSetting.Builder()
            .name("crystal-color")
            .description("The color of the of the crystal.")
            .defaultValue(new SettingColor(198, 135, 254, 255))
            .visible(this.crystals::get)
            .build()
      );
   public final Setting<Boolean> hand = this.sgHand
      .add(new BoolSetting.Builder().name("enabled").description("Enables tweaks of hand rendering.").defaultValue(false).build());
   public final Setting<Boolean> handTexture = this.sgHand
      .add(new BoolSetting.Builder().name("texture").description("Whether to render hand textures.").defaultValue(false).visible(this.hand::get).build());
   public final Setting<SettingColor> handColor = this.sgHand
      .add(
         new ColorSetting.Builder()
            .name("hand-color")
            .description("The color of your hand.")
            .defaultValue(new SettingColor(198, 135, 254, 150))
            .visible(this.hand::get)
            .build()
      );
   public static final Identifier BLANK = MeteorClient.identifier("textures/blank.png");

   public Chams() {
      super(Categories.Render, "chams", "Tweaks rendering of entities.");
   }

   public boolean shouldRender(Entity entity) {
      return this.isActive() && !this.isShader() && this.entities.get().contains(entity.getType()) && (entity != this.mc.player || !this.ignoreSelfDepth.get());
   }

   public boolean isShader() {
      return this.isActive() && this.shader.get() != Chams.Shader.None;
   }

   public enum Shader {
      Image,
      None;
   }
}
