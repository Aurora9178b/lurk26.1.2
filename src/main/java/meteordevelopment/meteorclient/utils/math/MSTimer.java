package meteordevelopment.meteorclient.utils.math;

public class MSTimer {
   private long lastTime = System.currentTimeMillis();

   public final boolean hasPassTime(long time) {
      return this.getPassTime() >= time;
   }

   public final boolean hasPassTime(int time) {
      return this.getPassTime() >= time;
   }

   public final void reset() {
      this.lastTime = System.currentTimeMillis();
   }

   public final void reset(long offset) {
      this.lastTime = System.currentTimeMillis() - offset;
   }

   public final long getPassTime() {
      return System.currentTimeMillis() - this.lastTime;
   }
}
