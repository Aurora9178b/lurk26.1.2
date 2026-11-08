package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ClickGUI extends Module {
   private static ClickGUI INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<ClickGUI.RenderMode> renderMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("render-mode"))
                  .description("The render mode used for ClickGUI backgrounds."))
               .defaultValue(ClickGUI.RenderMode.Blur))
            .build()
      );

   public ClickGUI() {
      super(Categories.Client, "ClickGUI", "ClickGUI render settings.");
      INSTANCE = this;
   }

   public static ClickGUI.RenderMode getRenderMode() {
      return INSTANCE == null ? ClickGUI.RenderMode.Blur : INSTANCE.renderMode.get();
   }

   public static void renderBackground(GuiRenderer renderer, double x, double y, double width, double height, double radius, Color color) {
      if (!(width <= 0.0) && !(height <= 0.0)) {
         if (getRenderMode() == ClickGUI.RenderMode.LiquidGlass && LiquidGlassSetting.isEnabled()) {
            GuiGraphicsExtractor graphics = renderer.graphics();
            if (graphics != null) {
               double scale = Minecraft.getInstance().getWindow().getGuiScale();
               LiquidGlassUtil.renderGui(graphics, x / scale, y / scale, width / scale, height / scale, radius / scale, color);
               return;
            }
         }

         BlurUtil.render((int)x, (int)y, (int)width, (int)height, (float)radius);
         Render2DUtil.drawRoundedRect(renderer, x, y, width, height, radius, color);
      }
   }

   public enum RenderMode {
      Blur,
      LiquidGlass;
   }
}
