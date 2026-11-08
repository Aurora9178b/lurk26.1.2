package meteordevelopment.meteorclient.systems.modules.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class JumpCircle extends Module {
   private static final Identifier DEFAULT_TEXTURE = MeteorClient.identifier("textures/particles/circle.png");
   private static final Identifier PORTAL_TEXTURE = MeteorClient.identifier("textures/particles/hitbubble.png");
   private static final MeshBuilder MESH = new MeshBuilder(MeteorRenderPipelines.WORLD_TEXTURED_LIGHTNING);
   private static final long EASE_OUT_LIFETIME_MS = 5000L;
   private static final long NORMAL_LIFETIME_MS = 6000L;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<JumpCircle.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("Texture style for the jump circle."))
               .defaultValue(JumpCircle.Mode.Default))
            .build()
      );
   private final Setting<Boolean> easeOut = this.sgGeneral
      .add(new BoolSetting.Builder().name("ease-out").description("Shrinks the circle again near the end of its lifetime.").defaultValue(true).build());
   private final Setting<Double> rotateSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rotate-speed")
            .description("How fast the circle rotates.")
            .defaultValue(2.0)
            .range(0.5, 5.0)
            .sliderRange(0.5, 5.0)
            .build()
      );
   private final Setting<Double> circleScale = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("circle-scale")
            .description("The size of the circle animation.")
            .defaultValue(1.0)
            .range(0.5, 5.0)
            .sliderRange(0.5, 5.0)
            .build()
      );
   private final Setting<Boolean> onlySelf = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-self").description("Only renders jump circles for you.").defaultValue(false).build());
   private final Setting<SettingColor> color = this.sgGeneral
      .add(new ColorSetting.Builder().name("color").description("The first jump circle color.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
   private final Setting<SettingColor> secondColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("second-color")
            .description("The second jump circle color.")
            .defaultValue(new SettingColor(149, 149, 149, 255))
            .build()
      );
   private final List<JumpCircle.Circle> circles = new ArrayList<>();
   private final Set<UUID> groundedPlayers = new HashSet<>();

   public JumpCircle() {
      super(Categories.Render, "JumpCircle", "Renders a circle when players jump.");
   }

   @Override
   public void onActivate() {
      this.circles.clear();
      this.groundedPlayers.clear();
   }

   @Override
   public void onDeactivate() {
      this.circles.clear();
      this.groundedPlayers.clear();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.level != null && this.mc.player != null) {
         long now = System.currentTimeMillis();
         long lifetime = this.easeOut.get() ? 5000L : 6000L;
         this.circles.removeIf(circle -> now - circle.createdAt > lifetime);
         Set<UUID> seenPlayers = new HashSet<>();

         for (Player player : this.mc.level.players()) {
            if (!this.onlySelf.get() || player == this.mc.player) {
               UUID uuid = player.getUUID();
               seenPlayers.add(uuid);
               if (player.onGround()) {
                  this.groundedPlayers.add(uuid);
               } else if (this.groundedPlayers.remove(uuid)) {
                  this.circles.add(new JumpCircle.Circle(new Vec3(player.getX(), Math.floor(player.getY()) + 0.001, player.getZ()), now));
               }
            }
         }

         this.groundedPlayers.retainAll(seenPlayers);
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.level != null && this.mc.player != null && !this.circles.isEmpty()) {
         long now = System.currentTimeMillis();
         Identifier textureId = this.mode.get() == JumpCircle.Mode.Portal ? PORTAL_TEXTURE : DEFAULT_TEXTURE;
         MESH.begin();
         MESH.ensureCapacity(this.circles.size() * 4, this.circles.size() * 6);

         for (int i = this.circles.size() - 1; i >= 0; i--) {
            this.renderCircle(this.circles.get(i), now);
         }

         AbstractTexture texture = this.mc.getTextureManager().getTexture(textureId);
         MeshRenderer.begin()
            .attachments(this.mc.getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.WORLD_TEXTURED_LIGHTNING)
            .mesh(MESH, event.matrices)
            .sampler("u_Texture", texture.getTextureView(), texture.getSampler())
            .end();
      }
   }

   private void renderCircle(JumpCircle.Circle circle, long now) {
      long elapsed = now - circle.createdAt;
      float fade = 1.0F - Mth.clamp((float)elapsed / 6000.0F, 0.0F, 1.0F);
      float size = (float)(this.circleScale.get() - Math.pow(1.0 - elapsed * (this.easeOut.get() ? 2.0 : 1.0) / 5000.0, 4.0));
      if (!(size <= 0.001F) && !(fade <= 0.0F)) {
         float angle = (float)Math.toRadians(size * this.rotateSpeed.get().floatValue() * 1000.0F);
         Vector3f p1 = rotate(-size, size, angle);
         Vector3f p2 = rotate(size, size, angle);
         Vector3f p3 = rotate(size, -size, angle);
         Vector3f p4 = rotate(-size, -size, angle);
         Vec3 pos = circle.pos;
         Color c1 = withFade(this.color.get(), fade);
         Color c2 = withFade(this.secondColor.get(), fade);
         int i1 = MESH.vec3(pos.x + p1.x, pos.y, pos.z + p1.z).vec2(0.0, 1.0).color(c1).next();
         int i2 = MESH.vec3(pos.x + p2.x, pos.y, pos.z + p2.z).vec2(1.0, 1.0).color(c2).next();
         int i3 = MESH.vec3(pos.x + p3.x, pos.y, pos.z + p3.z).vec2(1.0, 0.0).color(c1).next();
         int i4 = MESH.vec3(pos.x + p4.x, pos.y, pos.z + p4.z).vec2(0.0, 0.0).color(c2).next();
         MESH.quad(i1, i2, i3, i4);
      }
   }

   private static Vector3f rotate(float x, float z, float angle) {
      float cos = Mth.cos(angle);
      float sin = Mth.sin(angle);
      return new Vector3f(x * cos - z * sin, 0.0F, x * sin + z * cos);
   }

   private static Color withFade(Color color, float fade) {
      return new Color(color.r, color.g, color.b, (int)(color.a * Mth.clamp(fade, 0.0F, 1.0F)));
   }

   private record Circle(Vec3 pos, long createdAt) {
   }

   public enum Mode {
      Default,
      Portal,
      Custom;
   }
}
