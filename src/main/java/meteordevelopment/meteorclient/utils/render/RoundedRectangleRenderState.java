package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public record RoundedRectangleRenderState(
   Matrix3x2f pose, int x, int y, int width, int height, float radius, int color, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public RoundedRectangleRenderState(Matrix3x2fc pose, int x, int y, int width, int height, float radius, int color, @Nullable ScreenRectangle scissorArea) {
      this(new Matrix3x2f(pose), x, y, width, height, radius, color, scissorArea, getBounds(x, y, width, height, pose, scissorArea));
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      if (this.width > 0 && this.height > 0) {
         float safeRadius = Math.max(0.0F, Math.min(this.radius, Math.min(this.width, this.height) * 0.5F));
         if (safeRadius <= 0.5F) {
            this.addQuad(vertexConsumer, this.x, this.y, this.x + this.width, this.y + this.height);
         } else {
            int segments = Math.max(8, Math.min(32, (int)Math.ceil(safeRadius * 2.0F)));
            float bottom = this.y;
            float rectHeight = this.height;

            for (int i = 0; i < segments; i++) {
               float y0 = bottom + rectHeight * i / segments;
               float y1 = bottom + rectHeight * (i + 1) / segments;
               float midY = (y0 + y1) * 0.5F - bottom;
               float inset = 0.0F;
               if (midY < safeRadius) {
                  float dy = safeRadius - midY;
                  inset = safeRadius - (float)Math.sqrt(Math.max(0.0F, safeRadius * safeRadius - dy * dy));
               } else if (midY > rectHeight - safeRadius) {
                  float dy = midY - (rectHeight - safeRadius);
                  inset = safeRadius - (float)Math.sqrt(Math.max(0.0F, safeRadius * safeRadius - dy * dy));
               }

               this.addQuad(vertexConsumer, this.x + inset, y0, this.x + this.width - inset, y1);
            }
         }
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return RenderPipelines.GUI;
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   private void addQuad(VertexConsumer vertexConsumer, float x0, float y0, float x1, float y1) {
      vertexConsumer.addVertexWith2DPose(this.pose, x0, y0).setColor(this.color);
      vertexConsumer.addVertexWith2DPose(this.pose, x0, y1).setColor(this.color);
      vertexConsumer.addVertexWith2DPose(this.pose, x1, y1).setColor(this.color);
      vertexConsumer.addVertexWith2DPose(this.pose, x1, y0).setColor(this.color);
   }

   @Nullable
   private static ScreenRectangle getBounds(int x, int y, int width, int height, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
      ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
      return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
   }
}
