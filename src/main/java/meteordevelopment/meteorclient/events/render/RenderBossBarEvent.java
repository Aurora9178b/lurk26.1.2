package meteordevelopment.meteorclient.events.render;

import java.util.Iterator;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;

public class RenderBossBarEvent {
   public static class BossIterator {
      private static final RenderBossBarEvent.BossIterator INSTANCE = new RenderBossBarEvent.BossIterator();
      public Iterator<LerpingBossEvent> iterator;

      public static RenderBossBarEvent.BossIterator get(Iterator<LerpingBossEvent> iterator) {
         INSTANCE.iterator = iterator;
         return INSTANCE;
      }
   }

   public static class BossSpacing {
      private static final RenderBossBarEvent.BossSpacing INSTANCE = new RenderBossBarEvent.BossSpacing();
      public int spacing;

      public static RenderBossBarEvent.BossSpacing get(int spacing) {
         INSTANCE.spacing = spacing;
         return INSTANCE;
      }
   }

   public static class BossText {
      private static final RenderBossBarEvent.BossText INSTANCE = new RenderBossBarEvent.BossText();
      public LerpingBossEvent bossBar;
      public Component name;

      public static RenderBossBarEvent.BossText get(LerpingBossEvent bossBar, Component name) {
         INSTANCE.bossBar = bossBar;
         INSTANCE.name = name;
         return INSTANCE;
      }
   }
}
