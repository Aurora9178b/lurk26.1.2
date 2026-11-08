package meteordevelopment.meteorclient.utils.render;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ReGlassBackgroundQueue {
   private static final List<ReGlassBackgroundQueue.Entry> ENTRIES = new ArrayList<>();
   public static final Color PANEL = new Color(0, 0, 0, 41);
   public static final Color STRONG_PANEL = new Color(0, 0, 0, 76);
   public static final Color HOVER_PANEL = new Color(255, 255, 255, 42);
   public static final Color DISABLED_PANEL = new Color(0, 0, 0, 102);

   private ReGlassBackgroundQueue() {
   }

   public static void add(double x, double y, double width, double height, double radius, Color tint) {
      if (!(width <= 0.0) && !(height <= 0.0) && tint != null && tint.a > 0) {
         ENTRIES.add(new ReGlassBackgroundQueue.Entry(x, y, width, height, radius, tint.copy()));
      }
   }

   public static void addAutoRadius(double x, double y, double width, double height, Color tint) {
      add(x, y, width, height, Math.min(width, height) * 0.5, tint);
   }

   public static boolean isEmpty() {
      return ENTRIES.isEmpty();
   }

   public static void clear() {
      ENTRIES.clear();
   }

   public static void flush(GuiGraphicsExtractor graphics) {
      if (!ENTRIES.isEmpty()) {
         LiquidGlassUtil.beginFrame(graphics);
         Minecraft mc = Minecraft.getInstance();
         double scale = mc.getWindow().getGuiScale();

         for (ReGlassBackgroundQueue.Entry entry : ENTRIES) {
            double radius = entry.radius < 0.0 ? Math.min(entry.width, entry.height) * 0.5 : Math.min(entry.radius, Math.min(entry.width, entry.height) * 0.5);
            if (LiquidGlassSetting.isEnabled()) {
               LiquidGlassUtil.renderGui(graphics, entry.x, entry.y, entry.width, entry.height, radius, entry.tint);
            } else {
               Render2DUtil.drawRoundedRect(entry.x * scale, entry.y * scale, entry.width * scale, entry.height * scale, radius * scale, entry.tint);
            }
         }

         ENTRIES.clear();
      }
   }

   private record Entry(double x, double y, double width, double height, double radius, Color tint) {
   }
}
