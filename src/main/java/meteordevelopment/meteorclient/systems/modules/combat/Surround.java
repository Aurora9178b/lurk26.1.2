package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.DirectionAccessor;
import meteordevelopment.meteorclient.mixin.LevelRendererAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class Surround extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgToggles = this.settings.createGroup("Toggles");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("blocks")
            .description("What blocks to use for surround.")
            .defaultValue(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK)
            .filter(this::blockFilter)
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("Delay, in ticks, between block placements.").min(0).defaultValue(0).build());
   private final Setting<Integer> blocksPerTick = this.sgGeneral
      .add(new IntSetting.Builder().name("blocks-per-tick").description("How many blocks to place in one tick.").defaultValue(1).min(1).build());
   private final Setting<Surround.Center> center = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("center"))
                  .description("Teleports you to the center of the block."))
               .defaultValue(Surround.Center.Incomplete))
            .build()
      );
   private final Setting<Boolean> doubleHeight = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("double-height")
            .description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> onlyOnGround = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-on-ground").description("Works only when you are standing on blocks.").defaultValue(true).build());
   private final Setting<Boolean> airPlace = this.sgGeneral
      .add(new BoolSetting.Builder().name("air-place").description("Allows Surround to place blocks in the air.").defaultValue(true).build());
   private final Setting<Boolean> toggleModules = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-modules").description("Turn off other modules when surround is activated.").defaultValue(false).build());
   private final Setting<Boolean> toggleBack = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("toggle-back-on")
            .description("Turn the other modules back on when surround is deactivated.")
            .defaultValue(false)
            .visible(this.toggleModules::get)
            .build()
      );
   private final Setting<List<Module>> modules = this.sgGeneral
      .add(new ModuleListSetting.Builder().name("modules").description("Which modules to disable on activation.").visible(this.toggleModules::get).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Automatically faces towards the obsidian being placed.").defaultValue(true).build());
   private final Setting<Boolean> protect = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("protect")
            .description("Attempts to break crystals around surround positions to prevent surround break.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> toggleOnYChange = this.sgToggles
      .add(
         new BoolSetting.Builder()
            .name("toggle-on-y-change")
            .description("Automatically disables when your y level changes (step, jumping, etc).")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> toggleOnComplete = this.sgToggles
      .add(new BoolSetting.Builder().name("toggle-on-complete").description("Toggles off when all blocks are placed.").defaultValue(false).build());
   private final Setting<Boolean> toggleOnDeath = this.sgToggles
      .add(new BoolSetting.Builder().name("toggle-on-death").description("Toggles off when you die.").defaultValue(true).build());
   private final Setting<Boolean> swing = this.sgRender
      .add(new BoolSetting.Builder().name("swing").description("Render your hand swinging when placing surround blocks.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the obsidian will be placed.").defaultValue(true).build());
   private final Setting<Boolean> renderBelow = this.sgRender
      .add(new BoolSetting.Builder().name("below").description("Renders the block below you.").defaultValue(false).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> safeSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("safe-side-color")
            .description("The side color for safe blocks.")
            .defaultValue(new SettingColor(13, 255, 0, 0))
            .visible(() -> this.render.get() && this.shapeMode.get() != ShapeMode.Lines)
            .build()
      );
   private final Setting<SettingColor> safeLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("safe-line-color")
            .description("The line color for safe blocks.")
            .defaultValue(new SettingColor(13, 255, 0, 0))
            .visible(() -> this.render.get() && this.shapeMode.get() != ShapeMode.Sides)
            .build()
      );
   private final Setting<SettingColor> normalSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("normal-side-color")
            .description("The side color for normal blocks.")
            .defaultValue(new SettingColor(0, 255, 238, 12))
            .visible(() -> this.render.get() && this.shapeMode.get() != ShapeMode.Lines)
            .build()
      );
   private final Setting<SettingColor> normalLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("normal-line-color")
            .description("The line color for normal blocks.")
            .defaultValue(new SettingColor(0, 255, 238, 100))
            .visible(() -> this.render.get() && this.shapeMode.get() != ShapeMode.Sides)
            .build()
      );
   private final Setting<SettingColor> unsafeSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("unsafe-side-color")
            .description("The side color for unsafe blocks.")
            .defaultValue(new SettingColor(204, 0, 0, 12))
            .visible(() -> this.render.get() && this.shapeMode.get() != ShapeMode.Lines)
            .build()
      );
   private final Setting<SettingColor> unsafeLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("unsafe-line-color")
            .description("The line color for unsafe blocks.")
            .defaultValue(new SettingColor(204, 0, 0, 100))
            .visible(() -> this.render.get() && this.shapeMode.get() != ShapeMode.Sides)
            .build()
      );
   public ArrayList<Module> toActivate = new ArrayList<>();
   private int timer;

   public Surround() {
      super(Categories.Combat, "surround", "Surrounds you in blocks to prevent massive crystal damage.");
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.render.get()) {
         BlockPos playerPos = this.mc.player.blockPosition();
         if (this.renderBelow.get()) {
            this.draw(playerPos.below(), event, 0);
         }

         for (Direction direction : DirectionAccessor.meteor$getHorizontal()) {
            BlockPos renderPos = playerPos.relative(direction);
            this.draw(renderPos, event, this.doubleHeight.get() ? 2 : 0);
            if (this.doubleHeight.get()) {
               this.draw(renderPos.above(), event, 4);
            }
         }
      }
   }

   private void draw(BlockPos renderPos, Render3DEvent event, int exclude) {
      Color sideColor = this.getSideColor(renderPos);
      Color lineColor = this.getLineColor(renderPos);
      event.renderer.box(renderPos, sideColor, lineColor, this.shapeMode.get(), exclude);
   }

   @Override
   public void onActivate() {
      if (this.center.get() == Surround.Center.OnActivate) {
         PlayerUtils.centerPlayer();
      }

      this.timer = this.delay.get();
      if (this.toggleModules.get() && !this.modules.get().isEmpty() && this.mc.level != null && this.mc.player != null) {
         for (Module module : this.modules.get()) {
            if (module.isActive()) {
               module.toggle();
               this.toActivate.add(module);
            }
         }
      }
   }

   @Override
   public void onDeactivate() {
      if (this.toggleBack.get() && !this.toActivate.isEmpty() && this.mc.level != null && this.mc.player != null) {
         for (Module module : this.toActivate) {
            module.enable();
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.timer++ >= this.delay.get()) {
         if (this.toggleOnYChange.get() && this.mc.player.yo != this.mc.player.getY()) {
            this.toggle();
         } else if (!this.onlyOnGround.get() || this.mc.player.onGround()) {
            FindItemResult block = InvUtils.findInHotbar(itemStack -> this.blocks.get().contains(Block.byItem(itemStack.getItem())));
            if (block.found()) {
               if (this.center.get() == Surround.Center.Always) {
                  PlayerUtils.centerPlayer();
               }

               int placedCount = 0;
               boolean complete = true;
               BlockPos playerPos = this.mc.player.blockPosition();

               for (Direction direction : DirectionAccessor.meteor$getHorizontal()) {
                  BlockPos placePos = playerPos.relative(direction);
                  if (!this.airPlace.get() && this.isAirPlace(placePos) && this.mc.level.getBlockState(placePos).canBeReplaced()) {
                     if (this.place(placePos.below(), block)) {
                        if (++placedCount >= this.blocksPerTick.get()) {
                           break;
                        }
                     }

                     if (this.mc.level.getBlockState(placePos.below()).canBeReplaced()) {
                        complete = false;
                     }
                  }

                  if (this.place(placePos, block)) {
                     if (++placedCount >= this.blocksPerTick.get()) {
                        break;
                     }
                  }

                  if (this.mc.level.getBlockState(placePos).canBeReplaced()) {
                     complete = false;
                  }
               }

               if (this.doubleHeight.get() && complete) {
                  for (Direction direction : DirectionAccessor.meteor$getHorizontal()) {
                     BlockPos placePos = playerPos.relative(direction).above();
                     if (this.place(placePos, block)) {
                        if (++placedCount >= this.blocksPerTick.get()) {
                           break;
                        }
                     }

                     if (this.mc.level.getBlockState(placePos).canBeReplaced()) {
                        complete = false;
                     }
                  }
               }

               this.timer = 0;
               if (complete && this.toggleOnComplete.get()) {
                  this.toggle();
               } else {
                  if (!complete && this.center.get() == Surround.Center.Incomplete) {
                     PlayerUtils.centerPlayer();
                  }
               }
            }
         }
      }
   }

   private boolean place(BlockPos placePos, FindItemResult block) {
      boolean placed = BlockUtils.place(placePos, block, this.rotate.get(), 100, this.swing.get(), true);
      boolean beingMined = false;

      for (BlockDestructionProgress value : ((LevelRendererAccessor)this.mc.levelRenderer).meteor$getDestroyingBlocks().values()) {
         if (value.getPos().equals(placePos)) {
            beingMined = true;
            break;
         }
      }

      boolean isThreat = this.mc.level.getBlockState(placePos).canBeReplaced() || beingMined;
      if (this.protect.get() && !placed && isThreat) {
         AABB box = new AABB(placePos.getX() - 1, placePos.getY() - 1, placePos.getZ() - 1, placePos.getX() + 1, placePos.getY() + 1, placePos.getZ() + 1);
         Predicate<Entity> entityPredicate = entity -> entity instanceof EndCrystal
            && DamageUtils.crystalDamage(this.mc.player, entity.position()) < PlayerUtils.getTotalHealth();

         for (Entity crystal : this.mc.level.getEntities((Entity)null, box, entityPredicate)) {
            if (this.rotate.get()) {
               Rotations.rotate(
                  Rotations.getPitch(crystal), Rotations.getYaw(crystal), () -> this.mc.player.connection.send(new ServerboundAttackPacket(crystal.getId()))
               );
            } else {
               this.mc.player.connection.send(new ServerboundAttackPacket(crystal.getId()));
            }

            this.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         }
      }

      return placed;
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundPlayerCombatKillPacket packet) {
         Entity entity = this.mc.level.getEntity(packet.playerId());
         if (entity == this.mc.player && this.toggleOnDeath.get()) {
            this.toggle();
            this.info("Toggled off because you died.");
         }
      }
   }

   private Surround.BlockType getBlockType(BlockPos pos) {
      BlockState blockState = this.mc.level.getBlockState(pos);
      if (blockState.getBlock().defaultDestroyTime() < 0.0F) {
         return Surround.BlockType.Safe;
      } else {
         return blockState.getBlock().getExplosionResistance() >= 600.0F ? Surround.BlockType.Normal : Surround.BlockType.Unsafe;
      }
   }

   private Color getSideColor(BlockPos pos) {
      return switch (this.getBlockType(pos)) {
         case Safe -> (SettingColor)this.safeSideColor.get();
         case Normal -> (SettingColor)this.normalSideColor.get();
         case Unsafe -> (SettingColor)this.unsafeSideColor.get();
      };
   }

   private Color getLineColor(BlockPos pos) {
      return switch (this.getBlockType(pos)) {
         case Safe -> (SettingColor)this.safeLineColor.get();
         case Normal -> (SettingColor)this.normalLineColor.get();
         case Unsafe -> (SettingColor)this.unsafeLineColor.get();
      };
   }

   private boolean isAirPlace(BlockPos blockPos) {
      for (Direction direction : Direction.values()) {
         if (!this.mc.level.getBlockState(blockPos.relative(direction)).canBeReplaced()) {
            return false;
         }
      }

      return true;
   }

   private boolean blockFilter(Block block) {
      return block.getExplosionResistance() >= 600.0F && block.defaultDestroyTime() >= 0.0F && block != Blocks.REINFORCED_DEEPSLATE;
   }

   public enum BlockType {
      Safe,
      Normal,
      Unsafe;
   }

   public enum Center {
      Never,
      OnActivate,
      Incomplete,
      Always;
   }
}
