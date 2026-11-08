package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import java.util.Random;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ChestStealer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Item>> items = this.sgGeneral
      .add(new ItemListSetting.Builder().name("items").description("Items used by whitelist or blacklist mode.").build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("delay")
            .description("Delay between taking items in milliseconds.")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 1000)
            .build()
      );
   private final Setting<Boolean> random = this.sgGeneral
      .add(new BoolSetting.Builder().name("random").description("Adds random extra delay based on the delay value.").defaultValue(false).build());
   private final Setting<Boolean> close = this.sgGeneral
      .add(new BoolSetting.Builder().name("close").description("Closes the container when it is empty or your inventory is full.").defaultValue(false).build());
   private final Setting<Boolean> autoMyst = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-myst").description("Automatically opens nearby ender chests after stealing.").defaultValue(false).build());
   private final Setting<Boolean> debug = this.sgGeneral
      .add(new BoolSetting.Builder().name("debug").description("Prints the current container title.").defaultValue(false).build());
   private final Setting<ChestStealer.Sort> sort = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("sort"))
                  .description("How the item list should be used."))
               .defaultValue(ChestStealer.Sort.None))
            .build()
      );
   private final Setting<List<String>> ignoredContainer = this.sgGeneral
      .add(new StringListSetting.Builder().name("ignored-container").description("Container titles to ignore.").defaultValue(List.of("Shop")).build());
   private final Random rnd = new Random();
   private long stealTimer;
   private long autoMystTimer;
   private long debugTimer;

   public ChestStealer() {
      super(Categories.Player, "chest-stealer", "Takes selected items from chests.");
   }

   @Override
   public void onActivate() {
      long now = System.currentTimeMillis();
      this.stealTimer = now;
      this.autoMystTimer = now;
      this.debugTimer = now;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         if (this.debug.get()
            && this.mc.player.containerMenu instanceof ChestMenu
            && this.mc.screen instanceof ContainerScreen screen
            && this.passed(this.debugTimer, 1000L)) {
            this.info(screen.getTitle().getString());
            this.debugTimer = System.currentTimeMillis();
         }

         if (this.autoMyst.get() && this.mc.screen == null && this.passed(this.autoMystTimer, 3000L)) {
            for (BlockEntity blockEntity : Utils.blockEntities()) {
               if (blockEntity instanceof EnderChestBlockEntity) {
                  Vec3 center = Vec3.atCenterOf(blockEntity.getBlockPos());
                  if (!(this.mc.player.distanceToSqr(center) > 39.0)) {
                     Vec3 hitPos = center.add(this.randomOffset(), 0.375, this.randomOffset());
                     this.mc
                        .gameMode
                        .useItemOn(this.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hitPos, Direction.UP, blockEntity.getBlockPos(), false));
                     this.mc.player.swing(InteractionHand.MAIN_HAND);
                     this.autoMystTimer = System.currentTimeMillis();
                     break;
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.player != null && this.mc.gameMode != null && !this.debug.get()) {
         if (this.mc.player.containerMenu instanceof ChestMenu chest && this.mc.screen instanceof ContainerScreen screen) {
            String var11 = screen.getTitle().getString();
            if (!this.containsAny(var11, this.ignoredContainer.get())) {
               int containerSlots = chest.getRowCount() * 9;

               for (int i = 0; i < containerSlots; i++) {
                  Slot slot = chest.getSlot(i);
                  if (slot.hasItem() && this.isAllowed(slot.getItem()) && this.passed(this.stealTimer, this.nextDelay())) {
                     this.mc.gameMode.handleContainerInput(chest.containerId, i, 0, ContainerInput.QUICK_MOVE, this.mc.player);
                     long now = System.currentTimeMillis();
                     this.stealTimer = now;
                     this.autoMystTimer = now;
                     break;
                  }
               }

               if (this.close.get() && (this.isContainerEmpty(chest, containerSlots) || !this.hasEmptySlot())) {
                  this.mc.player.closeContainer();
               }
            }
         }
      }
   }

   private boolean isAllowed(ItemStack stack) {
      boolean selected = this.items.get().contains(stack.getItem());

      return switch ((ChestStealer.Sort)this.sort.get()) {
         case None -> true;
         case WhiteList -> selected;
         case BlackList -> !selected;
      };
   }

   private boolean isContainerEmpty(ChestMenu chest, int containerSlots) {
      for (int i = 0; i < containerSlots; i++) {
         if (chest.getSlot(i).hasItem()) {
            return false;
         }
      }

      return true;
   }

   private boolean hasEmptySlot() {
      for (int i = 0; i < 36; i++) {
         if (this.mc.player.getInventory().getItem(i).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private boolean containsAny(String text, List<String> strings) {
      for (String string : strings) {
         if (!string.isEmpty() && text.contains(string)) {
            return true;
         }
      }

      return false;
   }

   private int nextDelay() {
      int value = this.delay.get();
      return this.random.get() && value > 0 ? value + this.rnd.nextInt(value) : value;
   }

   private double randomOffset() {
      return -0.4 + this.rnd.nextDouble() * 0.8;
   }

   private boolean passed(long timer, long delay) {
      return System.currentTimeMillis() - timer >= delay;
   }

   public enum Sort {
      None,
      WhiteList,
      BlackList;
   }
}
