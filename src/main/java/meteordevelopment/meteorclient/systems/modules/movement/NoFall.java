package meteordevelopment.meteorclient.systems.modules.movement;

import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class NoFall extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<NoFall.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("The way you are saved from fall damage."))
               .defaultValue(NoFall.Mode.Packet))
            .build()
      );
   private final Setting<NoFall.PlacedItem> placedItem = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("placed-item"))
                     .description("Which block to place."))
                  .defaultValue(NoFall.PlacedItem.Bucket))
               .visible(() -> this.mode.get() == NoFall.Mode.Place))
            .build()
      );
   private final Setting<NoFall.PlaceMode> airPlaceMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("air-place-mode"))
                     .description("Whether place mode places before you die or before you take damage."))
                  .defaultValue(NoFall.PlaceMode.BeforeDeath))
               .visible(() -> this.mode.get() == NoFall.Mode.AirPlace))
            .build()
      );
   private final Setting<Boolean> anchor = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("anchor")
            .description("Centers the player and reduces movement when using bucket or air place mode.")
            .defaultValue(true)
            .visible(() -> this.mode.get() != NoFall.Mode.Packet)
            .build()
      );
   private final Setting<Boolean> antiBounce = this.sgGeneral
      .add(new BoolSetting.Builder().name("anti-bounce").description("Disables bouncing on slime-block and bed upon landing.").defaultValue(true).build());
   private final Setting<Boolean> pauseOnMace = this.sgGeneral
      .add(new BoolSetting.Builder().name("pause-on-mace").description("Pauses NoFall when using a mace.").defaultValue(true).build());
   private boolean placedWater;
   private BlockPos targetPos;
   private int timer;
   private boolean prePathManagerNoFall;

   public NoFall() {
      super(Categories.Movement, "no-fall", "Attempts to prevent you from taking fall damage.");
   }

   @Override
   public void onActivate() {
      this.prePathManagerNoFall = PathManagers.get().getSettings().getNoFall().get();
      if (this.mode.get() == NoFall.Mode.Packet) {
         PathManagers.get().getSettings().getNoFall().set(true);
      }

      this.placedWater = false;
   }

   @Override
   public void onDeactivate() {
      PathManagers.get().getSettings().getNoFall().set(this.prePathManagerNoFall);
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (this.mc.player != null) {
         if (!this.pauseOnMace.get() || !(this.mc.player.getMainHandItem().getItem() instanceof MaceItem)) {
            if (!this.mc.player.getAbilities().instabuild
               && event.packet instanceof ServerboundMovePlayerPacket
               && this.mode.get() == NoFall.Mode.Packet
               && ((IServerboundMovePlayerPacket)event.packet).meteor$getTag() != 1337) {
               if (!Modules.get().isActive(Flight.class)) {
                  if (this.mc.player.isFallFlying()) {
                     return;
                  }

                  if (this.mc.player.getDeltaMovement().y > -0.5) {
                     return;
                  }

                  ((ServerboundMovePlayerPacketAccessor)event.packet).meteor$setOnGround(true);
               } else {
                  ((ServerboundMovePlayerPacketAccessor)event.packet).meteor$setOnGround(true);
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate()) {
         if (this.timer > 20) {
            this.placedWater = false;
            this.timer = 0;
         }

         if (!this.mc.player.getAbilities().instabuild) {
            if (!this.pauseOnMace.get() || !(this.mc.player.getMainHandItem().getItem() instanceof MaceItem)) {
               if (this.mode.get() == NoFall.Mode.AirPlace) {
                  if (!this.airPlaceMode.get().test((float)this.mc.player.fallDistance)) {
                     return;
                  }

                  if (this.anchor.get()) {
                     PlayerUtils.centerPlayer();
                  }

                  Rotations.rotate(
                     this.mc.player.getYRot(),
                     90.0,
                     Integer.MAX_VALUE,
                     () -> {
                        double preY = this.mc.player.getDeltaMovement().y;
                        ((IVec3)this.mc.player.getDeltaMovement()).meteor$setY(0.0);
                        BlockUtils.place(
                           this.mc.player.blockPosition().below(), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem), false, 0, true
                        );
                        ((IVec3)this.mc.player.getDeltaMovement()).meteor$setY(preY);
                     }
                  );
               } else if (this.mode.get() == NoFall.Mode.Place) {
                  NoFall.PlacedItem placedItem1 = this.mc.level.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)
                        && this.placedItem.get() == NoFall.PlacedItem.Bucket
                     ? NoFall.PlacedItem.PowderSnow
                     : this.placedItem.get();
                  if (this.mc.player.fallDistance > 3.0 && !EntityUtils.isAboveWater(this.mc.player)) {
                     Item item = placedItem1.item;
                     FindItemResult findItemResult = InvUtils.findInHotbar(item);
                     if (!findItemResult.found()) {
                        return;
                     }

                     if (this.anchor.get()) {
                        PlayerUtils.centerPlayer();
                     }

                     BlockHitResult result = this.mc
                        .level
                        .clip(
                           new ClipContext(
                              this.mc.player.position(), this.mc.player.position().subtract(0.0, 5.0, 0.0), Block.OUTLINE, Fluid.NONE, this.mc.player
                           )
                        );
                     if (result != null && result.getType() == Type.BLOCK) {
                        this.targetPos = result.getBlockPos().above();
                        if (placedItem1 == NoFall.PlacedItem.Bucket) {
                           this.useItem(findItemResult, true, this.targetPos, true);
                        } else {
                           this.useItem(findItemResult, placedItem1 == NoFall.PlacedItem.PowderSnow, this.targetPos, false);
                        }
                     }
                  }

                  if (this.placedWater) {
                     this.timer++;
                     if (this.mc.player.getInBlockState().getBlock() == placedItem1.block) {
                        this.useItem(InvUtils.findInHotbar(Items.BUCKET), false, this.targetPos, true);
                     } else if (this.mc.level.getBlockState(this.mc.player.blockPosition().below()).getBlock() == Blocks.POWDER_SNOW
                        && this.mc.player.fallDistance == 0.0
                        && placedItem1.block == Blocks.POWDER_SNOW) {
                        this.useItem(InvUtils.findInHotbar(Items.BUCKET), false, this.targetPos.below(), true);
                     }
                  }
               }
            }
         }
      }
   }

   public boolean cancelBounce() {
      return this.isActive() && this.antiBounce.get();
   }

   private void useItem(FindItemResult item, boolean placedWater, BlockPos blockPos, boolean useItem) {
      if (item.found()) {
         if (useItem) {
            Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 10, true, () -> {
               if (item.isOffhand()) {
                  this.mc.gameMode.useItem(this.mc.player, InteractionHand.OFF_HAND);
               } else {
                  InvUtils.swap(item.slot(), true);
                  this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
                  InvUtils.swapBack();
               }
            });
         } else {
            BlockUtils.place(blockPos, item, true, 10, true);
         }

         this.placedWater = placedWater;
      }
   }

   @Override
   public String getInfoString() {
      return this.mode.get().toString();
   }

   public enum Mode {
      Packet,
      AirPlace,
      Place;
   }

   public enum PlaceMode {
      BeforeDamage(height -> height > 2.0F),
      BeforeDeath(height -> height > Math.max(PlayerUtils.getTotalHealth(), 2.0F));

      private final Predicate<Float> fallHeight;

      PlaceMode(Predicate<Float> fallHeight) {
         this.fallHeight = fallHeight;
      }

      public boolean test(float fallheight) {
         return this.fallHeight.test(fallheight);
      }
   }

   public enum PlacedItem {
      Bucket(Items.WATER_BUCKET, Blocks.WATER),
      PowderSnow(Items.POWDER_SNOW_BUCKET, Blocks.POWDER_SNOW),
      HayBale(Items.HAY_BLOCK, Blocks.HAY_BLOCK),
      Cobweb(Items.COBWEB, Blocks.COBWEB),
      SlimeBlock(Items.SLIME_BLOCK, Blocks.SLIME_BLOCK);

      private final Item item;
      private final net.minecraft.world.level.block.Block block;

      PlacedItem(Item item, net.minecraft.world.level.block.Block block) {
         this.item = item;
         this.block = block;
      }
   }
}
