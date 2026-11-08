package meteordevelopment.meteorclient.utils.leaves;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class LeavesInventoryUtil {
   private static int serverSlot = -1;

   private LeavesInventoryUtil() {
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundSetCarriedItemPacket packet) {
         serverSlot = packet.getSlot();
      }
   }

   public static int findItem(Item item) {
      if (MeteorClient.mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         if (MeteorClient.mc.player.getInventory().getItem(i).is(item)) {
            return i;
         }
      }

      return -1;
   }

   public static int findItemInventorySlot(Item item) {
      if (MeteorClient.mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 36; i++) {
         if (MeteorClient.mc.player.getInventory().getItem(i).is(item)) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   public static int findClass(Class<?> clazz) {
      if (MeteorClient.mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = MeteorClient.mc.player.getInventory().getItem(i);
         if (!stack.isEmpty()) {
            if (clazz.isInstance(stack.getItem())) {
               return i;
            }

            if (stack.getItem() instanceof BlockItem blockItem && clazz.isInstance(blockItem.getBlock())) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findClassInventory(Class<?> clazz) {
      if (MeteorClient.mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 36; i++) {
         ItemStack stack = MeteorClient.mc.player.getInventory().getItem(i);
         if (!stack.isEmpty()) {
            if (clazz.isInstance(stack.getItem())) {
               return i < 9 ? i + 36 : i;
            }

            if (stack.getItem() instanceof BlockItem blockItem && clazz.isInstance(blockItem.getBlock())) {
               return i < 9 ? i + 36 : i;
            }
         }
      }

      return -1;
   }

   public static void switchToSlot(int slot) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null && slot >= 0 && slot <= 8) {
         if (slot != serverSlot) {
            serverSlot = slot;
            MeteorClient.mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
         }
      }
   }

   public static void inventorySwap(int slot, int selectedSlot) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null) {
         if (slot >= 0) {
            if (slot >= 36 && slot <= 44) {
               switchToSlot(slot - 36);
            } else {
               int slotId = slot < 9 ? 36 + slot : SlotUtils.indexToId(slot);
               if (slotId != -1) {
                  MeteorClient.mc
                     .gameMode
                     .handleContainerInput(MeteorClient.mc.player.containerMenu.containerId, slotId, selectedSlot, ContainerInput.SWAP, MeteorClient.mc.player);
               }
            }
         }
      }
   }

   public static ItemStack getStackInSlot(int slot) {
      return MeteorClient.mc.player != null && slot >= 0 && slot < MeteorClient.mc.player.getInventory().getContainerSize()
         ? MeteorClient.mc.player.getInventory().getItem(slot)
         : ItemStack.EMPTY;
   }

   public static int findBlock(Block block) {
      if (MeteorClient.mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = MeteorClient.mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) {
            return i;
         }
      }

      return -1;
   }

   static {
      MeteorClient.EVENT_BUS.subscribe(new LeavesInventoryUtil());
   }
}
