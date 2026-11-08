package meteordevelopment.meteorclient.utils.render;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.client.BlurSetting;
import meteordevelopment.meteorclient.utils.render.color.Color;

public final class ChatBackgroundRenderer {
   private static final List<ChatBackgroundRenderer.Entry> ENTRIES = new ArrayList<>();

   private ChatBackgroundRenderer() {
   }

   public static void request(double x, double y, double width, double height, double radius, Color color) {
      request(x, y, width, height, radius, color, 1.0F);
   }

   public static void request(double x, double y, double width, double height, double radius, Color color, float blurMultiplier) {
      if (!(width <= 0.0) && !(height <= 0.0) && color != null && color.a > 0) {
         ENTRIES.add(new ChatBackgroundRenderer.Entry(x, y, width, height, radius, color.copy(), Math.max(0.0F, Math.min(1.0F, blurMultiplier))));
      }
   }

   public static void renderPending() {
      if (!ENTRIES.isEmpty()) {
         BlurUtil.beginFrame();
         double scale = MeteorClient.mc.getWindow().getGuiScale();

         for (ChatBackgroundRenderer.Entry entry : ENTRIES) {
            double renderX = entry.x * scale;
            double renderY = entry.y * scale;
            double renderWidth = entry.width * scale;
            double renderHeight = entry.height * scale;
            double renderRadius = Math.min(entry.radius, Math.min(entry.width, entry.height) / 2.0) * scale;
            BlurUtil.render(
               (int)Math.round(renderX),
               (int)Math.round(renderY),
               (int)Math.ceil(renderWidth),
               (int)Math.ceil(renderHeight),
               (float)renderRadius,
               BlurSetting.getBlurStrengthDirect() * entry.blurMultiplier
            );
            Render2DUtil.drawRoundedRect(renderX, renderY, renderWidth, renderHeight, Math.max(0.001, renderRadius), entry.color);
         }

         ENTRIES.clear();
      }
   }

   private record Entry(double x, double y, double width, double height, double radius, Color color, float blurMultiplier) {
   }
}
