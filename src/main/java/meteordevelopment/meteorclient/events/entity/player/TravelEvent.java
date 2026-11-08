package meteordevelopment.meteorclient.events.entity.player;

import meteordevelopment.meteorclient.events.Cancellable;

public class TravelEvent extends Cancellable {
   private static final TravelEvent INSTANCE = new TravelEvent();

   public static TravelEvent get() {
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
