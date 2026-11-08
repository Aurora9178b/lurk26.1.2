package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.utils.render.color.Color;

public class ColorUtils {
   public static Color interpolateColor(Color start, Color end, float blend) {
      int r = (int)(start.r + (end.r - start.r) * blend);
      int g = (int)(start.g + (end.g - start.g) * blend);
      int b = (int)(start.b + (end.b - start.b) * blend);
      int a = (int)(start.a + (end.a - start.a) * blend);
      return new Color(r, g, b, a);
   }
}
