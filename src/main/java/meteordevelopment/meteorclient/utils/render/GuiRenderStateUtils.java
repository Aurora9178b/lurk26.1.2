package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.mixin.GuiGraphicsExtractorAccessor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.ARGB;

public final class GuiRenderStateUtils {
   private GuiRenderStateUtils() {
   }

   public static void beginBlurStratum(GuiGraphicsExtractor graphics) {
      graphics.nextStratum();

      try {
         graphics.blurBeforeThisStratum();
      } catch (IllegalStateException var2) {
      }
   }

   public static void roundedRect(GuiGraphicsExtractor graphics, double x, double y, double width, double height, double radius, Color color) {
      if (!(width <= 0.0) && !(height <= 0.0) && color != null && color.a > 0) {
         GuiRenderState renderState = ((GuiGraphicsExtractorAccessor)graphics).getGuiRenderState();
         ScreenRectangle scissor = ((GuiGraphicsExtractorAccessor)graphics).getScissorStack().peek();
         int packed = ARGB.color(color.a, color.r, color.g, color.b);
         renderState.addGuiElement(
            new RoundedRectangleRenderState(
               graphics.pose(),
               (int)Math.round(x),
               (int)Math.round(y),
               (int)Math.ceil(width),
               (int)Math.ceil(height),
               (float)Math.max(0.0, radius),
               packed,
               scissor
            )
         );
      }
   }
}
