package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IMultiPlayerGameMode;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class SlotHelper {
   public static final SlotHelper INSTANCE = new SlotHelper();
   private int currentItem;
   private int targetItem;
   private boolean active;
   private boolean persistent;
   private int activeTick;
   private int ticks;
   private SlotHelper.Silence silence = SlotHelper.Silence.DEFAULT;

   private SlotHelper() {
   }

   public SlotHelper setTargetItem(int slot) {
      return this.setTargetItem(slot, false);
   }

   public SlotHelper setTargetItem(int slot, boolean persistent) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null) {
         slot = Mth.clamp(slot, 0, 8);
         if (!this.active) {
            this.currentItem = MeteorClient.mc.player.getInventory().getSelectedSlot();
         }

         this.targetItem = slot;
         this.persistent = persistent;
         this.activeTick = this.ticks;
         this.active = true;
         this.sync(true, true);
         return this;
      } else {
         return this;
      }
   }

   public SlotHelper silence(SlotHelper.Silence silence) {
      this.silence = silence;
      return this;
   }

   public void stop() {
      this.persistent = false;
      this.activeTick = -1;
      this.sync(true, true);
   }

   public void stopNextTick() {
      this.persistent = false;
      this.activeTick = this.ticks;
   }

   public void tick() {
      this.sync(true, true);
      this.ticks++;
   }

   public void sync(boolean reset, boolean check) {
      if (this.active && MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null) {
         if (!check || MeteorClient.mc.screen == null && MeteorClient.mc.getOverlay() == null) {
            if (reset && !this.persistent && this.activeTick != this.ticks) {
               this.setSelectedSlot(this.currentItem);
               this.silence = SlotHelper.Silence.DEFAULT;
               this.active = false;
            } else {
               this.setSelectedSlot(this.targetItem);
            }
         }
      }
   }

   public ItemStack getMainHandStack(ItemStack original) {
      return this.active && this.silence != SlotHelper.Silence.NONE && MeteorClient.mc.player != null
         ? MeteorClient.mc.player.getInventory().getItem(this.currentItem)
         : original;
   }

   public int getSelectedSlot(int original) {
      return this.active && this.silence == SlotHelper.Silence.FULL ? this.currentItem : original;
   }

   public boolean isActive() {
      return this.active;
   }

   public int getVisualSlot() {
      return this.currentItem;
   }

   public static SlotHelper setCurrentItem(int slot) {
      return INSTANCE.setTargetItem(slot);
   }

   public static SlotHelper setCurrentItem(int slot, boolean persistent) {
      return INSTANCE.setTargetItem(slot, persistent);
   }

   private void setSelectedSlot(int slot) {
      MeteorClient.mc.player.getInventory().setSelectedSlot(slot);
      ((IMultiPlayerGameMode)MeteorClient.mc.gameMode).meteor$syncSelected();
   }

   public enum Silence {
      NONE,
      DEFAULT,
      FULL;
   }
}
