package meteordevelopment.meteorclient.events.render;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEvent {
   private static final Render2DEvent INSTANCE = new Render2DEvent();
   public GuiGraphicsExtractor graphics;
   public int screenWidth;
   public int screenHeight;
   public double frameTime;
   public float tickDelta;

   public static Render2DEvent get(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, float tickDelta) {
      BlurUtil.beginFrame(graphics);
      LiquidGlassUtil.beginFrame(graphics);
      INSTANCE.graphics = graphics;
      INSTANCE.screenWidth = screenWidth;
      INSTANCE.screenHeight = screenHeight;
      INSTANCE.frameTime = Utils.frameTime;
      INSTANCE.tickDelta = tickDelta;
      return INSTANCE;
   }
}
