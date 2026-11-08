package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.packets.InventoryEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractContainerScreenAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ScreenHandlerListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.InventorySorter;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DecoratedPotBlock;

public class InventoryTweaks extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSorting = this.settings.createGroup("Sorting");
   private final SettingGroup sgAntiDrop = this.settings.createGroup("Anti Drop");
   private final SettingGroup sgAutoDrop = this.settings.createGroup("Auto Drop");
   private final SettingGroup sgStealDump = this.settings.createGroup("Steal and Dump");
   private final SettingGroup sgAutoSteal = this.settings.createGroup("Auto Steal");
   private final Setting<Boolean> mouseDragItemMove = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("mouse-drag-item-move")
            .description("Moving mouse over items while holding shift will transfer it to the other container.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> xCarry = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("xcarry")
            .description("Allows you to store four extra item stacks in your crafting grid.")
            .defaultValue(true)
            .onChanged(v -> {
               if (!v && Utils.canUpdate()) {
                  this.mc.player.connection.send(new ServerboundContainerClosePacket(this.mc.player.inventoryMenu.containerId));
                  this.invOpened = false;
               }
            })
            .build()
      );
   private final Setting<Boolean> uncapBundleScrolling = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("uncap-bundle-scrolling")
            .description("Whether to uncap the bundle scrolling feature to let you select any item.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> frameInput = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("frame-input-handling")
            .description(
               "Changes input handling to work every frame instead of every tick. A very minor effect but may\nmake inputs feel smoother, especially in laggy environments. Will flag anticheats that check packet order (Grim)."
            )
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> sortingEnabled = this.sgSorting
      .add(new BoolSetting.Builder().name("sorting-enabled").description("Automatically sorts stacks in inventory.").defaultValue(true).build());
   private final Setting<Keybind> sortingKey = this.sgSorting
      .add(
         new KeybindSetting.Builder()
            .name("sorting-key")
            .description("Key to trigger the sort.")
            .visible(this.sortingEnabled::get)
            .defaultValue(Keybind.fromButton(2))
            .build()
      );
   private final Setting<Integer> sortingDelay = this.sgSorting
      .add(
         new IntSetting.Builder()
            .name("sorting-delay")
            .description("Delay in ticks between moving items when sorting.")
            .visible(this.sortingEnabled::get)
            .defaultValue(1)
            .min(0)
            .build()
      );
   private final Setting<Boolean> disableInCreative = this.sgSorting
      .add(
         new BoolSetting.Builder()
            .name("disable-in-creative")
            .description("Disables the inventory sorter when in creative mode.")
            .defaultValue(true)
            .visible(this.sortingEnabled::get)
            .build()
      );
   private final Setting<List<Item>> antiDropItems = this.sgAntiDrop
      .add(new ItemListSetting.Builder().name("anti-drop-items").description("Items to prevent dropping. Doesn't work in creative inventory screen.").build());
   private final Setting<Boolean> antiItemFrame = this.sgAntiDrop
      .add(
         new BoolSetting.Builder()
            .name("item-frames")
            .description("Prevent anti-drop items from being placed in item frames or pots")
            .defaultValue(true)
            .build()
      );
   private final Setting<Keybind> antiDropOverrideBind = this.sgAntiDrop
      .add(new KeybindSetting.Builder().name("override-bind").description("Hold this bind to temporarily bypass anti-drop").build());
   private final Setting<List<Item>> autoDropItems = this.sgAutoDrop
      .add(new ItemListSetting.Builder().name("auto-drop-items").description("Items to drop.").build());
   private final Setting<Boolean> autoDropExcludeEquipped = this.sgAutoDrop
      .add(new BoolSetting.Builder().name("exclude-equipped").description("Whether or not to drop items equipped in armor slots.").defaultValue(true).build());
   private final Setting<Boolean> autoDropExcludeHotbar = this.sgAutoDrop
      .add(new BoolSetting.Builder().name("exclude-hotbar").description("Whether or not to drop items from your hotbar.").defaultValue(false).build());
   private final Setting<Boolean> autoDropOnlyFullStacks = this.sgAutoDrop
      .add(new BoolSetting.Builder().name("only-full-stacks").description("Only drops the items if the stack is full.").defaultValue(false).build());
   public final Setting<List<MenuType<?>>> stealScreens = this.sgStealDump
      .add(
         new ScreenHandlerListSetting.Builder()
            .name("steal-screens")
            .description("Select the screens to display buttons and auto steal.")
            .defaultValue(List.of(MenuType.GENERIC_9x3, MenuType.GENERIC_9x6))
            .build()
      );
   private final Setting<Boolean> buttons = this.sgStealDump
      .add(new BoolSetting.Builder().name("inventory-buttons").description("Shows steal and dump buttons in container guis.").defaultValue(true).build());
   private final Setting<Boolean> stealDrop = this.sgStealDump
      .add(new BoolSetting.Builder().name("steal-drop").description("Drop items to the ground instead of stealing them.").defaultValue(false).build());
   private final Setting<Boolean> dropBackwards = this.sgStealDump
      .add(new BoolSetting.Builder().name("drop-backwards").description("Drop items behind you.").defaultValue(false).visible(this.stealDrop::get).build());
   private final Setting<InventoryTweaks.ListMode> dumpFilter = this.sgStealDump
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("dump-filter")).description("Dump mode."))
               .defaultValue(InventoryTweaks.ListMode.None))
            .build()
      );
   private final Setting<List<Item>> dumpItems = this.sgStealDump.add(new ItemListSetting.Builder().name("dump-items").description("Items to dump.").build());
   private final Setting<InventoryTweaks.ListMode> stealFilter = this.sgStealDump
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("steal-filter")).description("Steal mode."))
               .defaultValue(InventoryTweaks.ListMode.None))
            .build()
      );
   private final Setting<List<Item>> stealItems = this.sgStealDump
      .add(new ItemListSetting.Builder().name("steal-items").description("Items to steal.").build());
   private final Setting<Boolean> autoSteal = this.sgAutoSteal
      .add(
         new BoolSetting.Builder()
            .name("auto-steal")
            .description("Automatically removes all possible items when you open a container.")
            .defaultValue(false)
            .onChanged(var1 -> this.checkAutoStealSettings())
            .build()
      );
   private final Setting<Boolean> autoDump = this.sgAutoSteal
      .add(
         new BoolSetting.Builder()
            .name("auto-dump")
            .description("Automatically dumps all possible items when you open a container.")
            .defaultValue(false)
            .onChanged(var1 -> this.checkAutoStealSettings())
            .build()
      );
   private final Setting<Integer> autoStealDelay = this.sgAutoSteal
      .add(
         new IntSetting.Builder()
            .name("delay")
            .description("The minimum delay between stealing the next stack in milliseconds.")
            .defaultValue(20)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Integer> autoStealInitDelay = this.sgAutoSteal
      .add(
         new IntSetting.Builder()
            .name("initial-delay")
            .description("The initial delay before stealing in milliseconds. 0 to use normal delay instead.")
            .defaultValue(50)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Integer> autoStealRandomDelay = this.sgAutoSteal
      .add(
         new IntSetting.Builder()
            .name("random")
            .description("Randomly adds a delay of up to the specified time in milliseconds.")
            .min(0)
            .sliderMax(1000)
            .defaultValue(50)
            .build()
      );
   private InventorySorter sorter;
   private boolean invOpened;

   public InventoryTweaks() {
      super(Categories.Misc, "inventory-tweaks", "Various inventory related utilities.");
   }

   @Override
   public void onActivate() {
      this.invOpened = false;
   }

   @Override
   public void onDeactivate() {
      this.sorter = null;
      if (this.invOpened) {
         this.mc.player.connection.send(new ServerboundContainerClosePacket(this.mc.player.inventoryMenu.containerId));
      }
   }

   @EventHandler
   private void onKey(KeyInputEvent event) {
      if (event.action == KeyAction.Press) {
         if (this.sortingKey.get().matches(event.input) && this.sort()) {
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      if (event.action == KeyAction.Press) {
         if (this.sortingKey.get().matches(event.input) && this.sort()) {
            event.cancel();
         }
      }
   }

   private boolean sort() {
      if (this.sortingEnabled.get()
         && this.mc.screen instanceof AbstractContainerScreen<?> screen
         && this.sorter == null
         && (!this.mc.player.isCreative() || !this.disableInCreative.get())) {
         if (!this.mc.player.containerMenu.getCarried().isEmpty()) {
            FindItemResult empty = InvUtils.findEmpty();
            if (!empty.found()) {
               InvUtils.click().slot(-999);
            } else {
               InvUtils.click().slot(empty.slot());
            }
         }

         Slot focusedSlot = ((AbstractContainerScreenAccessor)screen).meteor$getHoveredSlot();
         if (focusedSlot == null) {
            return false;
         }

         this.sorter = new InventorySorter(screen, focusedSlot);
         return true;
      } else {
         return false;
      }
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      this.sorter = null;
   }

   @EventHandler
   private void onTickPre(TickEvent.Pre event) {
      if (this.sorter != null && this.sorter.tick(this.sortingDelay.get())) {
         this.sorter = null;
      }
   }

   @EventHandler
   private void onTickPost(TickEvent.Post event) {
      if (Utils.canUpdate() && !(this.mc.screen instanceof AbstractContainerScreen) && !this.autoDropItems.get().isEmpty()) {
         for (int i = this.autoDropExcludeHotbar.get() ? 9 : 0; i < this.mc.player.getInventory().getContainerSize(); i++) {
            ItemStack itemStack = this.mc.player.getInventory().getItem(i);
            if (this.autoDropItems.get().contains(itemStack.getItem())
               && (!this.autoDropOnlyFullStacks.get() || itemStack.getCount() == itemStack.getMaxStackSize())
               && (!this.autoDropExcludeEquipped.get() || !SlotUtils.isArmor(i))) {
               InvUtils.drop().slot(i);
            }
         }
      }
   }

   @EventHandler
   private void onDropItems(DropItemsEvent event) {
      if (!this.antiDropOverrideBind.get().isPressed()) {
         if (this.antiDropItems.get().contains(event.itemStack.getItem())) {
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onInteractEntity(InteractEntityEvent event) {
      if (this.antiItemFrame.get() && !this.antiDropOverrideBind.get().isPressed()) {
         if (event.entity instanceof ItemFrame) {
            Item item = this.mc.player.getItemInHand(event.hand).getItem();
            if (this.antiDropItems.get().contains(item)) {
               event.cancel();
            }
         }
      }
   }

   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      if (this.antiItemFrame.get() && !this.antiDropOverrideBind.get().isPressed()) {
         if (event.hand == InteractionHand.MAIN_HAND) {
            Block block = this.mc.level.getBlockState(event.result.getBlockPos()).getBlock();
            if (block instanceof DecoratedPotBlock) {
               Item item = this.mc.player.getItemInHand(event.hand).getItem();
               if (this.antiDropItems.get().contains(item)) {
                  event.cancel();
               }
            }
         }
      }
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (this.xCarry.get() && event.packet instanceof ServerboundContainerClosePacket packet) {
         if (packet.getContainerId() == this.mc.player.inventoryMenu.containerId) {
            this.invOpened = true;
            event.cancel();
         }
      }
   }

   private void checkAutoStealSettings() {
      if (this.autoSteal.get() && this.autoDump.get()) {
         this.error("You can't enable Auto Steal and Auto Dump at the same time!");
         this.autoDump.set(false);
      }
   }

   private int getSleepTime() {
      return this.autoStealDelay.get() + (this.autoStealRandomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, this.autoStealRandomDelay.get()) : 0);
   }

   private void moveSlots(AbstractContainerMenu handler, int start, int end, boolean steal) {
      boolean initial = this.autoStealInitDelay.get() != 0;

      for (int i = start; i < end; i++) {
         if (handler.getSlot(i).hasItem()) {
            if (this.mc.screen == null || !Utils.canUpdate()) {
               break;
            }

            Item item = handler.getSlot(i).getItem().getItem();
            if (steal
               ? (this.stealFilter.get() != InventoryTweaks.ListMode.Whitelist || this.stealItems.get().contains(item))
                  && (this.stealFilter.get() != InventoryTweaks.ListMode.Blacklist || !this.stealItems.get().contains(item))
               : (this.dumpFilter.get() != InventoryTweaks.ListMode.Whitelist || this.dumpItems.get().contains(item))
                  && (this.dumpFilter.get() != InventoryTweaks.ListMode.Blacklist || !this.dumpItems.get().contains(item))) {
               int sleep;
               if (initial) {
                  sleep = this.autoStealInitDelay.get();
                  initial = false;
               } else {
                  sleep = this.getSleepTime();
               }

               if (sleep > 0) {
                  try {
                     Thread.sleep(sleep);
                  } catch (InterruptedException e) {
                     MeteorClient.LOG.error("Error when sleeping the slot mover", e);
                  }
               }

               if (this.mc.screen == null || !Utils.canUpdate()) {
                  break;
               }

               if (!steal || !this.stealDrop.get()) {
                  InvUtils.shiftClick().slotId(i);
               } else if (this.dropBackwards.get()) {
                  int iCopy = i;
                  Rotations.rotate(this.mc.player.getYRot() - 180.0F, this.mc.player.getXRot(), () -> InvUtils.drop().slotId(iCopy));
               }
            }
         }
      }
   }

   public void steal(AbstractContainerMenu handler) {
      MeteorExecutor.execute(() -> this.moveSlots(handler, 0, SlotUtils.indexToId(9), true));
   }

   public void dump(AbstractContainerMenu handler) {
      int playerInvOffset = SlotUtils.indexToId(9);
      MeteorExecutor.execute(() -> this.moveSlots(handler, playerInvOffset, playerInvOffset + 36, false));
   }

   public boolean showButtons() {
      return this.isActive() && this.buttons.get();
   }

   public boolean mouseDragItemMove() {
      return this.isActive() && this.mouseDragItemMove.get();
   }

   public boolean uncapBundleScrolling() {
      return this.isActive() && this.uncapBundleScrolling.get();
   }

   public boolean frameInput() {
      return this.isActive() && this.frameInput.get();
   }

   public boolean canSteal(AbstractContainerMenu handler) {
      try {
         return this.stealScreens.get().contains(handler.getType());
      } catch (UnsupportedOperationException var3) {
         return false;
      }
   }

   @EventHandler
   private void onInventory(InventoryEvent event) {
      AbstractContainerMenu handler = this.mc.player.containerMenu;
      if (this.canSteal(handler) && event.packet.containerId() == handler.containerId) {
         if (this.autoSteal.get()) {
            this.steal(handler);
         } else if (this.autoDump.get()) {
            this.dump(handler);
         }
      }
   }

   public enum ListMode {
      Whitelist,
      Blacklist,
      None;
   }
}
