package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public class NoInteract extends Module {
   private final SettingGroup sgBlocks = this.settings.createGroup("Blocks");
   private final SettingGroup sgEntities = this.settings.createGroup("Entities");
   private final Setting<List<Block>> blockMine = this.sgBlocks
      .add(new BlockListSetting.Builder().name("block-mine").description("Cancels block mining.").build());
   private final Setting<NoInteract.ListMode> blockMineMode = this.sgBlocks
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("block-mine-mode"))
                  .description("List mode to use for block mine."))
               .defaultValue(NoInteract.ListMode.BlackList))
            .build()
      );
   private final Setting<List<Block>> blockInteract = this.sgBlocks
      .add(new BlockListSetting.Builder().name("block-interact").description("Cancels block interaction.").build());
   private final Setting<NoInteract.ListMode> blockInteractMode = this.sgBlocks
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("block-interact-mode"))
                  .description("List mode to use for block interact."))
               .defaultValue(NoInteract.ListMode.BlackList))
            .build()
      );
   private final Setting<NoInteract.HandMode> blockInteractHand = this.sgBlocks
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("block-interact-hand"))
                  .description("Cancels block interaction if performed by this hand."))
               .defaultValue(NoInteract.HandMode.None))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entityHit = this.sgEntities
      .add(new EntityTypeListSetting.Builder().name("entity-hit").description("Cancel entity hitting.").onlyAttackable().build());
   private final Setting<NoInteract.ListMode> entityHitMode = this.sgEntities
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("entity-hit-mode"))
                  .description("List mode to use for entity hit."))
               .defaultValue(NoInteract.ListMode.BlackList))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entityInteract = this.sgEntities
      .add(new EntityTypeListSetting.Builder().name("entity-interact").description("Cancel entity interaction.").onlyAttackable().build());
   private final Setting<NoInteract.ListMode> entityInteractMode = this.sgEntities
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("entity-interact-mode"))
                  .description("List mode to use for entity interact."))
               .defaultValue(NoInteract.ListMode.BlackList))
            .build()
      );
   private final Setting<NoInteract.HandMode> entityInteractHand = this.sgEntities
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("entity-interact-hand"))
                  .description("Cancels entity interaction if performed by this hand."))
               .defaultValue(NoInteract.HandMode.None))
            .build()
      );
   private final Setting<NoInteract.InteractMode> friends = this.sgEntities
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("friends")).description("Friends cancel mode."))
               .defaultValue(NoInteract.InteractMode.None))
            .build()
      );
   private final Setting<NoInteract.InteractMode> babies = this.sgEntities
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("babies")).description("Baby entity cancel mode."))
               .defaultValue(NoInteract.InteractMode.None))
            .build()
      );
   private final Setting<NoInteract.InteractMode> nametagged = this.sgEntities
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("nametagged"))
                  .description("Nametagged entity cancel mode."))
               .defaultValue(NoInteract.InteractMode.None))
            .build()
      );

   public NoInteract() {
      super(Categories.Player, "no-interact", "Blocks interactions with certain types of inputs.");
   }

   @EventHandler(priority = 100)
   private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
      if (!this.shouldAttackBlock(event.blockPos)) {
         event.cancel();
      }
   }

   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      if (!this.shouldInteractBlock(event.result, event.hand)) {
         event.cancel();
      }
   }

   @EventHandler(priority = 100)
   private void onAttackEntity(AttackEntityEvent event) {
      if (!this.shouldAttackEntity(event.entity)) {
         event.cancel();
      }
   }

   @EventHandler
   private void onInteractEntity(InteractEntityEvent event) {
      if (!this.shouldInteractEntity(event.entity, event.hand)) {
         event.cancel();
      }
   }

   private boolean shouldAttackBlock(BlockPos blockPos) {
      boolean blockInList = this.blockMine.get().contains(this.mc.level.getBlockState(blockPos).getBlock());
      return this.blockMineMode.get() == NoInteract.ListMode.WhiteList ? blockInList : !blockInList;
   }

   private boolean shouldInteractBlock(BlockHitResult hitResult, InteractionHand hand) {
      if (this.blockInteractHand.get() != NoInteract.HandMode.Both
         && (this.blockInteractHand.get() != NoInteract.HandMode.Mainhand || hand != InteractionHand.MAIN_HAND)
         && (this.blockInteractHand.get() != NoInteract.HandMode.Offhand || hand != InteractionHand.OFF_HAND)) {
         boolean blockInList = this.blockInteract.get().contains(this.mc.level.getBlockState(hitResult.getBlockPos()).getBlock());
         return this.blockInteractMode.get() == NoInteract.ListMode.WhiteList ? blockInList : !blockInList;
      } else {
         return false;
      }
   }

   private boolean shouldAttackEntity(Entity entity) {
      if ((this.friends.get() == NoInteract.InteractMode.Both || this.friends.get() == NoInteract.InteractMode.Hit)
         && entity instanceof Player player
         && !Friends.get().shouldAttack(player)) {
         return false;
      } else if ((this.babies.get() == NoInteract.InteractMode.Both || this.babies.get() == NoInteract.InteractMode.Hit)
         && entity instanceof Animal animal
         && animal.isBaby()) {
         return false;
      } else if ((this.nametagged.get() == NoInteract.InteractMode.Both || this.nametagged.get() == NoInteract.InteractMode.Hit) && entity.hasCustomName()) {
         return false;
      } else {
         return this.entityHitMode.get() == NoInteract.ListMode.BlackList && this.entityHit.get().contains(entity.getType())
            ? false
            : this.entityHitMode.get() != NoInteract.ListMode.WhiteList || this.entityHit.get().contains(entity.getType());
      }
   }

   private boolean shouldInteractEntity(Entity entity, InteractionHand hand) {
      if (this.entityInteractHand.get() != NoInteract.HandMode.Both
         && (this.entityInteractHand.get() != NoInteract.HandMode.Mainhand || hand != InteractionHand.MAIN_HAND)
         && (this.entityInteractHand.get() != NoInteract.HandMode.Offhand || hand != InteractionHand.OFF_HAND)) {
         if ((this.friends.get() == NoInteract.InteractMode.Both || this.friends.get() == NoInteract.InteractMode.Interact)
            && entity instanceof Player player
            && !Friends.get().shouldAttack(player)) {
            return false;
         } else if ((this.babies.get() == NoInteract.InteractMode.Both || this.babies.get() == NoInteract.InteractMode.Interact)
            && entity instanceof Animal animal
            && animal.isBaby()) {
            return false;
         } else if ((this.nametagged.get() == NoInteract.InteractMode.Both || this.nametagged.get() == NoInteract.InteractMode.Interact)
            && entity.hasCustomName()) {
            return false;
         } else {
            return this.entityInteractMode.get() == NoInteract.ListMode.BlackList && this.entityInteract.get().contains(entity.getType())
               ? false
               : this.entityInteractMode.get() != NoInteract.ListMode.WhiteList || this.entityInteract.get().contains(entity.getType());
         }
      } else {
         return false;
      }
   }

   public enum HandMode {
      Mainhand,
      Offhand,
      Both,
      None;
   }

   public enum InteractMode {
      Hit,
      Interact,
      Both,
      None;
   }

   public enum ListMode {
      WhiteList,
      BlackList;
   }
}
