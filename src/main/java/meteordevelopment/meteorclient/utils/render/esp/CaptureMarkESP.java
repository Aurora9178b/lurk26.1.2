package meteordevelopment.meteorclient.utils.render.esp;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CaptureMarkESP {
   private static final Identifier CAPTURE_MARK_TEXTURE = MeteorClient.identifier("esp/target.png");
   private static final Identifier CAPTURE_ROUNDED_TEXTURE = MeteorClient.identifier("esp/capture_rounded.png");
   private static final Identifier CAPTURE_TRIANGLE_TEXTURE = MeteorClient.identifier("esp/capture_triangle.png");
   private static final MeshBuilder MESH = new MeshBuilder(MeteorRenderPipelines.WORLD_TEXTURED);

   private CaptureMarkESP() {
   }

   public static void render(PoseStack poseStack, LivingEntity target, double espSize, double rotSpeed, double waveSpeed, Color color1, Color color2) {
      render(poseStack, target, espSize, rotSpeed, waveSpeed, color1, color2, CaptureMarkESP.Style.One);
   }

   public static void render(
      PoseStack poseStack, LivingEntity target, double espSize, double rotSpeed, double waveSpeed, Color color1, Color color2, CaptureMarkESP.Style style
   ) {
      if (target != null && MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         double timeSeconds = System.nanoTime() * 1.0E-9;
         float rotation = (float)(-(timeSeconds * rotSpeed * 60.0 % 360.0));
         float tickDelta = MeteorClient.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         double x = Mth.lerp(tickDelta, target.xOld, target.getX());
         double y = Mth.lerp(tickDelta, target.yOld, target.getY()) + target.getBbHeight() * 0.5;
         double z = Mth.lerp(tickDelta, target.zOld, target.getZ());
         float size = (float)espSize * 0.5F;
         Color c1 = getColorForProgress(0.0F, waveSpeed, color1, color2, timeSeconds);
         Color c2 = getColorForProgress(0.25F, waveSpeed, color1, color2, timeSeconds);
         Color c3 = getColorForProgress(0.5F, waveSpeed, color1, color2, timeSeconds);
         Color c4 = getColorForProgress(0.75F, waveSpeed, color1, color2, timeSeconds);
         Camera camera = MeteorClient.mc.gameRenderer.getMainCamera();
         Matrix4f rotationMatrix = new Matrix4f()
            .rotateY((float)Math.toRadians(-camera.yRot()))
            .rotateX((float)Math.toRadians(camera.xRot()))
            .rotateZ((float)Math.toRadians(rotation));
         Vector3f p1 = transform(rotationMatrix, -size, -size);
         Vector3f p2 = transform(rotationMatrix, -size, size);
         Vector3f p3 = transform(rotationMatrix, size, size);
         Vector3f p4 = transform(rotationMatrix, size, -size);
         MESH.begin();
         MESH.ensureQuadCapacity();
         int i1 = MESH.vec3(x + p1.x, y + p1.y, z + p1.z).vec2(0.0, 0.0).color(c1).next();
         int i2 = MESH.vec3(x + p2.x, y + p2.y, z + p2.z).vec2(0.0, 1.0).color(c2).next();
         int i3 = MESH.vec3(x + p3.x, y + p3.y, z + p3.z).vec2(1.0, 1.0).color(c3).next();
         int i4 = MESH.vec3(x + p4.x, y + p4.y, z + p4.z).vec2(1.0, 0.0).color(c4).next();
         MESH.quad(i1, i2, i3, i4);
         AbstractTexture texture = MeteorClient.mc.getTextureManager().getTexture(texture(style));
         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.WORLD_TEXTURED)
            .mesh(MESH, poseStack)
            .sampler("u_Texture", texture.getTextureView(), texture.getSampler())
            .end();
      }
   }

   private static Identifier texture(CaptureMarkESP.Style style) {
      return switch (style) {
         case One -> CAPTURE_MARK_TEXTURE;
         case Two -> CAPTURE_ROUNDED_TEXTURE;
         case Three -> CAPTURE_TRIANGLE_TEXTURE;
      };
   }

   private static Vector3f transform(Matrix4f matrix, float x, float y) {
      return new Vector3f(x, y, 0.0F).mulPosition(matrix);
   }

   private static Color getColorForProgress(float progress, double waveSpeed, Color color1, Color color2, double timeSeconds) {
      float wave = (float)Math.sin(progress * Math.PI * 2.0 + timeSeconds * waveSpeed);
      return interpolate(color1, color2, (wave + 1.0F) / 2.0F);
   }

   private static Color interpolate(Color from, Color to, float progress) {
      progress = Mth.clamp(progress, 0.0F, 1.0F);
      return new Color(
         (int)Mth.lerp(progress, from.r, to.r),
         (int)Mth.lerp(progress, from.g, to.g),
         (int)Mth.lerp(progress, from.b, to.b),
         (int)Mth.lerp(progress, from.a, to.a)
      );
   }

   public enum Style {
      One,
      Two,
      Three;
   }
}
