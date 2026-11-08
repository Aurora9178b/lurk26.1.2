package meteordevelopment.meteorclient.utils.leaves;

public class LeavesTimer {
   private long time = System.nanoTime();

   public void reset() {
      this.time = System.nanoTime();
   }

   public void setMs(long ms) {
      this.time = System.nanoTime() - ms * 1000000L;
   }

   public boolean passedMs(long ms) {
      return System.nanoTime() - this.time >= ms * 1000000L;
   }
}
