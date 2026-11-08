package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.util.Mth;

public class WMeteorModule extends WPressable implements MeteorWidget {
   private final Module module;
   private final String title;
   private double titleWidth;
   private double animationProgress;

   public WMeteorModule(Module module, String title) {
      this.module = module;
      this.title = title;
      this.tooltip = module.description;
      this.animationProgress = module.isActive() ? 1.0 : 0.0;
   }

   @Override
   public double pad() {
      return this.theme.scale(4.0);
   }

   @Override
   protected void onCalculateSize() {
      double pad = this.pad();
      if (this.titleWidth == 0.0) {
         this.titleWidth = this.theme.textWidth(this.title);
      }

      this.width = pad + this.titleWidth + pad;
      this.height = (pad + this.theme.textHeight() + pad) * 2.0;
   }

   @Override
   protected void onPressed(int button) {
      if (button == 0) {
         this.module.toggle();
      } else if (button == 1) {
         MeteorClient.mc.setScreen(this.theme.moduleScreen(this.module));
      }
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      MeteorGuiTheme theme = this.theme();
      double pad = this.pad();
      this.animationProgress = this.animationProgress + delta * 6.0 * (this.module.isActive() ? 1 : -1);
      this.animationProgress = Mth.clamp(this.animationProgress, 0.0, 1.0);
      if (this.animationProgress > 0.0) {
         double animWidth = this.width * this.animationProgress;
         renderer.quad(this.x, this.y, animWidth, this.height, theme.accentColor.get());
      }

      double x = this.x + pad;
      double w = this.width - pad * 2.0;
      if (theme.moduleAlignment.get() == AlignmentX.Center) {
         x += w / 2.0 - this.titleWidth / 2.0;
      } else if (theme.moduleAlignment.get() == AlignmentX.Right) {
         x += w - this.titleWidth;
      }

      renderer.text(this.title, x, this.y + this.height / 2.0 - theme.textHeight() / 2.0, theme.textColor.get(), false);
   }
}
