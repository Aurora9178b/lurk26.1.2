package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.BlockBehaviourAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StatusEffectListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Quiver extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSafety = this.settings.createGroup("Safety");
   private final Setting<List<MobEffect>> effects = this.sgGeneral
      .add(
         new StatusEffectListSetting.Builder()
            .name("effects")
            .description("Which effects to shoot you with.")
            .defaultValue(MobEffects.STRENGTH.value())
            .build()
      );
   private final Setting<Integer> cooldown = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("cooldown")
            .description("How many ticks between shooting effects (19 minimum for NCP).")
            .defaultValue(10)
            .range(0, 40)
            .sliderRange(0, 40)
            .build()
      );
   private final Setting<Boolean> checkEffects = this.sgGeneral
      .add(new BoolSetting.Builder().name("check-effects").description("Won't shoot you with effects you already have.").defaultValue(true).build());
   private final Setting<Boolean> silentBow = this.sgGeneral
      .add(new BoolSetting.Builder().name("silent-bow").description("Takes a bow from your inventory to quiver.").defaultValue(true).build());
   private final Setting<Boolean> chatInfo = this.sgGeneral
      .add(new BoolSetting.Builder().name("chat-info").description("Sends info about quiver checks in chat.").defaultValue(false).build());
   private final Setting<Boolean> onlyInHoles = this.sgSafety
      .add(new BoolSetting.Builder().name("only-in-holes").description("Only quiver when you're in a hole.").defaultValue(true).build());
   private final Setting<Boolean> onlyOnGround = this.sgSafety
      .add(new BoolSetting.Builder().name("only-on-ground").description("Only quiver when you're on the ground.").defaultValue(true).build());
   private final Setting<Double> minHealth = this.sgSafety
      .add(
         new DoubleSetting.Builder()
            .name("min-health")
            .description("How much health you must have to quiver.")
            .defaultValue(10.0)
            .range(0.0, 36.0)
            .sliderRange(0.0, 36.0)
            .build()
      );
   private final List<Integer> arrowSlots = new ArrayList<>();
   private FindItemResult bow;
   private boolean wasMainhand;
   private boolean wasHotbar;
   private int timer;
   private int prevSlot;
   private final MutableBlockPos testPos = new MutableBlockPos();

   public Quiver() {
      super(Categories.Combat, "quiver", "Shoots arrows at yourself.");
   }

   @Override
   public void onActivate() {
      this.bow = InvUtils.find(Items.BOW);
      if (this.shouldQuiver()) {
         this.mc.options.keyUse.setDown(false);
         this.mc.gameMode.releaseUsingItem(this.mc.player);
         this.prevSlot = this.bow.slot();
         this.wasHotbar = this.bow.isHotbar();
         this.timer = 0;
         if (!this.bow.isMainHand()) {
            if (this.wasHotbar) {
               InvUtils.swap(this.bow.slot(), true);
            } else {
               InvUtils.move().from(this.mc.player.getInventory().getSelectedSlot()).to(this.prevSlot);
            }
         } else {
            this.wasMainhand = true;
         }

         this.arrowSlots.clear();
         List<MobEffect> usedEffects = new ArrayList<>();

         for (int i = this.mc.player.getInventory().getContainerSize(); i > 0; i--) {
            if (i != this.mc.player.getInventory().getSelectedSlot()) {
               ItemStack item = this.mc.player.getInventory().getItem(i);
               if (item.getItem() == Items.TIPPED_ARROW) {
                  Iterator<MobEffectInstance> effects = item.getItem().components().get(DataComponents.POTION_CONTENTS).getAllEffects().iterator();
                  if (effects.hasNext()) {
                     MobEffect effect = effects.next().getEffect().value();
                     if (this.effects.get().contains(effect) && !usedEffects.contains(effect) && (!this.hasEffect(effect) || !this.checkEffects.get())) {
                        usedEffects.add(effect);
                        this.arrowSlots.add(i);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDeactivate() {
      if (!this.wasMainhand) {
         if (this.wasHotbar) {
            InvUtils.swapBack();
         } else {
            InvUtils.move().from(this.mc.player.getInventory().getSelectedSlot()).to(this.prevSlot);
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.bow = InvUtils.find(Items.BOW);
      if (this.shouldQuiver()) {
         if (this.arrowSlots.isEmpty()) {
            this.toggle();
         } else if (this.timer > 0) {
            this.timer--;
         } else {
            boolean charging = this.mc.options.keyUse.isDown();
            if (!charging) {
               InvUtils.move().from(this.arrowSlots.getFirst()).to(9);
               this.mc.options.keyUse.setDown(true);
            } else if (BowItem.getPowerForTime(this.mc.player.getTicksUsingItem()) >= 0.12) {
               int targetSlot = this.arrowSlots.getFirst();
               this.arrowSlots.removeFirst();
               this.mc.getConnection().send(new Rot(this.mc.player.getYRot(), -90.0F, this.mc.player.onGround(), this.mc.player.horizontalCollision));
               this.mc.options.keyUse.setDown(false);
               this.mc.gameMode.releaseUsingItem(this.mc.player);
               if (targetSlot != 9) {
                  InvUtils.move().from(9).to(targetSlot);
               }

               this.timer = this.cooldown.get();
            }
         }
      }
   }

   private boolean shouldQuiver() {
      if (!this.bow.found() || !this.bow.isHotbar() && !this.silentBow.get()) {
         if (this.chatInfo.get()) {
            this.error("Couldn't find a usable bow, disabling.");
         }

         this.toggle();
         return false;
      } else if (!this.headIsOpen()) {
         if (this.chatInfo.get()) {
            this.error("Not enough space to quiver, disabling.");
         }

         this.toggle();
         return false;
      } else if (EntityUtils.getTotalHealth(this.mc.player) < this.minHealth.get()) {
         if (this.chatInfo.get()) {
            this.error("Not enough health to quiver, disabling.");
         }

         this.toggle();
         return false;
      } else if (this.onlyOnGround.get() && !this.mc.player.onGround()) {
         if (this.chatInfo.get()) {
            this.error("You are not on the ground, disabling.");
         }

         this.toggle();
         return false;
      } else if (this.onlyInHoles.get() && !this.isSurrounded(this.mc.player)) {
         if (this.chatInfo.get()) {
            this.error("You are not in a hole, disabling.");
         }

         this.toggle();
         return false;
      } else {
         return true;
      }
   }

   private boolean headIsOpen() {
      this.testPos.set(this.mc.player.blockPosition().offset(0, 1, 0));
      BlockState pos1 = this.mc.level.getBlockState(this.testPos);
      if (((BlockBehaviourAccessor)pos1.getBlock()).meteor$isHasCollision()) {
         return false;
      }

      this.testPos.offset(0, 1, 0);
      BlockState pos2 = this.mc.level.getBlockState(this.testPos);
      return !((BlockBehaviourAccessor)pos2.getBlock()).meteor$isHasCollision();
   }

   private boolean hasEffect(MobEffect effect) {
      for (MobEffectInstance statusEffect : this.mc.player.getActiveEffects()) {
         if (statusEffect.getEffect().value().equals(effect)) {
            return true;
         }
      }

      return false;
   }

   private boolean isSurrounded(Player target) {
      for (Direction dir : Direction.values()) {
         if (dir != Direction.UP && dir != Direction.DOWN) {
            this.testPos.set(target.blockPosition()).relative(dir);
            Block block = this.mc.level.getBlockState(this.testPos).getBlock();
            if (block != Blocks.OBSIDIAN
               && block != Blocks.BEDROCK
               && block != Blocks.RESPAWN_ANCHOR
               && block != Blocks.CRYING_OBSIDIAN
               && block != Blocks.NETHERITE_BLOCK) {
               return false;
            }
         }
      }

      return true;
   }
}
