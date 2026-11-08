package meteordevelopment.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.EntityHitResult;

public class AutoRefreshTrade extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> range = this.sgGeneral
      .add(new IntSetting.Builder().name("range").description("Interaction range.").defaultValue(5).min(0).sliderMax(12).build());
   private final Setting<Integer> wallRange = this.sgGeneral
      .add(new IntSetting.Builder().name("wall-range").description("Interaction range through walls.").defaultValue(5).min(0).sliderMax(12).build());
   private final Setting<Integer> waitMine = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("wait-mine-delay")
            .description("Delay after mining the lectern in milliseconds.")
            .defaultValue(5000)
            .min(0)
            .sliderMax(10000)
            .build()
      );
   private final Setting<Set<ResourceKey<Enchantment>>> enchantmentList = this.sgGeneral
      .add(new EnchantmentListSetting.Builder().name("enchantments-list").description("Target enchantments.").build());
   private final Setting<Integer> enchantmentLevel = this.sgGeneral
      .add(new IntSetting.Builder().name("level").description("Target enchantment level.").defaultValue(3).min(0).sliderMax(5).build());
   private final AutoRefreshTrade.SimpleTimer timer = new AutoRefreshTrade.SimpleTimer();
   private BlockPos lecternPos;

   public AutoRefreshTrade() {
      super(Categories.World, "AutoRefreshTrade", "Automatically refreshes villager trades for selected enchanted books.", "auto-refresh-trade");
   }

   @Override
   public void onActivate() {
      this.lecternPos = null;
      this.timer.setMs(999999L);
   }

   @Override
   public void onDeactivate() {
      SlotHelper.INSTANCE.stopNextTick();
      RotationManager.INSTANCE.reset();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate() && this.mc.gameMode != null && this.mc.getConnection() != null) {
         if (this.timer.passedMs(this.waitMine.get().intValue())) {
            if (this.mc.options.keyDown.isDown()) {
               this.toggle();
            } else if (!this.tryPlaceLectern()) {
               Villager target = this.getTarget();
               if (target != null) {
                  RotationManager.INSTANCE.setRotations(RotationUtil.calculate(target.getEyePosition()), 10.0, Priority.High);
                  this.interact(target);
                  this.handleTradeMenu();
               }
            }
         }
      }
   }

   private boolean tryPlaceLectern() {
      if (this.lecternPos != null && this.mc.level.isEmptyBlock(this.lecternPos)) {
         FindItemResult lectern = InvUtils.findInHotbar(Items.LECTERN);
         if (!lectern.found()) {
            return true;
         }

         SlotHelper.setCurrentItem(lectern.slot(), true);
         BlockUtils.place(this.lecternPos, lectern, true, 0, true);
         SlotHelper.INSTANCE.stopNextTick();
         RotationManager.INSTANCE.reset();
         return true;
      } else {
         return false;
      }
   }

   private void interact(Villager villager) {
      EntityHitResult hitResult = new EntityHitResult(villager, villager.getBoundingBox().getCenter());
      this.mc.gameMode.interact(this.mc.player, villager, hitResult, InteractionHand.MAIN_HAND);
   }

   private void handleTradeMenu() {
      if (this.mc.player.containerMenu instanceof MerchantMenu menu) {
         MerchantOffers var15 = menu.getOffers();
         AtomicBoolean foundTarget = new AtomicBoolean(false);
         boolean foundBook = false;

         for (MerchantOffer offer : var15) {
            ItemStack sellStack = offer.getResult();
            if (sellStack.is(Items.ENCHANTED_BOOK)) {
               foundBook = true;
               Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntArrayMap<>();
               Utils.getEnchantments(sellStack, enchantments);

               for (Entry<Holder<Enchantment>> entry : enchantments.object2IntEntrySet()) {
                  int level = entry.getIntValue();
                  int maxLevel = entry.getKey().value().getMaxLevel();
                  this.info(entry.getKey().value().description().copy().append(" " + level));

                  for (ResourceKey<Enchantment> target : this.enchantmentList.get()) {
                     if (entry.getKey().is(target) && (level >= this.enchantmentLevel.get() || level == maxLevel)) {
                        foundTarget.set(true);
                        this.info("Found target enchantment.");
                        break;
                     }
                  }
               }
            }
         }

         if (!foundBook) {
            this.info("No enchanted book found.");
         }

         this.mc.getConnection().send(new ServerboundContainerClosePacket(this.mc.player.containerMenu.containerId));
         if (this.mc.screen != null) {
            this.mc.screen.onClose();
         }

         if (foundTarget.get()) {
            this.toggle();
         } else {
            this.lecternPos = this.getLecternPos();
            RotationManager.INSTANCE.setRotations(RotationUtil.calculate(this.lecternPos), 10.0, Priority.High);
            this.mc.gameMode.startDestroyBlock(this.lecternPos, BlockUtils.getDirection(this.lecternPos));
            this.timer.reset();
         }
      }
   }

   private BlockPos getLecternPos() {
      Direction facing = this.mc.player.getDirection();
      return this.mc.player.blockPosition().relative(facing);
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.lecternPos != null) {
         Color color = new Color(50, 232, 252, 80);
         event.renderer.box(this.lecternPos, color, color, ShapeMode.Both, 0);
      }
   }

   private Villager getTarget() {
      Villager target = null;
      double closestDistance = this.range.get().intValue();

      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof Villager villager) {
            double distance = this.mc.player.distanceTo(villager);
            if ((this.mc.player.hasLineOfSight(villager) || !(distance > this.wallRange.get().intValue())) && !(distance > closestDistance)) {
               target = villager;
               closestDistance = distance;
            }
         }
      }

      return target;
   }

   private static class SimpleTimer {
      private long time = System.nanoTime();

      void reset() {
         this.time = System.nanoTime();
      }

      void setMs(long ms) {
         this.time = System.nanoTime() - ms * 1000000L;
      }

      boolean passedMs(long ms) {
         return System.nanoTime() - this.time >= ms * 1000000L;
      }
   }
}
