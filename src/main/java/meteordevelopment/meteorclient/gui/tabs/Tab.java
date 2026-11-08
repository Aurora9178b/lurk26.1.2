package meteordevelopment.meteorclient.gui.tabs;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import net.minecraft.client.gui.screens.Screen;

public abstract class Tab {
   public final String name;

   public Tab(String name) {
      this.name = name;
   }

   public void openScreen(GuiTheme theme) {
      TabScreen screen = this.createScreen(theme);
      screen.addDirect(theme.topBar()).top().padTop(theme.scale(8.0)).centerX();
      MeteorClient.mc.setScreen(screen);
   }

   public abstract TabScreen createScreen(GuiTheme var1);

   public abstract boolean isScreen(Screen var1);
}
