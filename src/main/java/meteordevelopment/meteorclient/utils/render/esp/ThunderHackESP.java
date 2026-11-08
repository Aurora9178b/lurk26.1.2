package meteordevelopment.meteorclient.utils.render.esp;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class ThunderHackESP {
   private static final MeshBuilder MESH = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED);
   private static final long START_TIME = System.currentTimeMillis();

   private ThunderHackESP() {
   }

   public static void render(PoseStack stack, Entity target) {
      if (target != null && MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         float tickDelta = MeteorClient.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         double x = Mth.lerp(tickDelta, target.xOld, target.getX());
         double y = Mth.lerp(tickDelta, target.yOld, target.getY());
         double z = Mth.lerp(tickDelta, target.zOld, target.getZ());
         double height = target.getBbHeight();
         MESH.begin();

         for (int offset = 0; offset < 360; offset += 120) {
            renderStrip(x, y, z, height, offset);
         }

         MeshRenderer.begin().attachments(MeteorClient.mc.getMainRenderTarget()).pipeline(MeteorRenderPipelines.WORLD_COLORED).mesh(MESH, stack).end();
      }
   }

   private static void renderStrip(double x, double y, double z, double startHeight, int angleOffset) {
      int previousPrevious = -1;
      int previous = -1;
      int stripIndex = 0;

      for (int i = 0; i < 361; i++) {
         double height = startHeight - i * 0.004;
         double nextHeight = startHeight - (i + 1) * 0.004;
         double angle = Math.toRadians((i + angleOffset) % 360);
         double nextAngle = Math.toRadians((i + 1 + angleOffset) % 360);
         float alpha = 1.0F - (i + (float)(System.currentTimeMillis() - START_TIME) / 5.0F) % 360.0F / 60.0F;
         Color color = hudColor((int)(i / 20.0F), (int)(alpha * 255.0F));
         int first = addStripVertex(x + Math.cos(angle) * 0.5, y + height, z + Math.sin(angle) * 0.5, color);
         if (previousPrevious != -1) {
            addStripTriangle(previousPrevious, previous, first, stripIndex);
         }

         previousPrevious = previous;
         previous = first;
         stripIndex++;
         int second = addStripVertex(x + Math.cos(nextAngle) * 0.5, y + nextHeight + 0.1F, z + Math.sin(nextAngle) * 0.5, color);
         if (previousPrevious != -1) {
            addStripTriangle(previousPrevious, previous, second, stripIndex);
         }

         previousPrevious = previous;
         previous = second;
         stripIndex++;
      }
   }

   private static int addStripVertex(double x, double y, double z, Color color) {
      MESH.ensureCapacity(1, 3);
      return MESH.vec3(x, y, z).color(color).next();
   }

   private static void addStripTriangle(int first, int second, int third, int stripIndex) {
      if ((stripIndex & 1) == 0) {
         MESH.triangle(first, second, third);
      } else {
         MESH.triangle(second, first, third);
      }
   }

   private static Color hudColor(int count, int alpha) {
      int angle = (int)((System.currentTimeMillis() / 18L + count) % 360L);
      float hue = angle / 360.0F;
      java.awt.Color color = java.awt.Color.getHSBColor(hue, 0.0F, 0.58431375F);
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Mth.clamp(alpha, 0, 255));
   }
}
