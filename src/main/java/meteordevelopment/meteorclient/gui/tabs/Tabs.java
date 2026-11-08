package meteordevelopment.meteorclient.gui.tabs;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.LurkClickGuiScreen;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.gui.screens.Screen;

public class Tabs {
   private static final List<Tab> tabs = new ArrayList<>();
   private static final Reference2ReferenceOpenHashMap<Class<? extends Tab>, Tab> tabInstances = new Reference2ReferenceOpenHashMap<>();

   private Tabs() {
   }

   @PreInit
   public static void init() {
      add(new Tab("Modules") {
         @Override
         public TabScreen createScreen(GuiTheme theme) {
            return new ModulesScreen(theme);
         }

         @Override
         public boolean isScreen(Screen screen) {
            return screen instanceof ModulesScreen || screen instanceof LurkClickGuiScreen;
         }
      });
   }

   public static void add(Tab tab) {
      tabs.add(tab);
      tabInstances.put((Class<? extends Tab>)tab.getClass(), tab);
   }

   public static List<Tab> get() {
      return tabs;
   }

   public static Tab get(Class<? extends Tab> klass) {
      return tabInstances.get(klass);
   }
}
