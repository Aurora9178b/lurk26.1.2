package meteordevelopment.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.player.BlockBreakingCooldownEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class Nuker extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgWhitelist = this.settings.createGroup("Whitelist");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Nuker.Shape> shape = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape"))
                  .description("The shape of nuking algorithm."))
               .defaultValue(Nuker.Shape.Sphere))
            .build()
      );
   private final Setting<Nuker.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("The way the blocks are broken."))
               .defaultValue(Nuker.Mode.Flatten))
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("The break range.")
            .defaultValue(4.0)
            .min(0.0)
            .visible(() -> this.shape.get() != Nuker.Shape.Cube)
            .build()
      );
   private final Setting<Integer> range_up = this.sgGeneral
      .add(
         new IntSetting.Builder().name("up").description("The break range.").defaultValue(1).min(0).visible(() -> this.shape.get() == Nuker.Shape.Cube).build()
      );
   private final Setting<Integer> range_down = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("down")
            .description("The break range.")
            .defaultValue(1)
            .min(0)
            .visible(() -> this.shape.get() == Nuker.Shape.Cube)
            .build()
      );
   private final Setting<Integer> range_left = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("left")
            .description("The break range.")
            .defaultValue(1)
            .min(0)
            .visible(() -> this.shape.get() == Nuker.Shape.Cube)
            .build()
      );
   private final Setting<Integer> range_right = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("right")
            .description("The break range.")
            .defaultValue(1)
            .min(0)
            .visible(() -> this.shape.get() == Nuker.Shape.Cube)
            .build()
      );
   private final Setting<Integer> range_forward = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("forward")
            .description("The break range.")
            .defaultValue(1)
            .min(0)
            .visible(() -> this.shape.get() == Nuker.Shape.Cube)
            .build()
      );
   private final Setting<Integer> range_back = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("back")
            .description("The break range.")
            .defaultValue(1)
            .min(0)
            .visible(() -> this.shape.get() == Nuker.Shape.Cube)
            .build()
      );
   private final Setting<Double> wallsRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("Range in which to break when behind blocks.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("Delay in ticks between breaking blocks.").defaultValue(0).build());
   private final Setting<Integer> maxBlocksPerTick = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-blocks-per-tick")
            .description("Maximum blocks to try to break per tick. Useful when insta mining.")
            .defaultValue(1)
            .min(1)
            .build()
      );
   private final Setting<Nuker.SortMode> sortMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("sort-mode"))
                  .description("The blocks you want to mine first."))
               .defaultValue(Nuker.SortMode.Closest))
            .build()
      );
   private final Setting<Boolean> packetMine = this.sgGeneral
      .add(new BoolSetting.Builder().name("packet-mine").description("Attempt to instamine everything at once.").defaultValue(false).build());
   private final Setting<Boolean> suitableTools = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-suitable-tools").description("Only mines when using an appropriate for the block.").defaultValue(false).build());
   private final Setting<Boolean> interact = this.sgGeneral
      .add(new BoolSetting.Builder().name("interact").description("Interacts with the block instead of mining.").defaultValue(false).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates server-side to the block being mined.").defaultValue(true).build());
   private final Setting<Nuker.ListMode> listMode = this.sgWhitelist
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("list-mode")).description("Selection mode."))
               .defaultValue(Nuker.ListMode.Blacklist))
            .build()
      );
   private final Setting<List<Block>> blacklist = this.sgWhitelist
      .add(
         new BlockListSetting.Builder()
            .name("blacklist")
            .description("The blocks you don't want to mine.")
            .visible(() -> this.listMode.get() == Nuker.ListMode.Blacklist)
            .build()
      );
   private final Setting<List<Block>> whitelist = this.sgWhitelist
      .add(
         new BlockListSetting.Builder()
            .name("whitelist")
            .description("The blocks you want to mine.")
            .visible(() -> this.listMode.get() == Nuker.ListMode.Whitelist)
            .build()
      );
   private final Setting<Keybind> selectBlockBind = this.sgWhitelist
      .add(
         new KeybindSetting.Builder()
            .name("select-block-bind")
            .description("Adds targeted block to list when this button is pressed.")
            .defaultValue(Keybind.none())
            .build()
      );
   private final Setting<Boolean> swing = this.sgRender
      .add(new BoolSetting.Builder().name("swing").description("Whether to swing hand client-side.").defaultValue(true).build());
   private final Setting<Boolean> enableRenderBounding = this.sgRender
      .add(new BoolSetting.Builder().name("bounding-box").description("Enable rendering bounding box for Cube and Uniform Cube.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeModeBox = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("nuke-box-mode"))
                  .description("How the shape for the bounding box is rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColorBox = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the bounding box.")
            .defaultValue(new SettingColor(16, 106, 144, 100))
            .build()
      );
   private final Setting<SettingColor> lineColorBox = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the bounding box.")
            .defaultValue(new SettingColor(16, 106, 144, 255))
            .build()
      );
   private final Setting<Boolean> enableRenderBreaking = this.sgRender
      .add(new BoolSetting.Builder().name("broken-blocks").description("Enable rendering bounding box for Cube and Uniform Cube.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeModeBreak = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("nuke-block-mode"))
                     .description("How the shapes for broken blocks are rendered."))
                  .defaultValue(ShapeMode.Both))
               .visible(this.enableRenderBreaking::get))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the target block rendering.")
            .defaultValue(new SettingColor(255, 0, 0, 80))
            .visible(this.enableRenderBreaking::get)
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the target block rendering.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(this.enableRenderBreaking::get)
            .build()
      );
   private final List<BlockPos> blocks = new ArrayList<>();
   private final Set<BlockPos> interacted = new ObjectOpenHashSet<>();
   private boolean firstBlock;
   private final MutableBlockPos lastBlockPos = new MutableBlockPos();
   private int timer;
   private int noBlockTimer;
   private final MutableBlockPos pos1 = new MutableBlockPos();
   private final MutableBlockPos pos2 = new MutableBlockPos();
   int maxh = 0;
   int maxv = 0;

   public Nuker() {
      super(Categories.World, "nuker", "Breaks blocks around you.");
   }

   @Override
   public void onActivate() {
      this.firstBlock = true;
      this.timer = 0;
      this.noBlockTimer = 0;
      this.interacted.clear();
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.enableRenderBounding.get() && this.shape.get() != Nuker.Shape.Sphere && this.mode.get() != Nuker.Mode.Smash) {
         int minX = Math.min(this.pos1.getX(), this.pos2.getX());
         int minY = Math.min(this.pos1.getY(), this.pos2.getY());
         int minZ = Math.min(this.pos1.getZ(), this.pos2.getZ());
         int maxX = Math.max(this.pos1.getX(), this.pos2.getX());
         int maxY = Math.max(this.pos1.getY(), this.pos2.getY());
         int maxZ = Math.max(this.pos1.getZ(), this.pos2.getZ());
         event.renderer.box(minX, minY, minZ, maxX, maxY, maxZ, this.sideColorBox.get(), this.lineColorBox.get(), this.shapeModeBox.get(), 0);
      }
   }

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      if (event.action == KeyAction.Press) {
         this.addTargetedBlockToList();
      }
   }

   @EventHandler
   private void onKey(KeyInputEvent event) {
      if (event.action == KeyAction.Press) {
         this.addTargetedBlockToList();
      }
   }

   @EventHandler
   private void onTickPre(TickEvent.Pre event) {
      if (this.timer > 0) {
         this.timer--;
      } else {
         double pX = this.mc.player.getX();
         double pY = this.mc.player.getY();
         double pZ = this.mc.player.getZ();
         double rangeSq = Math.pow(this.range.get(), 2.0);
         BlockPos playerBlockPos = this.mc.player.blockPosition();
         if (this.shape.get() == Nuker.Shape.UniformCube) {
            this.range.set((double)Math.round(this.range.get()));
         }

         double pX_ = pX;
         double pZ_ = pZ;
         int r = (int)Math.round(this.range.get());
         if (this.shape.get() == Nuker.Shape.UniformCube) {
            pX_++;
            this.pos1.set(pX_ - r, pY - r + 1.0, pZ - r + 1.0);
            this.pos2.set(pX_ + r - 1.0, pY + r, pZ + r);
            this.maxh = 0;
            this.maxv = 0;
         } else {
            Direction direction = this.mc.player.getDirection();
            switch (direction) {
               case SOUTH:
                  pZ_++;
                  pX_++;
                  this.pos1.set(pX_ - (this.range_right.get() + 1), Math.ceil(pY) - this.range_down.get().intValue(), pZ_ - (this.range_back.get() + 1));
                  this.pos2
                     .set(
                        pX_ + this.range_left.get().intValue(), Math.ceil(pY + this.range_up.get().intValue() + 1.0), pZ_ + this.range_forward.get().intValue()
                     );
                  break;
               case WEST:
                  this.pos1
                     .set(pX_ - this.range_forward.get().intValue(), Math.ceil(pY) - this.range_down.get().intValue(), pZ_ - this.range_right.get().intValue());
                  this.pos2
                     .set(
                        pX_ + this.range_back.get().intValue() + 1.0,
                        Math.ceil(pY + this.range_up.get().intValue() + 1.0),
                        pZ_ + this.range_left.get().intValue() + 1.0
                     );
                  break;
               case NORTH:
                  pX_++;
                  pZ_++;
                  this.pos1.set(pX_ - (this.range_left.get() + 1), Math.ceil(pY) - this.range_down.get().intValue(), pZ_ - (this.range_forward.get() + 1));
                  this.pos2
                     .set(pX_ + this.range_right.get().intValue(), Math.ceil(pY + this.range_up.get().intValue() + 1.0), pZ_ + this.range_back.get().intValue());
                  break;
               case EAST:
                  pX_++;
                  this.pos1.set(pX_ - (this.range_back.get() + 1), Math.ceil(pY) - this.range_down.get().intValue(), pZ_ - this.range_left.get().intValue());
                  this.pos2
                     .set(
                        pX_ + this.range_forward.get().intValue(),
                        Math.ceil(pY + this.range_up.get().intValue() + 1.0),
                        pZ_ + this.range_right.get().intValue() + 1.0
                     );
            }

            this.maxh = 1 + Math.max(Math.max(Math.max(this.range_back.get(), this.range_right.get()), this.range_forward.get()), this.range_left.get());
            this.maxv = 1 + Math.max(this.range_up.get(), this.range_down.get());
         }

         if (this.mode.get() == Nuker.Mode.Flatten) {
            this.pos1.setY((int)Math.floor(pY + 0.5));
         }

         AABB box = new AABB(this.pos1.getCenter(), this.pos2.getCenter());
         BlockIterator.register(
            Math.max((int)Math.ceil(this.range.get() + 1.0), this.maxh),
            Math.max((int)Math.ceil(this.range.get()), this.maxv),
            (blockPos, blockState) -> {
               Vec3 center = blockPos.getCenter();
               switch ((Nuker.Shape)this.shape.get()) {
                  case Cube:
                     if (!box.contains(center)) {
                        return;
                     }
                     break;
                  case UniformCube:
                     if (chebyshevDist(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ())
                        >= this.range.get()) {
                        return;
                     }
                     break;
                  case Sphere:
                     if (Utils.squaredDistance(pX, pY, pZ, center.x(), center.y(), center.z()) > rangeSq) {
                        return;
                     }
               }

               if (this.mode.get() != Nuker.Mode.Flatten || !(blockPos.getY() + 0.5 < pY)) {
                  if (this.mode.get() != Nuker.Mode.Smash || blockState.getDestroySpeed(this.mc.level, blockPos) == 0.0F) {
                     if (!this.suitableTools.get() || this.interact.get() || this.mc.player.getMainHandItem().isCorrectToolForDrops(blockState)) {
                        if (BlockUtils.canBreak(blockPos, blockState) || this.interact.get()) {
                           if (!this.isOutOfRange(blockPos)) {
                              if (this.listMode.get() != Nuker.ListMode.Whitelist || this.whitelist.get().contains(blockState.getBlock())) {
                                 if (this.listMode.get() != Nuker.ListMode.Blacklist || !this.blacklist.get().contains(blockState.getBlock())) {
                                    if (!this.interact.get() || !this.interacted.contains(blockPos)) {
                                       this.blocks.add(blockPos.immutable());
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         );
         BlockIterator.after(
            () -> {
               if (this.sortMode.get() == Nuker.SortMode.TopDown) {
                  this.blocks.sort(Comparator.comparingDouble(value -> -value.getY()));
               } else if (this.sortMode.get() != Nuker.SortMode.None) {
                  this.blocks
                     .sort(
                        Comparator.comparingDouble(
                           value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5)
                              * (this.sortMode.get() == Nuker.SortMode.Closest ? 1 : -1)
                        )
                     );
               }

               if (this.blocks.isEmpty()) {
                  this.interacted.clear();
                  if (this.noBlockTimer++ >= this.delay.get()) {
                     this.firstBlock = true;
                  }
               } else {
                  this.noBlockTimer = 0;
                  if (!this.firstBlock && !this.lastBlockPos.equals(this.blocks.getFirst())) {
                     this.timer = this.delay.get();
                     this.firstBlock = false;
                     this.lastBlockPos.set(this.blocks.getFirst());
                     if (this.timer > 0) {
                        return;
                     }
                  }

                  int count = 0;

                  for (BlockPos block : this.blocks) {
                     if (count >= this.maxBlocksPerTick.get()) {
                        break;
                     }

                     boolean canInstaMine = BlockUtils.canInstaBreak(block);
                     if (this.rotate.get()) {
                        Rotations.rotate(Rotations.getYaw(block), Rotations.getPitch(block), () -> this.breakBlock(block));
                     } else {
                        this.breakBlock(block);
                     }

                     if (this.enableRenderBreaking.get()) {
                        RenderUtils.renderTickingBlock(block, this.sideColor.get(), this.lineColor.get(), this.shapeModeBreak.get(), 0, 8, true, false);
                     }

                     this.lastBlockPos.set(block);
                     count++;
                     if (!canInstaMine && !this.packetMine.get()) {
                        break;
                     }
                  }

                  this.firstBlock = false;
                  this.blocks.clear();
               }
            }
         );
      }
   }

   private void breakBlock(BlockPos blockPos) {
      if (this.interact.get()) {
         BlockUtils.interact(
            new BlockHitResult(blockPos.getCenter(), BlockUtils.getDirection(blockPos), blockPos, true), InteractionHand.MAIN_HAND, this.swing.get()
         );
         this.interacted.add(blockPos);
      } else if (this.packetMine.get()) {
         this.mc
            .gameMode
            .startPrediction(
               this.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos), sequence)
            );
         if (this.swing.get()) {
            this.mc.player.swing(InteractionHand.MAIN_HAND);
         } else {
            this.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         }

         this.mc
            .gameMode
            .startPrediction(
               this.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos), sequence)
            );
      } else {
         BlockUtils.breakBlock(blockPos, this.swing.get());
      }
   }

   private boolean isOutOfRange(BlockPos blockPos) {
      Vec3 pos = blockPos.getCenter();
      ClipContext clipContext = new ClipContext(
         this.mc.player.getEyePosition(), pos, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this.mc.player
      );
      BlockHitResult result = this.mc.level.clip(clipContext);
      return result != null && result.getBlockPos().equals(blockPos) ? false : !PlayerUtils.isWithin(pos, this.wallsRange.get());
   }

   private void addTargetedBlockToList() {
      if (this.selectBlockBind.get().isPressed() && this.mc.screen == null) {
         HitResult hitResult = this.mc.hitResult;
         if (hitResult != null && hitResult.getType() == Type.BLOCK) {
            BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
            Block targetBlock = this.mc.level.getBlockState(pos).getBlock();
            List<Block> list = this.listMode.get() == Nuker.ListMode.Whitelist ? this.whitelist.get() : this.blacklist.get();
            String modeName = this.listMode.get().name();
            if (list.contains(targetBlock)) {
               list.remove(targetBlock);
               this.info("Removed " + Names.get(targetBlock) + " from " + modeName);
            } else {
               list.add(targetBlock);
               this.info("Added " + Names.get(targetBlock) + " to " + modeName);
            }
         }
      }
   }

   @EventHandler(priority = 200)
   private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
      event.cooldown = 0;
   }

   public static int chebyshevDist(int x1, int y1, int z1, int x2, int y2, int z2) {
      int dX = Math.abs(x2 - x1);
      int dY = Math.abs(y2 - y1);
      int dZ = Math.abs(z2 - z1);
      return Math.max(Math.max(dX, dY), dZ);
   }

   public enum ListMode {
      Whitelist,
      Blacklist;
   }

   public enum Mode {
      All,
      Flatten,
      Smash;
   }

   public enum Shape {
      Cube,
      UniformCube,
      Sphere;
   }

   public enum SortMode {
      None,
      Closest,
      Furthest,
      TopDown;
   }
}
