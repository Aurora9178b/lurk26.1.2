package meteordevelopment.meteorclient.systems.modules.render;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import meteordevelopment.meteorclient.events.render.RenderBossBarEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;

public class BossStack extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Boolean> stack = this.sgGeneral
      .add(new BoolSetting.Builder().name("stack").description("Stacks boss bars and adds a counter to the text.").defaultValue(true).build());
   public final Setting<Boolean> hideName = this.sgGeneral
      .add(new BoolSetting.Builder().name("hide-name").description("Hides the names of boss bars.").defaultValue(false).build());
   private final Setting<Double> spacing = this.sgGeneral
      .add(new DoubleSetting.Builder().name("bar-spacing").description("The spacing reduction between each boss bar.").defaultValue(10.0).min(0.0).build());
   public static final Map<LerpingBossEvent, Integer> barMap = new WeakHashMap<>();

   public BossStack() {
      super(Categories.Render, "boss-stack", "Stacks boss bars to make your HUD less cluttered.");
   }

   @EventHandler
   private void onFetchText(RenderBossBarEvent.BossText event) {
      if (this.hideName.get()) {
         event.name = Component.empty();
      } else if (!barMap.isEmpty() && this.stack.get()) {
         LerpingBossEvent bar = event.bossBar;
         Integer integer = barMap.get(bar);
         barMap.remove(bar);
         if (integer != null && !this.hideName.get()) {
            event.name = event.name.copy().append(" x" + integer);
         }
      }
   }

   @EventHandler
   private void onSpaceBars(RenderBossBarEvent.BossSpacing event) {
      event.spacing = this.spacing.get().intValue();
   }

   @EventHandler
   private void onGetBars(RenderBossBarEvent.BossIterator event) {
      if (this.stack.get()) {
         HashMap<String, LerpingBossEvent> chosenBarMap = new HashMap<>();
         event.iterator.forEachRemaining(bar -> {
            String name = bar.getName().getString();
            if (chosenBarMap.containsKey(name)) {
               barMap.compute(chosenBarMap.get(name), (clientBossBar, integer) -> integer == null ? 2 : integer + 1);
            } else {
               chosenBarMap.put(name, bar);
            }
         });
         event.iterator = chosenBarMap.values().iterator();
      }
   }
}
