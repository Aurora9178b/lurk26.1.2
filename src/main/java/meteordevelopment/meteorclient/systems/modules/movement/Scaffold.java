package meteordevelopment.meteorclient.systems.modules.movement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.RaytraceUtils;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Scaffold.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("The scaffold mode."))
               .defaultValue(Scaffold.Mode.Grim))
            .build()
      );
   private final Setting<Boolean> skipTicks = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("skip-ticks")
            .description("Sends a rotation packet and places immediately when falling fast.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> snap = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("snap")
            .description("Keeps GodBridge rotation snapped until a placement is needed.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == Scaffold.Mode.GodBridge)
            .build()
      );
   private final Setting<Scaffold.RotationMode> rotationMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("rotation-mode")).description("Rotation speed mode."))
               .defaultValue(Scaffold.RotationMode.Rise))
            .build()
      );
   private final Setting<Scaffold.RaytraceMode> raytrace = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("raytrace-mode")).description("Raytrace check mode."))
               .defaultValue(Scaffold.RaytraceMode.Normal))
            .build()
      );
   private final Setting<Integer> rotateSpeed = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("rotation-speed")
            .description("Rotation speed.")
            .defaultValue(10)
            .range(1, 10)
            .sliderRange(1, 10)
            .visible(() -> this.rotationMode.get() == Scaffold.RotationMode.Rise)
            .build()
      );
   private final Setting<Integer> rotateBackSpeed = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("rotation-back-speed")
            .description("Rotation back speed.")
            .defaultValue(10)
            .range(1, 10)
            .sliderRange(1, 10)
            .visible(this::isBridgeMode)
            .build()
      );
   private final Setting<Integer> placeTicks = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("place-ticks")
            .description("Ticks to wait before placing in bridge modes.")
            .defaultValue(1)
            .range(0, 6)
            .sliderRange(0, 6)
            .visible(this::isBridgeMode)
            .build()
      );
   private final Setting<Boolean> safeWalk = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("safe-walk")
            .description("Sneaks on block edges in GodBridge.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == Scaffold.Mode.GodBridge)
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders placed blocks.").defaultValue(true).build());
   private final Setting<Boolean> fade = this.sgRender
      .add(new BoolSetting.Builder().name("fade").description("Fades rendered boxes.").defaultValue(true).visible(this.render::get).build());
   private final Setting<Integer> fadeTime = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("fade-time")
            .description("Render fade time in milliseconds.")
            .defaultValue(500)
            .range(0, 3000)
            .sliderRange(0, 3000)
            .visible(() -> this.render.get() && this.fade.get())
            .build()
      );
   private final Setting<Boolean> shrink = this.sgRender
      .add(new BoolSetting.Builder().name("shrink").description("Shrinks rendered boxes while fading.").defaultValue(false).visible(this.render::get).build());
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 183, 197, 100))
            .visible(this.render::get)
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 105, 180, 255))
            .visible(this.render::get)
            .build()
      );
   private int airTicks;
   private int yLevel;
   private BlockPos blockPos;
   private Direction direction;
   private Rotation rotation;
   private int rotateCount;
   private FindItemResult blockResult;
   private boolean shouldSwapBack;
   private final List<Scaffold.RenderInfo> renderBoxes = new ArrayList<>();
   private static final List<Block> BLACKLISTED_BLOCKS = List.of(
      Blocks.AIR,
      Blocks.WATER,
      Blocks.LAVA,
      Blocks.ENCHANTING_TABLE,
      Blocks.GLASS_PANE,
      Blocks.IRON_BARS,
      Blocks.SNOW,
      Blocks.COAL_ORE,
      Blocks.DIAMOND_ORE,
      Blocks.EMERALD_ORE,
      Blocks.CHEST,
      Blocks.TRAPPED_CHEST,
      Blocks.TORCH,
      Blocks.ANVIL,
      Blocks.NOTE_BLOCK,
      Blocks.JUKEBOX,
      Blocks.TNT,
      Blocks.GOLD_ORE,
      Blocks.IRON_ORE,
      Blocks.LAPIS_ORE,
      Blocks.STONE_PRESSURE_PLATE,
      Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
      Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
      Blocks.STONE_BUTTON,
      Blocks.LEVER,
      Blocks.TALL_GRASS,
      Blocks.TRIPWIRE,
      Blocks.TRIPWIRE_HOOK,
      Blocks.RAIL,
      Blocks.CORNFLOWER,
      Blocks.RED_MUSHROOM,
      Blocks.BROWN_MUSHROOM,
      Blocks.VINE,
      Blocks.SUNFLOWER,
      Blocks.LADDER,
      Blocks.FURNACE,
      Blocks.SAND,
      Blocks.CACTUS,
      Blocks.DISPENSER,
      Blocks.DROPPER,
      Blocks.CRAFTING_TABLE,
      Blocks.COBWEB,
      Blocks.PUMPKIN,
      Blocks.COBBLESTONE_WALL,
      Blocks.OAK_FENCE,
      Blocks.REDSTONE_TORCH,
      Blocks.FLOWER_POT
   );

   public Scaffold() {
      super(Categories.Movement, "scaffold", "Bypass Scaffold");
   }

   @Override
   public String getInfoString() {
      return this.mode.get().toString();
   }

   @Override
   public void onActivate() {
      this.airTicks = 0;
      this.blockPos = null;
      this.direction = null;
      this.rotation = null;
      this.rotateCount = 0;
      this.blockResult = null;
      this.shouldSwapBack = false;
   }

   @Override
   public void onDeactivate() {
      this.yLevel = 0;
      if (this.mc.player != null) {
         if (this.shouldSwapBack) {
            this.swapBack();
         }

         RotationManager.INSTANCE.reset();
         this.mc.options.keyShift.setDown(Input.isPressed(this.mc.options.keyShift));
      }
   }

   @EventHandler
   private void onMotion(SendMovementPacketsEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (!this.isBridgeMode() && this.safeWalk.get()) {
            this.mc.options.keyShift.setDown(this.mc.player.onGround() && this.isOnBlockEdge(0.3F));
         }
      }
   }

   @EventHandler
   private void onPlayerTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null && this.mc.getConnection() != null) {
         this.blockResult = this.findBlockResult();
         if (this.blockResult.found()) {
            if (this.mc.player.onGround()) {
               this.airTicks = 0;
               this.yLevel = Mth.floor(this.mc.player.getY()) - 1;
            } else {
               this.airTicks++;
            }

            this.getBlockInfo();
            if (this.skipTicks.get() && this.blockPos != null) {
               boolean reachable = true;
               if (this.mc.player.getDeltaMovement().y < -0.1) {
                  double predictedY = this.predictY(2);
                  if (this.blockPos.getY() > predictedY) {
                     reachable = false;
                  }
               }

               double horizontalSpeed = Math.hypot(this.mc.player.getDeltaMovement().x, this.mc.player.getDeltaMovement().z);
               if ((!reachable || horizontalSpeed >= 1.5) && this.rotateCount <= 8 && this.getBlockCount() >= 1) {
                  Rotation skipRotation = this.getRotation(this.blockPos, this.direction);
                  Rotation packetRotation = this.getPacketRotation(skipRotation);
                  this.rotateCount++;
                  RotationManager.INSTANCE.rotations = packetRotation.copy();
                  RotationManager.INSTANCE
                     .setRotations(packetRotation, this.rotateSpeed.get().intValue(), this.rotateBackSpeed.get().intValue(), Priority.High);
                  this.mc
                     .getConnection()
                     .send(new Rot(packetRotation.getYaw(), packetRotation.getPitch(), this.mc.player.onGround(), this.mc.player.horizontalCollision));
                  this.placeWithCurrentTarget();
                  return;
               }

               this.rotateCount = 0;
            }

            switch ((Scaffold.Mode)this.mode.get()) {
               case Grim:
                  this.handleBridge();
                  break;
               case GodBridge:
                  this.handleNormal();
            }
         }
      }
   }

   @EventHandler(priority = 100)
   private void onMoveInput(MoveInputEvent event) {
      if (this.mc.player != null) {
         if (this.mc.player.onGround() && !this.mc.options.keyJump.isDown() && this.hasMovementInput(event) && this.isBridgeMode()) {
            event.setJump(true);
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get() && !this.renderBoxes.isEmpty()) {
         long time = System.currentTimeMillis();
         long configuredFadeTime = this.fadeTime.get().intValue();
         long lifeTime = Math.max(configuredFadeTime, 1L);
         this.renderBoxes.removeIf(boxx -> time - boxx.startTime() > configuredFadeTime);

         for (Scaffold.RenderInfo box : this.renderBoxes) {
            float progress = Mth.clamp((float)(time - box.startTime()) / (float)lifeTime, 0.0F, 1.0F);
            double scale = 1.0;
            if (box.shrink()) {
               scale = 1.0 - this.easeInOutExpo(progress);
               if (scale < 0.0) {
                  scale = 0.0;
               }
            }

            float alphaFactor = box.fade() ? Mth.clamp(1.0F - progress, 0.0F, 1.0F) : 1.0F;
            Color side = this.withAlpha(box.sideColor(), alphaFactor);
            Color line = this.withAlpha(box.lineColor(), alphaFactor);
            AABB renderBox = box.aabb();
            if (box.shrink()) {
               renderBox = AABB.ofSize(renderBox.getCenter(), renderBox.getXsize() * scale, renderBox.getYsize() * scale, renderBox.getZsize() * scale);
            }

            event.renderer.box(renderBox, side, line, ShapeMode.Both, 0);
         }
      }
   }

   public int getBlockCount() {
      if (this.mc.player == null) {
         return 0;
      }

      int total = 0;
      if (this.isValidStack(this.mc.player.getOffhandItem())) {
         total += this.mc.player.getOffhandItem().getCount();
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (this.isValidStack(stack)) {
            total += stack.getCount();
         }
      }

      return total;
   }

   public boolean scaffolding() {
      return this.isActive();
   }

   public boolean towering() {
      return false;
   }

   private void handleBridge() {
      if (this.mc.player.onGround()) {
         RotationManager.INSTANCE
            .setRotations(
               new Rotation(this.mc.player.getYRot(), this.rotation == null ? this.mc.player.getXRot() : this.rotation.getPitch()),
               this.rotateBackSpeed.get().intValue(),
               Priority.High
            );
      } else {
         this.rotation = this.getRotation(this.blockPos, this.direction);
         double speed = this.rotateSpeed.get().intValue();
         if (this.rotationMode.get() == Scaffold.RotationMode.Hypixel) {
            speed = this.airTicks <= 1 ? 7.055 : 1.944;
         }

         this.setScaffoldRotations(this.rotation, speed, this.rotateBackSpeed.get().intValue());
         if (this.airTicks > this.placeTicks.get()) {
            this.place();
         }
      }
   }

   private void handleNormal() {
      if (this.onAir() || !this.snap.get()) {
         this.rotation = this.getRotation(this.blockPos, this.direction);
         this.setScaffoldRotations(this.rotation, this.rotateSpeed.get().intValue(), this.rotateBackSpeed.get().intValue());
      }

      this.place();
   }

   private void place() {
      if (this.onAir() && this.blockPos != null && this.direction != null) {
         boolean misses = switch ((Scaffold.RaytraceMode)this.raytrace.get()) {
            case Normal -> !RaytraceUtils.overBlock(RotationManager.INSTANCE.getRotation(), this.blockPos);
            case Strict -> !RaytraceUtils.overBlock(RotationManager.INSTANCE.getRotation(), this.blockPos, this.direction);
         };
         if (!misses) {
            this.placeWithCurrentTarget();
         }
      }
   }

   private void placeWithCurrentTarget() {
      if (this.blockPos != null && this.direction != null && this.blockResult != null) {
         this.swap();
         InteractionHand hand = this.getPlacementHand();
         if (hand != null && this.isValidStack(this.mc.player.getItemInHand(hand))) {
            InteractionResult result = this.mc
               .gameMode
               .useItemOn(this.mc.player, hand, new BlockHitResult(this.getVec3(this.blockPos, this.direction), this.direction, this.blockPos, false));
            if (result.consumesAction()) {
               this.mc.player.swing(hand);
               if (this.render.get()) {
                  this.renderBoxes
                     .add(
                        new Scaffold.RenderInfo(
                           new AABB(this.blockPos.relative(this.direction)),
                           this.lineColor.get().copy(),
                           this.sideColor.get().copy(),
                           System.currentTimeMillis(),
                           this.fade.get(),
                           this.shrink.get()
                        )
                     );
               }
            }
         } else {
            this.swapBack();
         }
      }
   }

   private int getYLevel() {
      return !this.mc.options.keyJump.isDown() && PlayerUtils.isMoving() && this.mc.player.fallDistance <= 0.25 && this.isBridgeMode()
         ? this.yLevel
         : Mth.floor(this.mc.player.getY()) - 1;
   }

   private void getBlockInfo() {
      this.blockPos = null;
      this.direction = null;
      Vec3 baseVec = this.mc.player.getEyePosition();
      BlockPos base = BlockPos.containing(baseVec.x, this.getYLevel(), baseVec.z);
      int baseX = base.getX();
      int baseZ = base.getZ();
      if (this.onAir()) {
         if (!this.checkBlock(baseVec, base)) {
            for (int d = 1; d <= 6; d++) {
               if (this.checkBlock(baseVec, new BlockPos(baseX, this.getYLevel() - d, baseZ))) {
                  return;
               }

               for (int x = 0; x <= d; x++) {
                  for (int z = 0; z <= d - x; z++) {
                     int y = d - x - z;

                     for (int rev1 = 0; rev1 <= 1; rev1++) {
                        for (int rev2 = 0; rev2 <= 1; rev2++) {
                           BlockPos pos = new BlockPos(baseX + (rev1 == 0 ? x : -x), this.getYLevel() - y, baseZ + (rev2 == 0 ? z : -z));
                           if (this.checkBlock(baseVec, pos)) {
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean checkBlock(Vec3 baseVec, BlockPos pos) {
      if (this.onAir() && pos.getY() <= this.getYLevel()) {
         Vec3 center = pos.getBottomCenter();

         for (Direction dir : Direction.values()) {
            Vec3 normal = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
            Vec3 hit = center.add(normal.scale(0.5));
            BlockPos baseBlockPos = pos.relative(dir);
            BlockState state = this.mc.level.getBlockState(baseBlockPos);
            if (!state.getCollisionShape(this.mc.level, baseBlockPos).isEmpty() && state.getMenuProvider(this.mc.level, baseBlockPos) == null) {
               Direction face = dir.getOpposite();
               if (!(hit.distanceToSqr(baseVec) > 20.25)
                  && !(hit.subtract(baseVec).dot(normal) < 0.0)
                  && (face != Direction.UP || !PlayerUtils.isMoving() || this.mc.options.keyJump.isDown())) {
                  this.blockPos = baseBlockPos;
                  this.direction = face;
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private Rotation getRotation(BlockPos pos, Direction direction) {
      if (this.rotation == null) {
         return new Rotation(Mth.wrapDegrees(this.mc.player.getYRot() - 135.0F), 82.0F);
      }

      if (this.onAir() && pos != null && direction != null) {
         Rotation calculated = RotationUtil.calculate(pos, direction);
         Float[] yawArray = new Float[]{-135.0F, -90.0F, -45.0F, 0.0F, 45.0F, 90.0F, 135.0F, 180.0F, calculated.getYaw()};
         Arrays.sort(
            yawArray,
            (a, b) -> Float.compare(
               Math.abs(Mth.wrapDegrees(this.mc.player.getYRot() - 180.0F - a)), Math.abs(Mth.wrapDegrees(this.mc.player.getYRot() - 180.0F - b))
            )
         );
         float[] pitchArray = new float[]{75.0F, 82.0F, 87.0F};
         Float[] var6 = yawArray;
         int var7 = var6.length;

         for (int var8 = 0; var8 < var7; var8++) {
            float yaw = var6[var8];

            for (float pitch : pitchArray) {
               Rotation candidate = new Rotation(yaw + this.randomRotationOffset(), pitch + this.randomRotationOffset());
               boolean matches = this.matchesScaffoldRaytrace(candidate, pos, direction);
               if (matches) {
                  return candidate;
               }
            }

            for (int pitch = -90; pitch < 90; pitch++) {
               Rotation candidate = new Rotation(yaw, pitch);
               boolean matches = this.matchesScaffoldRaytrace(candidate, pos, direction);
               if (matches) {
                  return candidate;
               }
            }
         }

         return calculated;
      } else {
         return this.rotation;
      }
   }

   private Function<Rotation, Boolean> scaffoldRaytrace(BlockPos pos, Direction direction) {
      return candidate -> pos != null && direction != null ? this.matchesScaffoldRaytrace(candidate, pos, direction) : true;
   }

   private boolean matchesScaffoldRaytrace(Rotation candidate, BlockPos pos, Direction direction) {
      return this.raytrace.get() == Scaffold.RaytraceMode.Normal ? RaytraceUtils.overBlock(candidate, pos) : RaytraceUtils.overBlock(candidate, pos, direction);
   }

   private Rotation getPacketRotation(Rotation target) {
      return this.shouldRandomizeRotation()
         ? RotationManager.INSTANCE.getRotationsWithNoise(target, this.scaffoldRaytrace(this.blockPos, this.direction))
         : target;
   }

   private void setScaffoldRotations(Rotation target, double speed, double backSpeed) {
      if (this.shouldRandomizeRotation()) {
         RotationManager.INSTANCE.setRotationsWithNoise(target, speed, backSpeed, this.scaffoldRaytrace(this.blockPos, this.direction), Priority.High);
      } else {
         RotationManager.INSTANCE.setRotations(target, speed, backSpeed, Priority.High);
      }
   }

   private boolean shouldRandomizeRotation() {
      return this.mode.get() == Scaffold.Mode.GodBridge;
   }

   private boolean isBridgeMode() {
      return this.mode.get() == Scaffold.Mode.Grim;
   }

   private float randomRotationOffset() {
      return this.shouldRandomizeRotation() ? this.random(-0.3F, 0.3F) : 0.0F;
   }

   private boolean onAir() {
      Vec3 baseVec = this.mc.player.getEyePosition();
      BlockPos base = BlockPos.containing(baseVec.x, this.getYLevel(), baseVec.z);
      return this.mc.level.getBlockState(base).canBeReplaced();
   }

   private Vec3 getVec3(BlockPos pos, Direction face) {
      double x = pos.getX() + 0.5;
      double y = pos.getY() + 0.5;
      double z = pos.getZ() + 0.5;
      if (face != Direction.UP && face != Direction.DOWN) {
         y += 0.08;
      } else {
         x += this.random(-0.3F, 0.3F);
         z += this.random(-0.3F, 0.3F);
      }

      if (face == Direction.WEST || face == Direction.EAST) {
         z += this.random(-0.3F, 0.3F);
      }

      if (face == Direction.SOUTH || face == Direction.NORTH) {
         x += this.random(-0.3F, 0.3F);
      }

      return new Vec3(x, y, z);
   }

   private FindItemResult findBlockResult() {
      ItemStack offhandStack = this.mc.player.getOffhandItem();
      if (this.isValidStack(offhandStack)) {
         return new FindItemResult(40, offhandStack.getCount());
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (this.isValidStack(stack)) {
            return new FindItemResult(i, stack.getCount());
         }
      }

      return new FindItemResult(-1, 0);
   }

   private void swap() {
      if (this.blockResult != null && !this.blockResult.isOffhand()) {
         int selectedSlot = this.mc.player.getInventory().getSelectedSlot();
         if (this.blockResult.slot() != selectedSlot) {
            SlotHelper.setCurrentItem(this.blockResult.slot(), true);
            this.shouldSwapBack = true;
         }
      }
   }

   private void swapBack() {
      if (this.blockResult != null && !this.blockResult.isOffhand() && this.shouldSwapBack) {
         SlotHelper.INSTANCE.stop();
         this.shouldSwapBack = false;
      }
   }

   private InteractionHand getPlacementHand() {
      return this.blockResult != null && this.blockResult.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   private boolean isValidStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
         String name = stack.getDisplayName().getString();
         if (name.contains("Click") || name.contains("点击")) {
            return false;
         }

         if (stack.getItem() instanceof StandingAndWallBlockItem) {
            return false;
         }

         Block block = blockItem.getBlock();
         return !(block instanceof FlowerBlock) && !(block instanceof BushBlock) && !(block instanceof NetherFungusBlock) && !(block instanceof CropBlock)
            ? !(block instanceof SlabBlock) && !BLACKLISTED_BLOCKS.contains(block)
            : false;
      } else {
         return false;
      }
   }

   private boolean hasMovementInput(MoveInputEvent event) {
      return event.getForward() != 0.0F || event.getStrafe() != 0.0F;
   }

   private boolean isOnBlockEdge(float inflate) {
      return !this.mc
         .level
         .getCollisions(this.mc.player, this.mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate(-inflate, 0.0, -inflate))
         .iterator()
         .hasNext();
   }

   private double predictY(int ticks) {
      double y = this.mc.player.getY();
      double velocity = this.mc.player.getDeltaMovement().y;

      for (int i = 0; i < ticks; i++) {
         y += velocity;
         velocity = (velocity - 0.08) * 0.98;
      }

      return y;
   }

   private Color withAlpha(Color color, float alphaFactor) {
      return new Color(color.r, color.g, color.b, (int)(color.a * alphaFactor));
   }

   private float easeInOutExpo(float x) {
      if (x <= 0.0F) {
         return 0.0F;
      } else if (x >= 1.0F) {
         return 1.0F;
      } else {
         return x < 0.5F ? (float)Math.pow(2.0, 20.0 * x - 10.0) / 2.0F : (2.0F - (float)Math.pow(2.0, -20.0 * x + 10.0)) / 2.0F;
      }
   }

   private float random(float min, float max) {
      return min + (float)Math.random() * (max - min);
   }

   private enum Mode {
      Grim,
      GodBridge;
   }

   private enum RaytraceMode {
      Normal,
      Strict;
   }

   private record RenderInfo(AABB aabb, Color lineColor, Color sideColor, long startTime, boolean fade, boolean shrink) {
   }

   private enum RotationMode {
      Rise,
      Hypixel;
   }
}
