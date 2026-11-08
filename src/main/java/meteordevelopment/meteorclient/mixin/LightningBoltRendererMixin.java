package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBoltRenderer.class)
public abstract class LightningBoltRendererMixin {
   @Inject(method = "quad", at = @At("HEAD"), cancellable = true)
   private static void onSetLightningVertex(
      Matrix4fc pose,
      VertexConsumer buffer,
      float xo0,
      float zo0,
      int h,
      float xo1,
      float zo1,
      float boltRed,
      float boltGreen,
      float boltBlue,
      float rr1,
      float rr2,
      boolean px1,
      boolean pz1,
      boolean px2,
      boolean pz2,
      CallbackInfo ci
   ) {
      Ambience ambience = Modules.get().get(Ambience.class);
      if (ambience.isActive() && ambience.changeLightningColor.get()) {
         Color color = ambience.lightningColor.get();
         buffer.addVertex(pose, xo0 + (px1 ? rr2 : -rr2), h * 16, zo0 + (pz1 ? rr2 : -rr2))
            .setColor(color.r / 255.0F, color.g / 255.0F, color.b / 255.0F, 0.3F);
         buffer.addVertex(pose, h + (px1 ? rr1 : -rr1), (h + 1) * 16, zo1 + (pz1 ? rr1 : -rr1))
            .setColor(color.r / 255.0F, color.g / 255.0F, color.b / 255.0F, 0.3F);
         buffer.addVertex(pose, h + (px2 ? rr1 : -rr1), (h + 1) * 16, zo1 + (pz2 ? rr1 : -rr1))
            .setColor(color.r / 255.0F, color.g / 255.0F, color.b / 255.0F, 0.3F);
         buffer.addVertex(pose, xo0 + (px2 ? rr2 : -rr2), h * 16, zo0 + (pz2 ? rr2 : -rr2))
            .setColor(color.r / 255.0F, color.g / 255.0F, color.b / 255.0F, 0.3F);
         ci.cancel();
      }
   }
}
