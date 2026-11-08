package meteordevelopment.meteorclient.utils.misc.text;

public class RunnableClickEvent extends MeteorClickEvent {
   public final Runnable runnable;

   public RunnableClickEvent(Runnable runnable) {
      super(null);
      this.runnable = runnable;
   }
}
