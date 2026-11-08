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

public class FireflyESP {
   private static final Identifier FIREFLY_TEX = MeteorClient.identifier("textures/particles/firefly.png");
   private static final MeshBuilder MESH = new MeshBuilder(MeteorRenderPipelines.WORLD_TEXTURED_LIGHTNING);

   private FireflyESP() {
   }

   public static void render(
      PoseStack stack,
      LivingEntity target,
      int espLength,
      int factor,
      double shaking,
      double amplitude,
      Color color,
      FireflyESP.ColorMode colorMode,
      Color secondColor,
      double colorMix,
      double colorSpeed,
      double rainbowSpeed,
      double rainbowSaturation,
      double rainbowBrightness
   ) {
      if (target != null && MeteorClient.mc.level != null && MeteorClient.mc.player != null) {
         boolean canSee = MeteorClient.mc.player.hasLineOfSight(target);
         Camera camera = MeteorClient.mc.gameRenderer.getMainCamera();
         float tickDelta = MeteorClient.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         double x = Mth.lerp(tickDelta, target.xOld, target.getX());
         double y = Mth.lerp(tickDelta, target.yOld, target.getY());
         double z = Mth.lerp(tickDelta, target.zOld, target.getZ());
         float age = target.tickCount - 1 + tickDelta;
         int quadCount = 3 * (espLength + 1);
         MESH.begin();
         MESH.ensureCapacity(quadCount * 4, quadCount * 6);
         Matrix4f rotationMatrix = new Matrix4f().rotateY((float)Math.toRadians(-camera.yRot())).rotateX((float)Math.toRadians(camera.xRot()));

         for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i <= espLength; i++) {
               double radians = Math.toRadians(((i / 1.5F + age) * factor + ring * 120) % (factor * 360));
               double wobble = Math.sin(Math.toRadians(age * 2.5F + i * (ring + 1)) * amplitude) / shaking;
               float progress = espLength <= 0 ? 0.0F : (float)i / espLength;
               float scale = Math.max(0.24F * progress, 0.2F);
               Color renderColor = resolveColor(
                  age,
                  i,
                  ring,
                  espLength,
                  colorMode,
                  color,
                  secondColor,
                  (float)colorMix,
                  (float)colorSpeed,
                  (float)rainbowSpeed,
                  (float)rainbowSaturation,
                  (float)rainbowBrightness
               );
               Vector3f p1 = transform(rotationMatrix, -scale, -scale);
               Vector3f p2 = transform(rotationMatrix, -scale, scale);
               Vector3f p3 = transform(rotationMatrix, scale, scale);
               Vector3f p4 = transform(rotationMatrix, scale, -scale);
               double px = x + Math.cos(radians) * target.getBbWidth();
               double py = y + target.getBbHeight() * 0.5 + wobble;
               double pz = z + Math.sin(radians) * target.getBbWidth();
               int i1 = MESH.vec3(px + p1.x, py + p1.y, pz + p1.z).vec2(0.0, 0.0).color(renderColor).next();
               int i2 = MESH.vec3(px + p2.x, py + p2.y, pz + p2.z).vec2(0.0, 1.0).color(renderColor).next();
               int i3 = MESH.vec3(px + p3.x, py + p3.y, pz + p3.z).vec2(1.0, 1.0).color(renderColor).next();
               int i4 = MESH.vec3(px + p4.x, py + p4.y, pz + p4.z).vec2(1.0, 0.0).color(renderColor).next();
               MESH.quad(i1, i2, i3, i4);
            }
         }

         AbstractTexture texture = MeteorClient.mc.getTextureManager().getTexture(FIREFLY_TEX);
         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(canSee ? MeteorRenderPipelines.WORLD_TEXTURED_LIGHTNING_DEPTH : MeteorRenderPipelines.WORLD_TEXTURED_LIGHTNING)
            .mesh(MESH, stack)
            .sampler("u_Texture", texture.getTextureView(), texture.getSampler())
            .end();
      }
   }

   private static Color resolveColor(
      float age,
      int index,
      int ringIndex,
      int espLength,
      FireflyESP.ColorMode mode,
      Color primaryColor,
      Color secondaryColor,
      float mixAmount,
      float blendSpeed,
      float rainbowSpeed,
      float rainbowSaturation,
      float rainbowBrightness
   ) {
      float progress = espLength <= 0 ? 0.0F : (float)index / espLength;
      float clampedMix = Mth.clamp(mixAmount, 0.0F, 1.0F);

      return switch (mode) {
         case Solid -> new Color(primaryColor);
         case Blend -> {
            float wave = (Mth.sin(age * blendSpeed * 0.25F + progress * (float) (Math.PI * 2) + ringIndex) + 1.0F) * 0.5F;
            float blendProgress = Mth.clamp(wave * clampedMix, 0.0F, 1.0F);
            yield interpolate(primaryColor, secondaryColor, blendProgress);
         }
         case Rainbow -> {
            float hue = age * 0.01F * rainbowSpeed + progress + ringIndex * 0.17F;
            hue -= (float)Math.floor(hue);
            java.awt.Color rainbow = java.awt.Color.getHSBColor(hue, Mth.clamp(rainbowSaturation, 0.0F, 1.0F), Mth.clamp(rainbowBrightness, 0.0F, 1.0F));
            yield new Color(rainbow.getRed(), rainbow.getGreen(), rainbow.getBlue(), primaryColor.a);
         }
      };
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

   private static Vector3f transform(Matrix4f matrix, float x, float y) {
      return new Vector3f(x, y, 0.0F).mulPosition(matrix);
   }

   public enum ColorMode {
      Solid,
      Blend,
      Rainbow;
   }
}
