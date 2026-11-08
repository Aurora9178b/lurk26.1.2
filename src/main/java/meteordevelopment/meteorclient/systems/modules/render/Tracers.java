package meteordevelopment.meteorclient.systems.modules.render;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
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
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2f;
import org.joml.Vector3d;

public class Tracers extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgAppearance = this.settings.createGroup("Appearance");
   private final SettingGroup sgColors = this.settings.createGroup("Colors");
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("entities").description("Select specific entities.").defaultValue(EntityType.PLAYER).build());
   private final Setting<Boolean> ignoreSelf = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("ignore-self")
            .description("Doesn't draw tracers to yourself when in third person or freecam.")
            .defaultValue(false)
            .build()
      );
   public final Setting<Boolean> ignoreFriends = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-friends").description("Doesn't draw tracers to friends.").defaultValue(false).build());
   public final Setting<Boolean> showInvis = this.sgGeneral
      .add(new BoolSetting.Builder().name("show-invisible").description("Shows invisible entities.").defaultValue(true).build());
   private final Setting<Tracers.TracerStyle> style = this.sgAppearance
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("style"))
                  .description("What display mode should be used"))
               .defaultValue(Tracers.TracerStyle.Lines))
            .build()
      );
   private final Setting<Target> target = this.sgAppearance
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target"))
                     .description("What part of the entity to target."))
                  .defaultValue(Target.Body))
               .visible(() -> this.style.get() == Tracers.TracerStyle.Lines))
            .build()
      );
   private final Setting<Boolean> stem = this.sgAppearance
      .add(
         new BoolSetting.Builder()
            .name("stem")
            .description("Draw a line through the center of the tracer target.")
            .defaultValue(true)
            .visible(() -> this.style.get() == Tracers.TracerStyle.Lines)
            .build()
      );
   private final Setting<Integer> maxDist = this.sgAppearance
      .add(new IntSetting.Builder().name("max-distance").description("Maximum distance for tracers to show.").defaultValue(256).min(0).sliderMax(256).build());
   private final Setting<Integer> distanceOffscreen = this.sgAppearance
      .add(
         new IntSetting.Builder()
            .name("distance-offscreen")
            .description("Offscreen's distance from center.")
            .defaultValue(200)
            .min(0)
            .sliderMax(500)
            .visible(() -> this.style.get() == Tracers.TracerStyle.Offscreen)
            .build()
      );
   private final Setting<Integer> sizeOffscreen = this.sgAppearance
      .add(
         new IntSetting.Builder()
            .name("size-offscreen")
            .description("Offscreen's size.")
            .defaultValue(10)
            .min(2)
            .sliderMax(50)
            .visible(() -> this.style.get() == Tracers.TracerStyle.Offscreen)
            .build()
      );
   private final Setting<Boolean> blinkOffscreen = this.sgAppearance
      .add(
         new BoolSetting.Builder()
            .name("blink-offscreen")
            .description("Make offscreen Blink.")
            .defaultValue(true)
            .visible(() -> this.style.get() == Tracers.TracerStyle.Offscreen)
            .build()
      );
   private final Setting<Double> blinkOffscreenSpeed = this.sgAppearance
      .add(
         new DoubleSetting.Builder()
            .name("blink-offscreen-speed")
            .description("Offscreen's blink speed.")
            .defaultValue(4.0)
            .min(1.0)
            .sliderMax(15.0)
            .visible(() -> this.style.get() == Tracers.TracerStyle.Offscreen && this.blinkOffscreen.get())
            .build()
      );
   public final Setting<Boolean> distance = this.sgColors
      .add(new BoolSetting.Builder().name("distance-colors").description("Changes the color of tracers depending on distance.").defaultValue(false).build());
   public final Setting<Boolean> friendOverride = this.sgColors
      .add(
         new BoolSetting.Builder()
            .name("show-friend-colors")
            .description("Whether or not to override the distance color of friends with the friend color.")
            .defaultValue(true)
            .visible(() -> this.distance.get() && !this.ignoreFriends.get())
            .build()
      );
   private final Setting<SettingColor> playersColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("players-colors")
            .description("The player's color.")
            .defaultValue(new SettingColor(205, 205, 205, 127))
            .visible(() -> !this.distance.get())
            .build()
      );
   private final Setting<SettingColor> animalsColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("animals-color")
            .description("The animal's color.")
            .defaultValue(new SettingColor(145, 255, 145, 127))
            .visible(() -> !this.distance.get())
            .build()
      );
   private final Setting<SettingColor> waterAnimalsColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("water-animals-color")
            .description("The water animal's color.")
            .defaultValue(new SettingColor(145, 145, 255, 127))
            .visible(() -> !this.distance.get())
            .build()
      );
   private final Setting<SettingColor> monstersColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("monsters-color")
            .description("The monster's color.")
            .defaultValue(new SettingColor(255, 145, 145, 127))
            .visible(() -> !this.distance.get())
            .build()
      );
   private final Setting<SettingColor> ambientColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("ambient-color")
            .description("The ambient color.")
            .defaultValue(new SettingColor(75, 75, 75, 127))
            .visible(() -> !this.distance.get())
            .build()
      );
   private final Setting<SettingColor> miscColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("misc-color")
            .description("The misc color.")
            .defaultValue(new SettingColor(145, 145, 145, 127))
            .visible(() -> !this.distance.get())
            .build()
      );
   private int count;
   private final Instant initTimer = Instant.now();

   public Tracers() {
      super(Categories.Render, "tracers", "Displays tracer lines to specified entities.");
   }

   private boolean shouldBeIgnored(Entity entity) {
      return !PlayerUtils.isWithin(entity, this.maxDist.get().intValue())
         || !Modules.get().isActive(Freecam.class) && entity == this.mc.player
         || !this.entities.get().contains(entity.getType())
         || this.ignoreSelf.get() && entity == this.mc.player
         || this.ignoreFriends.get() && entity instanceof Player player && Friends.get().isFriend(player)
         || !this.showInvis.get() && entity.isInvisible()
         || !EntityUtils.isInRenderDistance(entity);
   }

   private Color getEntityColor(Entity entity) {
      Color color;
      if (this.distance.get()) {
         if (this.friendOverride.get() && entity instanceof Player player && Friends.get().isFriend(player)) {
            color = Config.get().friendColor.get();
         } else {
            color = EntityUtils.getColorFromDistance(entity);
         }
      } else if (entity instanceof Player player) {
         color = PlayerUtils.getPlayerColor(player, this.playersColor.get());
      } else {
         color = switch (entity.getType().getCategory()) {
            case CREATURE -> (SettingColor)this.animalsColor.get();
            case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> (SettingColor)this.waterAnimalsColor.get();
            case MONSTER -> (SettingColor)this.monstersColor.get();
            case AMBIENT -> (SettingColor)this.ambientColor.get();
            default -> (SettingColor)this.miscColor.get();
         };
      }

      return new Color(color);
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (!this.mc.options.hideGui && this.style.get() != Tracers.TracerStyle.Offscreen) {
         this.count = 0;

         for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (!this.shouldBeIgnored(entity)) {
               Color color = this.getEntityColor(entity);
               double x = entity.xo + (entity.getX() - entity.xo) * event.tickDelta;
               double y = entity.yo + (entity.getY() - entity.yo) * event.tickDelta;
               double z = entity.zo + (entity.getZ() - entity.zo) * event.tickDelta;
               double height = entity.getBoundingBox().maxY - entity.getBoundingBox().minY;
               if (this.target.get() == Target.Head) {
                  y += height;
               } else if (this.target.get() == Target.Body) {
                  y += height / 2.0;
               }

               event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x, y, z, color);
               if (this.stem.get()) {
                  event.renderer.line(x, entity.getY(), z, x, entity.getY() + height, z, color);
               }

               this.count++;
            }
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent event) {
      if (!this.mc.options.hideGui && this.style.get() == Tracers.TracerStyle.Offscreen) {
         this.count = 0;
         Renderer2D.COLOR.begin();

         for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (!this.shouldBeIgnored(entity)) {
               Color color = this.getEntityColor(entity);
               if (this.blinkOffscreen.get()) {
                  color.a = (int)(color.a * this.getAlpha());
               }

               Vec2 screenCenter = new Vec2(this.mc.getWindow().getWidth() / 2.0F, this.mc.getWindow().getHeight() / 2.0F);
               Vector3d projection = new Vector3d(entity.xo, entity.yo, entity.zo);
               boolean projSucceeded = NametagUtils.to2D(projection, 1.0, false, false);
               if (!projSucceeded
                  || !(projection.x > 0.0)
                  || !(projection.x < this.mc.getWindow().getWidth())
                  || !(projection.y > 0.0)
                  || !(projection.y < this.mc.getWindow().getHeight())) {
                  projection = new Vector3d(entity.xo, entity.yo, entity.zo);
                  NametagUtils.to2D(projection, 1.0, false, true);
                  Vector2f angle = this.vectorAngles(new Vector3d(screenCenter.x - projection.x, screenCenter.y - projection.y, 0.0));
                  angle.y += 180.0F;
                  float angleYawRad = (float)Math.toRadians(angle.y);
                  Vector2f newPoint = new Vector2f(
                     screenCenter.x + this.distanceOffscreen.get().intValue() * (float)Math.cos(angleYawRad),
                     screenCenter.y + this.distanceOffscreen.get().intValue() * (float)Math.sin(angleYawRad)
                  );
                  Vector2f[] trianglePoints = new Vector2f[]{
                     new Vector2f(newPoint.x - this.sizeOffscreen.get().intValue(), newPoint.y - this.sizeOffscreen.get().intValue()),
                     new Vector2f(newPoint.x + this.sizeOffscreen.get().intValue() * 0.73205F, newPoint.y),
                     new Vector2f(newPoint.x - this.sizeOffscreen.get().intValue(), newPoint.y + this.sizeOffscreen.get().intValue())
                  };
                  this.rotateTriangle(trianglePoints, angle.y);
                  Renderer2D.COLOR
                     .triangle(
                        trianglePoints[2].x, trianglePoints[2].y, trianglePoints[1].x, trianglePoints[1].y, trianglePoints[0].x, trianglePoints[0].y, color
                     );
                  this.count++;
               }
            }
         }

         Renderer2D.COLOR.render();
      }
   }

   private void rotateTriangle(Vector2f[] points, float ang) {
      Vector2f triangleCenter = new Vector2f(0.0F, 0.0F);
      triangleCenter.add(points[0]).add(points[1]).add(points[2]).div(3.0F);
      float theta = (float)Math.toRadians(ang);
      float cos = (float)Math.cos(theta);
      float sin = (float)Math.sin(theta);

      for (int i = 0; i < 3; i++) {
         Vector2f point = new Vector2f(points[i].x, points[i].y).sub(triangleCenter);
         Vector2f newPoint = new Vector2f(point.x * cos - point.y * sin, point.x * sin + point.y * cos);
         newPoint.add(triangleCenter);
         points[i] = newPoint;
      }
   }

   private Vector2f vectorAngles(Vector3d forward) {
      float yaw;
      float pitch;
      if (forward.x == 0.0 && forward.y == 0.0) {
         yaw = 0.0F;
         if (forward.z > 0.0) {
            pitch = 270.0F;
         } else {
            pitch = 90.0F;
         }
      } else {
         yaw = (float)(Math.atan2(forward.y, forward.x) * 180.0 / Math.PI);
         if (yaw < 0.0F) {
            yaw += 360.0F;
         }

         float tmp = (float)Math.sqrt(forward.x * forward.x + forward.y * forward.y);
         pitch = (float)(Math.atan2(-forward.z, tmp) * 180.0 / Math.PI);
         if (pitch < 0.0F) {
            pitch += 360.0F;
         }
      }

      return new Vector2f(pitch, yaw);
   }

   private float getAlpha() {
      double speed = this.blinkOffscreenSpeed.get() / 4.0;
      double duration = Math.abs(Duration.between(Instant.now(), this.initTimer).toMillis()) * speed;
      return (float)Math.abs(duration % 1000.0 - 500.0) / 500.0F;
   }

   @Override
   public String getInfoString() {
      return Integer.toString(this.count);
   }

   public enum TracerStyle {
      Lines,
      Offscreen;
   }
}
