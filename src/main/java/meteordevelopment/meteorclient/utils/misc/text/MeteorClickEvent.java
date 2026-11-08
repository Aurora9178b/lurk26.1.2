package meteordevelopment.meteorclient.utils.misc.text;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;

public class MeteorClickEvent implements ClickEvent {
   public final String value;

   public MeteorClickEvent(String value) {
      this.value = value;
   }

   @Override
   public Action action() {
      return Action.RUN_COMMAND;
   }
}
