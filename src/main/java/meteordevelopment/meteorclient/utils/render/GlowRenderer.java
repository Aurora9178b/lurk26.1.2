package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class GlowRenderer {
   public static void drawGlow(double x, double y, double width, double height, double radius, Color glowColor, double intensity) {
      int layers = (int)(intensity * 3.0);

      for (int i = 0; i < layers; i++) {
         float glowOffset = i * 1.0F;
         float alphaMultiplier = 1.0F - (float)i / layers;
         int alpha = (int)(glowColor.a * alphaMultiplier * 0.5F);
         Color layerColor = new Color(glowColor.r, glowColor.g, glowColor.b, alpha);
         drawGlowLines(x, y, width, height, glowOffset, layerColor);
      }
   }

   private static void drawGlowLines(double x, double y, double width, double height, float offset, Color color) {
      Renderer2D.COLOR.line(x - offset, y - offset, x + width + offset, y - offset, color);
      Renderer2D.COLOR.line(x + width + offset, y - offset, x + width + offset, y + height + offset, color);
      Renderer2D.COLOR.line(x + width + offset, y + height + offset, x - offset, y + height + offset, color);
      Renderer2D.COLOR.line(x - offset, y + height + offset, x - offset, y - offset, color);
      drawCornerGlow(x, y, offset, color);
      drawCornerGlow(x + width, y, offset, color);
      drawCornerGlow(x + width, y + height, offset, color);
      drawCornerGlow(x, y + height, offset, color);
   }

   private static void drawCornerGlow(double x, double y, float offset, Color color) {
      float size = offset * 0.5F;
      Renderer2D.COLOR.line(x - offset, y - offset, x - offset + size, y - offset, color);
      Renderer2D.COLOR.line(x - offset, y - offset, x - offset, y - offset + size, color);
   }

   public static void drawGlowRect(double x, double y, double width, double height, double radius, Color fillColor, Color glowColor, double intensity) {
      drawGlow(x, y, width, height, radius, glowColor, intensity);
      Renderer2D.COLOR.quad(x, y, width, height, fillColor);
   }
}
