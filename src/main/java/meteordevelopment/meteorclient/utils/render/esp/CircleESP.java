package meteordevelopment.meteorclient.utils.render.esp;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class CircleESP {
   private static final MeshBuilder LINES = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED_LINES);
   private static final MeshBuilder LINES_DEPTH = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH);
   private static final MeshBuilder FACES = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED);
   private static final MeshBuilder FACES_DEPTH = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED_DEPTH);

   private CircleESP() {
   }

   public static void render(PoseStack poseStack, LivingEntity target, float radius, Color sideColor, Color lineColor, float alphaFactor) {
      if (target != null && MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         boolean canSee = MeteorClient.mc.player.hasLineOfSight(target);
         float ticks = (float)(System.currentTimeMillis() % 1000000L * 0.004);
         float alpha = 0.35F + 0.65F * ((Mth.sin(ticks * 1.8F) + 1.0F) * 0.5F) * alphaFactor;
         float clampedAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
         float tickDelta = MeteorClient.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         double x = Mth.lerp(tickDelta, target.xOld, target.getX());
         double y = Mth.lerp(tickDelta, target.yOld, target.getY()) + Math.sin(ticks) + 1.0;
         double z = Mth.lerp(tickDelta, target.zOld, target.getZ());
         Color outlineColor = new Color(lineColor.r, lineColor.g, lineColor.b, Math.round(lineColor.a * clampedAlpha));
         Color fillTop = new Color(sideColor.r, sideColor.g, sideColor.b, Math.round(sideColor.a * clampedAlpha * 0.52F));
         Color fillBottom = new Color(fillTop.r, fillTop.g, fillTop.b, 0);
         int segments = 90;
         double bottomY = y - Math.sin(ticks + 1.0F) / 2.7F;
         double lastX = x + Math.cos(0.0) * radius;
         double lastZ = z + Math.sin(0.0) * radius;
         MeshBuilder lineMesh = canSee ? LINES_DEPTH : LINES;
         MeshBuilder faceMesh = canSee ? FACES_DEPTH : FACES;
         lineMesh.begin();
         faceMesh.begin();
         lineMesh.ensureCapacity(segments * 2, segments * 2);
         faceMesh.ensureCapacity(segments * 4, segments * 6);

         for (int i = 1; i <= segments; i++) {
            double angle = (Math.PI * 2) * i / segments;
            double nextX = x + Math.cos(angle) * radius;
            double nextZ = z + Math.sin(angle) * radius;
            int p1 = lineMesh.vec3(lastX, y, lastZ).color(outlineColor).next();
            int p2 = lineMesh.vec3(nextX, y, nextZ).color(outlineColor).next();
            lineMesh.line(p1, p2);
            if (fillTop.a > 0 || fillBottom.a > 0) {
               int b1 = faceMesh.vec3(lastX, bottomY, lastZ).color(fillBottom).next();
               int t1 = faceMesh.vec3(lastX, y, lastZ).color(fillTop).next();
               int t2 = faceMesh.vec3(nextX, y, nextZ).color(fillTop).next();
               int b2 = faceMesh.vec3(nextX, bottomY, nextZ).color(fillBottom).next();
               faceMesh.quad(b1, t1, t2, b2);
            }

            lastX = nextX;
            lastZ = nextZ;
         }

         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(canSee ? MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH : MeteorRenderPipelines.WORLD_COLORED_LINES)
            .mesh(lineMesh, poseStack)
            .end();
         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(canSee ? MeteorRenderPipelines.WORLD_COLORED_DEPTH : MeteorRenderPipelines.WORLD_COLORED)
            .mesh(faceMesh, poseStack)
            .end();
      }
   }
}
