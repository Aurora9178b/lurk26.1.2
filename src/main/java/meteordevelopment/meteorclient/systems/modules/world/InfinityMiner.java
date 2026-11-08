package meteordevelopment.meteorclient.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IMineProcess;
import java.util.List;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;

public class InfinityMiner extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgWhenFull = this.settings.createGroup("When Full");
   public final Setting<List<Block>> targetBlocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("target-blocks")
            .description("The target blocks to mine.")
            .defaultValue(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE)
            .filter(this::filterBlocks)
            .build()
      );
   public final Setting<List<Item>> targetItems = this.sgGeneral
      .add(new ItemListSetting.Builder().name("target-items").description("The target items to collect.").defaultValue(Items.DIAMOND).build());
   public final Setting<List<Block>> repairBlocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("repair-blocks")
            .description("The repair blocks to mine.")
            .defaultValue(Blocks.COAL_ORE, Blocks.REDSTONE_ORE, Blocks.NETHER_QUARTZ_ORE)
            .filter(this::filterBlocks)
            .build()
      );
   public final Setting<Double> startRepairing = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("repair-threshold")
            .description("The durability percentage at which to start repairing.")
            .defaultValue(20.0)
            .range(1.0, 99.0)
            .sliderRange(1.0, 99.0)
            .build()
      );
   public final Setting<Double> startMining = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("mine-threshold")
            .description("The durability percentage at which to start mining.")
            .defaultValue(70.0)
            .range(1.0, 99.0)
            .sliderRange(1.0, 99.0)
            .build()
      );
   public final Setting<Boolean> walkHome = this.sgWhenFull
      .add(new BoolSetting.Builder().name("walk-home").description("Will walk 'home' when your inventory is full.").defaultValue(false).build());
   public final Setting<Boolean> logOut = this.sgWhenFull
      .add(
         new BoolSetting.Builder()
            .name("log-out")
            .description("Logs out when your inventory is full. Will walk home FIRST if walk home is enabled.")
            .defaultValue(false)
            .build()
      );
   private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
   private final Settings baritoneSettings = BaritoneAPI.getSettings();
   private final MutableBlockPos homePos = new MutableBlockPos();
   private boolean prevMineScanDroppedItems;
   private boolean repairing;

   public InfinityMiner() {
      super(
         Categories.World,
         "infinity-miner",
         "Allows you to essentially mine forever by mining repair blocks when the durability gets low. Needs a mending pickaxe."
      );
   }

   @Override
   public void onActivate() {
      this.prevMineScanDroppedItems = (Boolean)this.baritoneSettings.mineScanDroppedItems.value;
      this.baritoneSettings.mineScanDroppedItems.value = true;
      this.homePos.set(this.mc.player.blockPosition());
      this.repairing = false;
   }

   @Override
   public void onDeactivate() {
      this.baritone.getPathingBehavior().cancelEverything();
      this.baritoneSettings.mineScanDroppedItems.value = this.prevMineScanDroppedItems;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.isFull()) {
         if (this.walkHome.get()) {
            if (this.isBaritoneNotWalking()) {
               this.info("Walking home.");
               this.baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.homePos.x, this.homePos.y, this.homePos.z));
            } else if (this.mc.player.blockPosition().equals(this.homePos) && this.logOut.get()) {
               this.logOut();
            }
         } else if (this.logOut.get()) {
            this.logOut();
         } else {
            this.info("Inventory full, stopping process.");
            this.toggle();
         }
      } else if (!this.findPickaxe()) {
         this.error("Could not find a usable mending pickaxe.");
         this.toggle();
      } else if (!this.checkThresholds()) {
         this.error("Start mining value can't be lower than start repairing value.");
         this.toggle();
      } else {
         if (this.repairing) {
            if (!this.needsRepair()) {
               this.warning("Finished repairing, going back to mining.");
               this.repairing = false;
               this.baritoneSettings.mineScanDroppedItems.value = true;
               this.mineTargetBlocks();
               return;
            }

            if (this.isBaritoneNotMining()) {
               this.mineRepairBlocks();
            }
         } else {
            if (this.needsRepair()) {
               this.warning("Pickaxe needs repair, beginning repair process");
               this.repairing = true;
               this.baritoneSettings.mineScanDroppedItems.value = false;
               this.mineRepairBlocks();
               return;
            }

            if (this.isBaritoneNotMining()) {
               this.mineTargetBlocks();
            }
         }
      }
   }

   private boolean needsRepair() {
      ItemStack itemStack = this.mc.player.getMainHandItem();
      double toolPercentage = (itemStack.getMaxDamage() - itemStack.getDamageValue()) * 100.0F / itemStack.getMaxDamage();
      return !(toolPercentage > this.startMining.get()) && (!(toolPercentage > this.startRepairing.get()) || this.repairing);
   }

   private boolean findPickaxe() {
      Predicate<ItemStack> pickaxePredicate = stack -> stack.is(ItemTags.PICKAXES)
         && Utils.hasEnchantment(stack, Enchantments.MENDING)
         && !Utils.hasEnchantment(stack, Enchantments.SILK_TOUCH);
      FindItemResult bestPick = InvUtils.findInHotbar(pickaxePredicate);
      if (bestPick.isOffhand()) {
         InvUtils.shiftClick().fromOffhand().toHotbar(this.mc.player.getInventory().getSelectedSlot());
      } else if (bestPick.isHotbar()) {
         InvUtils.swap(bestPick.slot(), false);
      }

      return InvUtils.testInMainHand(pickaxePredicate);
   }

   private boolean checkThresholds() {
      return this.startRepairing.get() < this.startMining.get();
   }

   private void mineTargetBlocks() {
      this.baritone.getPathingBehavior().cancelEverything();
      this.baritone.getMineProcess().mineByName(0, this.targetBlocks.get().stream().map(Object::toString).toArray(String[]::new));
   }

   private void mineRepairBlocks() {
      this.baritone.getPathingBehavior().cancelEverything();
      this.baritone.getMineProcess().mineByName(0, this.repairBlocks.get().stream().map(Object::toString).toArray(String[]::new));
   }

   private void logOut() {
      this.toggle();
      this.mc.player.connection.send(new ClientboundDisconnectPacket(Component.literal("[Infinity Miner] Inventory is full.")));
   }

   private boolean isBaritoneNotMining() {
      return !(this.baritone.getPathingControlManager().mostRecentInControl().orElse(null) instanceof IMineProcess);
   }

   private boolean isBaritoneNotWalking() {
      return !(this.baritone.getPathingControlManager().mostRecentInControl().orElse(null) instanceof ICustomGoalProcess);
   }

   private boolean filterBlocks(Block block) {
      return block != Blocks.AIR && block.defaultBlockState().getDestroySpeed(this.mc.level, null) != -1.0F && !(block instanceof LiquidBlock);
   }

   private boolean isFull() {
      for (int i = 0; i <= 35; i++) {
         ItemStack itemStack = this.mc.player.getInventory().getItem(i);
         if (itemStack.isEmpty()) {
            return false;
         }

         for (Item item : this.targetItems.get()) {
            if (itemStack.getItem() == item && itemStack.getCount() < itemStack.getMaxStackSize()) {
               return false;
            }
         }
      }

      return true;
   }
}
