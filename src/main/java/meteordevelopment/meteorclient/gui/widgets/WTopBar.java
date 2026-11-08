package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.client.ClickGUI;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.glfw.GLFW;

public abstract class WTopBar extends WHorizontalList {
   protected abstract Color getButtonColor(boolean var1, boolean var2);

   protected abstract Color getNameColor();

   public WTopBar() {
      this.spacing = 12.0;
   }

   @Override
   public void init() {
      for (Tab tab : Tabs.get()) {
         this.add(new WTopBar.WTopBarButton(tab));
      }
   }

   protected class WTopBarButton extends WPressable {
      private final Tab tab;

      public WTopBarButton(Tab tab) {
         this.tab = tab;
      }

      @Override
      protected void onCalculateSize() {
         double pad = this.pad();
         this.width = pad + this.theme.textWidth(this.tab.name) + pad;
         this.height = pad + this.theme.textHeight() + pad;
      }

      @Override
      protected void onPressed(int button) {
         if (!(MeteorClient.mc.screen instanceof TabScreen tabScreen && tabScreen.tab == this.tab)) {
            double mouseX = MeteorClient.mc.mouseHandler.xpos();
            double mouseY = MeteorClient.mc.mouseHandler.ypos();
            this.tab.openScreen(this.theme);
            GLFW.glfwSetCursorPos(MeteorClient.mc.getWindow().handle(), mouseX, mouseY);
         }
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         double pad = this.pad();
         double topPadding = this.theme.scale(12.0);
         double renderY = this.y + topPadding;
         Color color = WTopBar.this.getButtonColor(
            this.pressed || MeteorClient.mc.screen instanceof TabScreen tabScreen && tabScreen.tab == this.tab, this.mouseOver
         );
         ClickGUI.renderBackground(renderer, this.x, renderY, this.width, this.height, 4.0, color);
         renderer.text(this.tab.name, this.x + pad, renderY + pad, WTopBar.this.getNameColor(), false);
      }
   }
}
