package meteordevelopment.meteorclient.utils.render;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.systems.modules.client.ButtonSetting;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

public final class ButtonRenderQueue {
   private static final List<ButtonRenderQueue.Entry> ENTRIES = new ArrayList<>();
   private static final List<ButtonRenderQueue.TextEntry> TEXT_ENTRIES = new ArrayList<>();

   private ButtonRenderQueue() {
   }

   public static void add(
      int x,
      int y,
      int width,
      int height,
      double radius,
      ButtonSetting.BackgroundMode backgroundMode,
      boolean enableGlow,
      Color color,
      Component message,
      boolean active
   ) {
      ENTRIES.add(new ButtonRenderQueue.Entry(x, y, width, height, radius, backgroundMode, enableGlow, color.copy(), message, active));
   }

   public static void addText(String text, int x, int y, float scale, int color, boolean shadow) {
      TEXT_ENTRIES.add(new ButtonRenderQueue.TextEntry(text, x, y, scale, color, shadow));
   }

   public static boolean isEmpty() {
      return ENTRIES.isEmpty() && TEXT_ENTRIES.isEmpty();
   }

   public static void clear() {
      ENTRIES.clear();
      TEXT_ENTRIES.clear();
   }

   public static void flush(GuiGraphicsExtractor graphics) {
      if (!ENTRIES.isEmpty() || !TEXT_ENTRIES.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         double scale = mc.getWindow().getGuiScale();
         if (!ENTRIES.isEmpty()) {
            if (hasBlurEntry()) {
               BlurUtil.beginFrame(graphics);
            }

            if (hasLiquidGlassEntry()) {
               LiquidGlassUtil.beginFrame(graphics);
            }
         }

         for (ButtonRenderQueue.Entry entry : ENTRIES) {
            double radius = Math.min(entry.radius, Math.min(entry.width, entry.height) / 2.0);
            double renderX = entry.x * scale;
            double renderY = entry.y * scale;
            double renderWidth = entry.width * scale;
            double renderHeight = entry.height * scale;
            double renderRadius = radius * scale;
            if (entry.backgroundMode == ButtonSetting.BackgroundMode.LiquidGlass && LiquidGlassSetting.isEnabled()) {
               LiquidGlassUtil.renderGui(graphics, entry.x, entry.y, entry.width, entry.height, radius, entry.color);
            } else {
               BlurUtil.render(
                  (int)Math.round(renderX), (int)Math.round(renderY), (int)Math.ceil(renderWidth), (int)Math.ceil(renderHeight), (float)renderRadius
               );
               Render2DUtil.drawRoundedRect(renderX, renderY, renderWidth, renderHeight, Math.max(0.001, renderRadius), entry.color);
            }

            if (entry.enableGlow) {
               Render2DUtil.drawRoundedGlow(renderX, renderY, renderWidth, renderHeight, renderRadius);
            }
         }

         for (ButtonRenderQueue.Entry entry : ENTRIES) {
            Component message = entry.message;
            if (message != null && !message.getString().isEmpty()) {
               int textColor = entry.active ? -1 : -6250336;
               int textWidth = mc.font.width(message);
               int textX = entry.x + (entry.width - textWidth) / 2;
               int textY = entry.y + (entry.height - 9) / 2;
               graphics.text(mc.font, message, textX, textY, textColor, true);
            }
         }

         for (ButtonRenderQueue.TextEntry entry : TEXT_ENTRIES) {
            if (entry.text != null && !entry.text.isEmpty() && !(entry.scale <= 0.0F)) {
               Matrix3x2fStack matrices = graphics.pose();
               matrices.pushMatrix();
               matrices.translate(entry.x, entry.y);
               matrices.scale(entry.scale, entry.scale);
               graphics.text(mc.font, entry.text, 0, 0, entry.color, entry.shadow);
               matrices.popMatrix();
            }
         }

         ENTRIES.clear();
         TEXT_ENTRIES.clear();
      }
   }

   private static boolean hasBlurEntry() {
      for (ButtonRenderQueue.Entry entry : ENTRIES) {
         if (entry.backgroundMode == ButtonSetting.BackgroundMode.Blur || !LiquidGlassSetting.isEnabled()) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasLiquidGlassEntry() {
      if (!LiquidGlassSetting.isEnabled()) {
         return false;
      }

      for (ButtonRenderQueue.Entry entry : ENTRIES) {
         if (entry.backgroundMode == ButtonSetting.BackgroundMode.LiquidGlass) {
            return true;
         }
      }

      return false;
   }

   private record Entry(
      int x,
      int y,
      int width,
      int height,
      double radius,
      ButtonSetting.BackgroundMode backgroundMode,
      boolean enableGlow,
      Color color,
      Component message,
      boolean active
   ) {
   }

   private record TextEntry(String text, int x, int y, float scale, int color, boolean shadow) {
   }
}
