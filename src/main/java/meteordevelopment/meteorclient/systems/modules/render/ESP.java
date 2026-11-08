package meteordevelopment.meteorclient.systems.modules.render;

import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3d;

public class ESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgColors = this.settings.createGroup("Colors");
   public final Setting<ESP.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Rendering mode."))
               .defaultValue(ESP.Mode.Shader))
            .build()
      );
   public final Setting<Boolean> highlightTarget = this.sgGeneral
      .add(new BoolSetting.Builder().name("highlight-target").description("highlights the currently targeted entity differently").defaultValue(false).build());
   public final Setting<Boolean> targetHitbox = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("target-hitbox")
            .description("draw the hitbox of the target entity")
            .defaultValue(true)
            .visible(this.highlightTarget::get)
            .build()
      );
   public final Setting<Integer> outlineWidth = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("outline-width")
            .description("The width of the shader outline.")
            .visible(() -> this.mode.get() == ESP.Mode.Shader)
            .defaultValue(2)
            .range(1, 10)
            .sliderRange(1, 5)
            .build()
      );
   public final Setting<Double> glowMultiplier = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("glow-multiplier")
            .description("Multiplier for glow effect")
            .visible(() -> this.mode.get() == ESP.Mode.Shader)
            .decimalPlaces(3)
            .defaultValue(3.5)
            .min(0.0)
            .sliderMax(10.0)
            .build()
      );
   public final Setting<Boolean> ignoreSelf = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-self").description("Ignores yourself drawing the shader.").defaultValue(true).build());
   public final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                     .description("How the shapes are rendered."))
                  .visible(() -> this.mode.get() != ESP.Mode.Glow))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   public final Setting<Double> fillOpacity = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("fill-opacity")
            .description("The opacity of the shape fill.")
            .visible(() -> this.shapeMode.get() != ShapeMode.Lines && this.mode.get() != ESP.Mode.Glow)
            .defaultValue(0.3)
            .range(0.0, 1.0)
            .sliderMax(1.0)
            .build()
      );
   private final Setting<Double> fadeDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("fade-distance")
            .description("The distance from an entity where the color begins to fade.")
            .defaultValue(3.0)
            .min(0.0)
            .sliderMax(12.0)
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("entities").description("Select specific entities.").defaultValue(EntityType.PLAYER).build());
   public final Setting<ESP.ESPColorMode> colorMode = this.sgColors
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("color-mode"))
                  .description("Determines the colors used for entities."))
               .defaultValue(ESP.ESPColorMode.EntityType))
            .build()
      );
   public final Setting<Boolean> friendOverride = this.sgColors
      .add(
         new BoolSetting.Builder()
            .name("show-friend-colors")
            .description("Whether or not to override the distance/health color of friends with the friend color.")
            .defaultValue(true)
            .visible(() -> this.colorMode.get() == ESP.ESPColorMode.Distance || this.colorMode.get() == ESP.ESPColorMode.Health)
            .build()
      );
   private final Setting<SettingColor> nonLivingEntityColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("non-living-entity-color")
            .description("The color used for non living entities such as dropped items.")
            .defaultValue(new SettingColor(25, 25, 25))
            .visible(() -> this.colorMode.get() == ESP.ESPColorMode.Health)
            .build()
      );
   private final Setting<SettingColor> playersColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("players-color")
            .description("The other player's color.")
            .defaultValue(new SettingColor(255, 255, 255))
            .visible(() -> this.colorMode.get() == ESP.ESPColorMode.EntityType)
            .build()
      );
   private final Setting<SettingColor> animalsColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("animals-color")
            .description("The animal's color.")
            .defaultValue(new SettingColor(25, 255, 25, 255))
            .visible(() -> this.colorMode.get() == ESP.ESPColorMode.EntityType)
            .build()
      );
   private final Setting<SettingColor> waterAnimalsColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("water-animals-color")
            .description("The water animal's color.")
            .defaultValue(new SettingColor(25, 25, 255, 255))
            .visible(() -> this.colorMode.get() == ESP.ESPColorMode.EntityType)
            .build()
      );
   private final Setting<SettingColor> monstersColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("monsters-color")
            .description("The monster's color.")
            .defaultValue(new SettingColor(255, 25, 25, 255))
            .visible(() -> this.colorMode.get() == ESP.ESPColorMode.EntityType)
            .build()
      );
   private final Setting<SettingColor> ambientColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("ambient-color")
            .description("The ambient's color.")
            .defaultValue(new SettingColor(25, 25, 25, 255))
            .visible(() -> this.colorMode.get() == ESP.ESPColorMode.EntityType)
            .build()
      );
   private final Setting<SettingColor> miscColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("misc-color")
            .description("The misc color.")
            .defaultValue(new SettingColor(175, 175, 175, 255))
            .visible(() -> this.colorMode.get() == ESP.ESPColorMode.EntityType)
            .build()
      );
   private final Setting<SettingColor> targetColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("target-color")
            .description("The target color.")
            .defaultValue(new SettingColor(200, 200, 200, 255))
            .visible(this.highlightTarget::get)
            .build()
      );
   private final Setting<SettingColor> targetHitboxColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("target-hitbox-color")
            .description("The target hitbox color.")
            .defaultValue(new SettingColor(100, 200, 200, 255))
            .visible(() -> this.highlightTarget.get() && this.targetHitbox.get())
            .build()
      );
   private final Color lineColor = new Color();
   private final Color sideColor = new Color();
   private final Color baseColor = new Color();
   private final Vector3d pos1 = new Vector3d();
   private final Vector3d pos2 = new Vector3d();
   private final Vector3d pos = new Vector3d();
   private int count;

   public ESP() {
      super(Categories.Render, "esp", "Renders entities through walls.");
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mode.get() != ESP.Mode._2D) {
         this.count = 0;
         Entity target = null;
         if (this.highlightTarget.get() && this.targetHitbox.get() && this.mc.hitResult instanceof EntityHitResult hr) {
            target = hr.getEntity();
         }

         for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (target == entity || !this.shouldSkip(entity)) {
               if (target == entity || this.mode.get() == ESP.Mode.Box || this.mode.get() == ESP.Mode.Wireframe) {
                  this.drawBoundingBox(event, entity);
               }

               this.count++;
            }
         }
      }
   }

   private void drawBoundingBox(Render3DEvent event, Entity entity) {
      Color color = this.getColor(entity);
      if (color != null) {
         this.lineColor.set(color);
         this.sideColor.set(color).a((int)(this.sideColor.a * this.fillOpacity.get()));
      }

      if (this.mode.get() == ESP.Mode.Wireframe) {
         WireframeEntityRenderer.render(event, entity, 1.0, this.sideColor, this.lineColor, this.shapeMode.get());
      }

      boolean target = this.drawAsTarget(entity);
      if (this.mode.get() == ESP.Mode.Box || this.targetHitbox.get() && target) {
         double x = Mth.lerp(event.tickDelta, entity.xOld, entity.getX()) - entity.getX();
         double y = Mth.lerp(event.tickDelta, entity.yOld, entity.getY()) - entity.getY();
         double z = Mth.lerp(event.tickDelta, entity.zOld, entity.getZ()) - entity.getZ();
         ShapeMode shape = this.shapeMode.get();
         if (target && this.mode.get() != ESP.Mode.Box) {
            shape = ShapeMode.Lines;
         }

         if (target) {
            this.lineColor.set(this.targetHitboxColor.get());
         }

         AABB box = entity.getBoundingBox();
         event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, this.sideColor, this.lineColor, shape, 0);
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mode.get() == ESP.Mode._2D) {
         Renderer2D.COLOR.begin();
         this.count = 0;

         for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (!this.shouldSkip(entity)) {
               AABB box = entity.getBoundingBox();
               double x = Mth.lerp(event.tickDelta, entity.xOld, entity.getX()) - entity.getX();
               double y = Mth.lerp(event.tickDelta, entity.yOld, entity.getY()) - entity.getY();
               double z = Mth.lerp(event.tickDelta, entity.zOld, entity.getZ()) - entity.getZ();
               this.pos1.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
               this.pos2.set(0.0, 0.0, 0.0);
               if (!this.checkCorner(box.minX + x, box.minY + y, box.minZ + z, this.pos1, this.pos2)
                  && !this.checkCorner(box.maxX + x, box.minY + y, box.minZ + z, this.pos1, this.pos2)
                  && !this.checkCorner(box.minX + x, box.minY + y, box.maxZ + z, this.pos1, this.pos2)
                  && !this.checkCorner(box.maxX + x, box.minY + y, box.maxZ + z, this.pos1, this.pos2)
                  && !this.checkCorner(box.minX + x, box.maxY + y, box.minZ + z, this.pos1, this.pos2)
                  && !this.checkCorner(box.maxX + x, box.maxY + y, box.minZ + z, this.pos1, this.pos2)
                  && !this.checkCorner(box.minX + x, box.maxY + y, box.maxZ + z, this.pos1, this.pos2)
                  && !this.checkCorner(box.maxX + x, box.maxY + y, box.maxZ + z, this.pos1, this.pos2)) {
                  Color color = this.getColor(entity);
                  if (color != null) {
                     this.lineColor.set(color);
                     this.sideColor.set(color).a((int)(this.sideColor.a * this.fillOpacity.get()));
                  }

                  if (this.shapeMode.get() != ShapeMode.Lines && this.sideColor.a > 0) {
                     Renderer2D.COLOR.quad(this.pos1.x, this.pos1.y, this.pos2.x - this.pos1.x, this.pos2.y - this.pos1.y, this.sideColor);
                  }

                  if (this.shapeMode.get() != ShapeMode.Sides) {
                     Renderer2D.COLOR.line(this.pos1.x, this.pos1.y, this.pos1.x, this.pos2.y, this.lineColor);
                     Renderer2D.COLOR.line(this.pos2.x, this.pos1.y, this.pos2.x, this.pos2.y, this.lineColor);
                     Renderer2D.COLOR.line(this.pos1.x, this.pos1.y, this.pos2.x, this.pos1.y, this.lineColor);
                     Renderer2D.COLOR.line(this.pos1.x, this.pos2.y, this.pos2.x, this.pos2.y, this.lineColor);
                  }

                  this.count++;
               }
            }
         }

         Renderer2D.COLOR.render();
      }
   }

   public boolean forceRender() {
      return this.isActive() && (this.mode.get() == ESP.Mode.Shader || this.mode.get() == ESP.Mode.Glow);
   }

   private boolean checkCorner(double x, double y, double z, Vector3d min, Vector3d max) {
      this.pos.set(x, y, z);
      if (!NametagUtils.to2D(this.pos, 1.0)) {
         return true;
      }

      if (this.pos.x < min.x) {
         min.x = this.pos.x;
      }

      if (this.pos.y < min.y) {
         min.y = this.pos.y;
      }

      if (this.pos.z < min.z) {
         min.z = this.pos.z;
      }

      if (this.pos.x > max.x) {
         max.x = this.pos.x;
      }

      if (this.pos.y > max.y) {
         max.y = this.pos.y;
      }

      if (this.pos.z > max.z) {
         max.z = this.pos.z;
      }

      return false;
   }

   public boolean drawAsTarget(Entity entity) {
      return this.highlightTarget.get() && this.mc.hitResult instanceof EntityHitResult hr && hr.getEntity() == entity;
   }

   public boolean shouldSkip(Entity entity) {
      if (this.drawAsTarget(entity)) {
         return false;
      } else if (!this.entities.get().contains(entity.getType())) {
         return true;
      } else if (entity == this.mc.player && this.ignoreSelf.get()) {
         return true;
      } else {
         return entity == this.mc.getCameraEntity() && this.mc.options.getCameraType().isFirstPerson() ? true : !EntityUtils.isInRenderDistance(entity);
      }
   }

   public boolean shouldSkip(EntityType<?> entityType) {
      return !this.entities.get().contains(entityType);
   }

   public Color getColor(Entity entity) {
      double alpha = 1.0;
      Color color;
      if (this.drawAsTarget(entity)) {
         color = this.targetColor.get();
      } else {
         if (!this.entities.get().contains(entity.getType())) {
            return null;
         }

         alpha = this.getFadeAlpha(entity);
         if (alpha == 0.0) {
            return null;
         }

         color = this.getEntityTypeColor(entity);
      }

      return this.baseColor.set(color.r, color.g, color.b, (int)(color.a * alpha));
   }

   private double getFadeAlpha(Entity entity) {
      double dist = PlayerUtils.squaredDistanceToCamera(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ());
      double fadeDist = Math.pow(this.fadeDistance.get(), 2.0);
      double alpha = 1.0;
      if (dist <= fadeDist * fadeDist) {
         alpha = (float)(Math.sqrt(dist) / fadeDist);
      }

      if (alpha <= 0.075) {
         alpha = 0.0;
      }

      return alpha;
   }

   public Color getEntityTypeColor(Entity entity) {
      if (this.colorMode.get() == ESP.ESPColorMode.EntityType) {
         if (entity instanceof Player player) {
            return PlayerUtils.getPlayerColor(player, this.playersColor.get());
         } else {
            return switch (entity.getType().getCategory()) {
               case CREATURE -> (SettingColor)this.animalsColor.get();
               case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> (SettingColor)this.waterAnimalsColor.get();
               case MONSTER -> (SettingColor)this.monstersColor.get();
               case AMBIENT -> (SettingColor)this.ambientColor.get();
               default -> (SettingColor)this.miscColor.get();
            };
         }
      } else if (this.friendOverride.get() && entity instanceof Player player && Friends.get().isFriend(player)) {
         return Config.get().friendColor.get();
      } else {
         return this.colorMode.get() == ESP.ESPColorMode.Health
            ? EntityUtils.getColorFromHealth(entity, this.nonLivingEntityColor.get())
            : EntityUtils.getColorFromDistance(entity);
      }
   }

   @Override
   public String getInfoString() {
      return Integer.toString(this.count);
   }

   public boolean isShader() {
      return this.isActive() && this.mode.get() == ESP.Mode.Shader;
   }

   public boolean isGlow() {
      return this.isActive() && this.mode.get() == ESP.Mode.Glow;
   }

   public enum ESPColorMode {
      EntityType,
      Distance,
      Health;

      @Override
      public String toString() {
         return this == EntityType ? "Entity Type" : super.toString();
      }
   }

   public enum Mode {
      Box,
      Wireframe,
      _2D,
      Shader,
      Glow;

      @Override
      public String toString() {
         return this == _2D ? "2D" : super.toString();
      }
   }
}
