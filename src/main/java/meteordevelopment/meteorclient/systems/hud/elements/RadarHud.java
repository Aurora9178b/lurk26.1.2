package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RadarHud extends HudElement {
   public static final HudElementInfo<RadarHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "radar", "Radar", "Displays a square radar for nearby players.", RadarHud::new
   );
   private static final Color LINE_COLOR = new Color(255, 255, 255, 80);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<RadarHud.RenderMode> renderMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("render-mode"))
                  .description("The render mode used for the radar background."))
               .defaultValue(RadarHud.RenderMode.Blurred))
            .build()
      );
   private final Setting<Boolean> drawBackground = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("draw-background")
            .description("Draws a colored background behind shader modes.")
            .defaultValue(true)
            .visible(() -> this.renderMode.get() == RadarHud.RenderMode.Shader || this.renderMode.get() == RadarHud.RenderMode.BlurredShader)
            .build()
      );
   private final Setting<Integer> radius = this.sgGeneral
      .add(new IntSetting.Builder().name("radius").description("Corner radius.").defaultValue(5).min(0).sliderRange(0, 20).build());
   private final Setting<Integer> size = this.sgGeneral
      .add(new IntSetting.Builder().name("size").description("Radar size.").defaultValue(80).min(20).sliderRange(50, 300).build());
   private final Setting<SettingColor> playerColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("player-color")
            .description("Color used for non-friend players.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
      );
   private final Setting<SettingColor> bgColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("bg-color").description("Color used for the radar background.").defaultValue(new SettingColor(0, 0, 0, 150)).build());

   private RadarHud() {
      super(INFO);
      this.setSize(this.size.get().intValue(), this.size.get().intValue());
   }

   @Override
   public void render(HudRenderer renderer) {
      int s = this.size.get();
      double x = this.getX();
      double y = this.getY();
      double r = this.radius.get().intValue();
      this.drawBackground(renderer, x, y, s, r);
      this.drawCross(renderer, x, y, s);
      this.drawPlayers(renderer, x, y, s);
      this.setSize(s, s);
   }

   private void drawBackground(HudRenderer renderer, double x, double y, int size, double radius) {
      if (this.renderMode.get() == RadarHud.RenderMode.Shader) {
         if (this.drawBackground.get()) {
            Render2DUtil.drawRoundedRect(renderer, x, y, size, size, radius, this.bgColor.get());
         }
      } else if (this.renderMode.get() == RadarHud.RenderMode.BlurredShader) {
         BlurUtil.render((int)Math.round(x), (int)Math.round(y), size, size, (float)radius);
         if (this.drawBackground.get()) {
            Render2DUtil.drawRoundedRect(renderer, x, y, size, size, radius, this.bgColor.get());
         }
      } else {
         BlurUtil.render((int)Math.round(x), (int)Math.round(y), size, size, (float)radius);
      }
   }

   private void drawCross(HudRenderer renderer, double x, double y, int size) {
      double half = size / 2.0;
      renderer.quad(x, y + half - 1.0, size, 2.0, LINE_COLOR);
      renderer.quad(x + half - 1.0, y, 2.0, size, LINE_COLOR);
   }

   private void drawPlayers(HudRenderer renderer, double x, double y, int size) {
      if (MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         float tickDelta = MeteorClient.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 playerPos = lerpedPosition(MeteorClient.mc.player, tickDelta);
         float yaw = MeteorClient.mc.player.getYRot(tickDelta);
         float cos = Mth.cos(yaw * (float) (Math.PI / 180.0));
         float sin = Mth.sin(yaw * (float) (Math.PI / 180.0));
         float limitX = size / 2.0F - 5.0F;
         float limitY = size / 2.0F - 6.0F;

         for (AbstractClientPlayer player : MeteorClient.mc.level.players()) {
            if (player != MeteorClient.mc.player) {
               Vec3 entityPos = lerpedPosition(player, tickDelta);
               float posX = (float)(entityPos.x - playerPos.x) * 2.0F;
               float posZ = (float)(entityPos.z - playerPos.z) * 2.0F;
               float rotY = -(posZ * cos - posX * sin);
               float rotX = -(posX * cos + posZ * sin);
               rotX = Mth.clamp(rotX, -limitX, limitX);
               rotY = Mth.clamp(rotY, -limitY, limitY);
               Color color = Friends.get().isFriend(player) ? Config.get().friendColor.get() : this.playerColor.get();
               double dotX = x + size / 2.0 + rotX - 2.0;
               double dotY = y + size / 2.0 + rotY - 2.0;
               Render2DUtil.drawRoundedRect(renderer, dotX, dotY, 4.0, 4.0, 2.0, color);
            }
         }
      }
   }

   private static Vec3 lerpedPosition(AbstractClientPlayer player, float tickDelta) {
      double x = Mth.lerp(tickDelta, player.xo, player.getX());
      double y = Mth.lerp(tickDelta, player.yo, player.getY());
      double z = Mth.lerp(tickDelta, player.zo, player.getZ());
      return new Vec3(x, y, z);
   }

   public enum RenderMode {
      Blurred,
      Shader,
      BlurredShader;
   }
}
