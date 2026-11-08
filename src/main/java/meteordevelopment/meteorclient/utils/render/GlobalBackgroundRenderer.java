package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import java.nio.ByteBuffer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.FixedUniformStorage;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.DynamicUniformStorage.DynamicUniform;

public final class GlobalBackgroundRenderer {
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private static final FixedUniformStorage<GlobalBackgroundRenderer.MainMenuData> UNIFORM_STORAGE = new FixedUniformStorage<>(
      "Meteor - Main Menu Background UBO", UNIFORM_SIZE, 1
   );
   private static boolean pending;
   private static float time = 10000.0F;

   private GlobalBackgroundRenderer() {
   }

   public static void draw(GuiGraphicsExtractor graphics, int width, int height) {
      if (width > 0 && height > 0) {
         request();
      }
   }

   public static void request() {
      pending = true;
   }

   public static void renderPending() {
      if (pending) {
         pending = false;
         int width = MeteorClient.mc.getWindow().getWidth();
         int height = MeteorClient.mc.getWindow().getHeight();
         if (width > 0 && height > 0) {
            time = time + 0.55F * AnimationUtility.deltaTime();
            UNIFORM_STORAGE.clear();
            GpuBufferSlice data = UNIFORM_STORAGE.write(new GlobalBackgroundRenderer.MainMenuData(width, height, time, 0.0F));
            Utils.unscaledProjection();
            MeshRenderer.begin()
               .attachments(MeteorClient.mc.getMainRenderTarget())
               .pipeline(MeteorRenderPipelines.MAIN_MENU_BACKGROUND)
               .fullscreen()
               .uniform("MainMenuData", data)
               .end();
            Utils.scaledProjection();
         }
      }
   }

   private record MainMenuData(float width, float height, float time, float padding) implements DynamicUniform {
      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer).putVec4(this.width, this.height, this.time, this.padding);
      }
   }
}
