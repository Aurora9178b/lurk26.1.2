package meteordevelopment.meteorclient.systems.modules.world;

import java.util.TimerTask;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class PacketMinePlus extends Module {
   public static PacketMinePlus INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(new BoolSetting.Builder().name("using-pause").description("Pauses mining while using an item.").defaultValue(true).build());
   private final Setting<Boolean> onlyMain = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("only-main")
            .description("Only pauses when using the main hand.")
            .defaultValue(true)
            .visible(this.usingPause::get)
            .build()
      );
   private final Setting<PacketMinePlus.MineSwitchMode> autoSwitch = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("auto-switch"))
                  .description("Automatically switches to the best tool."))
               .defaultValue(PacketMinePlus.MineSwitchMode.Silent))
            .build()
      );
   private final Setting<Integer> range = this.sgGeneral
      .add(new IntSetting.Builder().name("range").description("Mining range.").defaultValue(6).min(0).sliderMax(12).build());
   private final Setting<Integer> maxBreaks = this.sgGeneral
      .add(new IntSetting.Builder().name("try-break-time").description("Maximum repeated break attempts.").defaultValue(6).min(0).sliderMax(10).build());
   private final Setting<Boolean> farCancel = this.sgGeneral
      .add(new BoolSetting.Builder().name("far-cancel").description("Cancels the target when it is too far away.").defaultValue(true).build());
   private final Setting<Boolean> swing = this.sgGeneral
      .add(new BoolSetting.Builder().name("swing-hand").description("Swings the hand when sending mine packets.").defaultValue(true).build());
   private final Setting<Boolean> instantMine = this.sgGeneral
      .add(new BoolSetting.Builder().name("instant-mine").description("Keeps sending stop packets after the block is ready.").defaultValue(true).build());
   private final Setting<Integer> instantDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("instant-delay")
            .description("Delay between instant stop packets in milliseconds.")
            .defaultValue(10)
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Boolean> fastBypass = this.sgGeneral
      .add(new BoolSetting.Builder().name("fast-bypass").description("Sends an additional far start packet.").defaultValue(true).build());
   private final Setting<Boolean> doubleBreak = this.sgGeneral
      .add(new BoolSetting.Builder().name("double-break").description("Allows a secondary packet mine target.").defaultValue(false).build());
   private final Setting<Boolean> checkGround = this.sgGeneral
      .add(new BoolSetting.Builder().name("check-ground").description("Slows progress while airborne.").defaultValue(true).build());
   private final Setting<Boolean> bypassGround = this.sgGeneral
      .add(new BoolSetting.Builder().name("bypass-ground").description("Sends a tiny on-ground position packet before stopping.").defaultValue(false).build());
   private final Setting<Integer> switchDamage = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("switch-damage")
            .description("Progress percentage at which to switch tools.")
            .defaultValue(95)
            .min(0)
            .sliderMax(100)
            .build()
      );
   private final Setting<Integer> switchTime = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("switch-time")
            .description("Time before switching back in milliseconds.")
            .defaultValue(100)
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Integer> mineDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("mine-delay")
            .description("Delay between mining targets in milliseconds.")
            .defaultValue(300)
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Integer> packetDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("packet-delay")
            .description("Delay before the double-break stop packet in milliseconds.")
            .defaultValue(200)
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Double> mineDamage = this.sgGeneral
      .add(new DoubleSetting.Builder().name("damage").description("Mining progress multiplier.").defaultValue(0.8).sliderMax(2.0).build());
   private final Setting<Double> animationExp = this.sgRender
      .add(
         new DoubleSetting.Builder()
            .name("animation-exponent")
            .description("Animation exponent.")
            .defaultValue(3.0)
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .build()
      );
   private final Setting<Boolean> renderProgress = this.sgRender
      .add(new BoolSetting.Builder().name("render-progress").description("Renders progress text.").defaultValue(true).build());
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(
         new ColorSetting.Builder().name("target-color").description("Primary progress text color.").defaultValue(new SettingColor(255, 255, 255, 255)).build()
      );
   private final Setting<SettingColor> secondColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("second-color")
            .description("Secondary progress text color.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode")).description("Render shape mode."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideStartColor = this.color("side-start", new SettingColor(255, 255, 255, 0));
   private final Setting<SettingColor> sideEndColor = this.color("side-end", new SettingColor(255, 255, 255, 50));
   private final Setting<SettingColor> lineStartColor = this.color("line-start", new SettingColor(255, 255, 255, 0));
   private final Setting<SettingColor> lineEndColor = this.color("line-end", new SettingColor(255, 255, 255, 255));
   private final Setting<SettingColor> secondSideStartColor = this.color("second-side-start", new SettingColor(255, 255, 255, 0));
   private final Setting<SettingColor> secondSideEndColor = this.color("second-side-end", new SettingColor(255, 255, 255, 50));
   private final Setting<SettingColor> secondLineStartColor = this.color("second-line-start", new SettingColor(255, 255, 255, 0));
   private final Setting<SettingColor> secondLineEndColor = this.color("second-line-end", new SettingColor(255, 255, 255, 255));
   public static BlockPos selfClickPos;
   public static int maxBreaksCount;
   public static int publicProgress;
   public static int secondPublicProgress;
   public static boolean completed;
   public static BlockPos targetPos;
   public static BlockPos secondPos;
   private static double progress;
   private static double secondProgress;
   private static boolean started;
   private static boolean secondStarted;
   private final Vector3d textPos = new Vector3d();
   private final PacketMinePlus.SimpleTimer switchTimer = new PacketMinePlus.SimpleTimer();
   private final PacketMinePlus.SimpleTimer mineTimer = new PacketMinePlus.SimpleTimer();
   private final PacketMinePlus.SimpleTimer instantTimer = new PacketMinePlus.SimpleTimer();
   private long lastTime;
   private long secondLastTime;
   private boolean hasSwitch;
   private boolean secondHasSwitch;

   public PacketMinePlus() {
      super(Categories.World, "PacketMine+", "Leaves-style packet mine.", "packet-mine-plus", "packetmine+");
      INSTANCE = this;
   }

   @Override
   public void onActivate() {
      maxBreaksCount = 0;
      this.hasSwitch = false;
      this.secondHasSwitch = false;
      this.switchTimer.setMs(999999L);
      this.mineTimer.setMs(999999L);
      this.instantTimer.setMs(999999L);
      targetPos = null;
      secondPos = null;
      started = false;
      secondStarted = false;
      completed = false;
      publicProgress = 0;
      secondPublicProgress = 0;
      progress = 0.0;
      secondProgress = 0.0;
      this.lastTime = System.currentTimeMillis();
      this.secondLastTime = System.currentTimeMillis();
   }

   @Override
   public void onDeactivate() {
      this.stopSwitch();
      targetPos = null;
      secondPos = null;
      selfClickPos = null;
   }

   @EventHandler
   private void onBreak(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundPlayerActionPacket packet && packet.getAction() == Action.STOP_DESTROY_BLOCK) {
         this.mineTimer.reset();
      }
   }

   @EventHandler
   private void onStartBreakingBlock(StartBreakingBlockEvent event) {
      if (BlockUtils.canBreak(event.blockPos)) {
         event.cancel();
         if (this.mineDelay.get() > 0 && !this.mineTimer.passedMs(this.mineDelay.get().intValue()) && completed) {
            this.resetPrimary();
            completed = false;
         } else if (this.mineTimer.passedMs(this.mineDelay.get().intValue())) {
            selfClickPos = event.blockPos;
            this.mine(event.blockPos);
         }
      }
   }

   public void mine(BlockPos pos) {
      maxBreaksCount = 0;
      if (this.doubleBreak.get()) {
         if (targetPos != null && secondPos == null && !targetPos.equals(pos)) {
            if (completed) {
               targetPos = pos;
               this.resetPrimaryState();
               completed = false;
            } else {
               secondPos = targetPos;
               targetPos = pos;
               secondStarted = false;
               secondProgress = 0.0;
               secondPublicProgress = 0;
               started = false;
            }
         } else if (targetPos == null || !targetPos.equals(pos)) {
            targetPos = pos;
            this.resetPrimaryState();
            completed = false;
         }
      } else if (!pos.equals(targetPos)) {
         targetPos = pos;
         this.resetPrimaryState();
         completed = false;
      }

      this.mineTimer.reset();
   }

   @Override
   public String getInfoString() {
      if (targetPos == null) {
         return null;
      }

      double max = this.getMineTicks(targetPos, this.getTool(targetPos));
      return progress >= max * this.mineDamage.get() ? "[100%]" : "[" + publicProgress + "%]";
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.level != null && this.mc.player != null) {
         if (targetPos == null && secondPos == null) {
            selfClickPos = null;
         }

         if (publicProgress >= 100 && !this.instantMine.get()) {
            targetPos = null;
         }

         if (secondPublicProgress >= 100) {
            secondPos = null;
         }

         if (this.switchTimer.passedMs(this.switchTime.get().intValue())
            && (this.hasSwitch || this.secondHasSwitch)
            && this.autoSwitch.get() != PacketMinePlus.MineSwitchMode.None) {
            this.stopSwitch();
         }

         if (maxBreaksCount >= this.maxBreaks.get() * 10) {
            maxBreaksCount = 0;
            targetPos = null;
         }

         this.tickSecond(event);
         this.tickPrimary(event);
         this.renderProgressText();
      }
   }

   private void tickSecond(Render3DEvent event) {
      if (secondPos != null && this.doubleBreak.get()) {
         if (this.farCancel.get() && this.distanceTo(secondPos) > this.range.get().intValue()) {
            secondPos = null;
         } else {
            double secondMax = this.getMineTicks(secondPos, this.getTool(secondPos));
            double delta = this.deltaSeconds(true);
            secondPublicProgress = this.getProgressPercent(secondProgress, secondMax);
            if (!secondStarted) {
               this.sendStart(secondPos);
               secondStarted = true;
               secondProgress = 0.0;
            } else {
               this.updateSecondProgress(delta);
               this.renderSecondAnimation(event);
               if (secondProgress >= secondMax * this.mineDamage.get()) {
                  this.sendStopSecond();
               }

               if ((!this.usingPause.get() || !this.checkPause(this.onlyMain.get()))
                  && (secondPublicProgress >= this.switchDamage.get() || publicProgress >= this.switchDamage.get())
                  && !this.hasSwitch
                  && secondPos != null) {
                  this.switchToBestTool(secondPos, true);
               }
            }
         }
      }
   }

   private void tickPrimary(Render3DEvent event) {
      if (targetPos != null) {
         if (this.farCancel.get() && this.distanceTo(targetPos) > this.range.get().intValue()) {
            targetPos = null;
         } else {
            double max = this.getMineTicks(targetPos, this.getTool(targetPos));
            publicProgress = this.getProgressPercent(progress, max);
            if (progress >= max * this.mineDamage.get() && completed) {
               if (this.isAir(targetPos)) {
                  maxBreaksCount = 0;
               } else if (!this.usingPause.get() || !this.checkPause(this.onlyMain.get())) {
                  maxBreaksCount++;
               }
            }

            if (this.instantMine.get() && completed) {
               event.renderer
                  .box(
                     new AABB(targetPos),
                     this.getColor(this.sideStartColor.get(), this.sideEndColor.get(), 1.0),
                     this.getColor(this.lineStartColor.get(), this.lineEndColor.get(), 1.0),
                     this.shapeMode.get(),
                     0
                  );
               if (!this.isAir(targetPos) && this.instantTimer.passedMs(this.instantDelay.get().intValue())) {
                  this.sendStop();
                  this.instantTimer.reset();
               }
            } else {
               double delta = this.deltaSeconds(false);
               if (!started) {
                  this.sendStart(targetPos);
               } else {
                  this.updatePrimaryProgress(delta);
                  this.renderAnimation(event);
                  if (progress >= max * this.mineDamage.get()) {
                     this.sendStop();
                     completed = true;
                     if (!this.instantMine.get() && secondPos == null) {
                        targetPos = null;
                     }
                  }
               }
            }
         }
      }
   }

   private void sendStart(final BlockPos pos) {
      final Direction direction = BlockUtils.getDirection(pos);
      this.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, direction));
      if (this.fastBypass.get()) {
         BlockPos bypassPos = BlockPos.containing(this.mc.player.getX(), 321.0, this.mc.player.getZ());
         this.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, bypassPos, Direction.DOWN));
      }

      if (this.doubleBreak.get()) {
         long delay = this.packetDelay.get().intValue();
         final java.util.Timer timer = new java.util.Timer("PacketMinePlus-DoubleBreak", true);
         timer.schedule(
            new TimerTask() {
               @Override
               public void run() {
                  PacketMinePlus.this.mc
                     .execute(() -> PacketMinePlus.this.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, pos, direction)));
                  timer.cancel();
               }
            },
            delay
         );
      }

      this.mc.player.swing(InteractionHand.MAIN_HAND);
      if (pos.equals(targetPos)) {
         started = true;
         progress = 0.0;
      } else {
         secondStarted = true;
         secondProgress = 0.0;
      }
   }

   private void sendStop() {
      if (!this.usingPause.get() || !this.checkPause(this.onlyMain.get())) {
         if (!this.doubleBreak.get() || secondPos == null) {
            this.switchToBestTool(targetPos, false);
         }

         if (this.bypassGround.get() && targetPos != null && !this.isAir(targetPos) && !this.mc.player.isFallFlying() && !this.mc.player.onGround()) {
            this.sendPacket(new Pos(this.mc.player.getX(), this.mc.player.getY() + 1.0E-9, this.mc.player.getZ(), true, this.mc.player.horizontalCollision));
         }

         if (this.swing.get()) {
            this.mc.player.swing(InteractionHand.MAIN_HAND);
         }

         this.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, targetPos, BlockUtils.getDirection(targetPos)));
      }
   }

   private void sendStopSecond() {
      if (this.bypassGround.get() && secondPos != null && !this.isAir(secondPos) && !this.mc.player.isFallFlying() && !this.mc.player.onGround()) {
         this.sendPacket(new Pos(this.mc.player.getX(), this.mc.player.getY() + 1.0E-9, this.mc.player.getZ(), true, this.mc.player.horizontalCollision));
      }

      if (secondPos != null && !this.isAir(secondPos)) {
         this.mc.level.setBlock(secondPos, Blocks.AIR.defaultBlockState(), 3);
      }
   }

   private void switchToBestTool(BlockPos pos, boolean second) {
      if (pos != null && this.autoSwitch.get() != PacketMinePlus.MineSwitchMode.None) {
         int bestSlot = this.getTool(pos);
         if (bestSlot != -1) {
            SlotHelper.setCurrentItem(bestSlot, true);
            this.switchTimer.reset();
            if (second) {
               this.secondHasSwitch = true;
            } else {
               this.hasSwitch = true;
            }
         }
      }
   }

   private void stopSwitch() {
      SlotHelper.INSTANCE.stopNextTick();
      this.hasSwitch = false;
      this.secondHasSwitch = false;
   }

   private boolean isAir(BlockPos pos) {
      return pos == null || this.mc.level.isEmptyBlock(pos) || this.mc.level.getBlockState(pos).canBeReplaced();
   }

   private double getMineTicks(BlockPos pos, int slot) {
      if (pos != null && this.mc.level != null && this.mc.player != null) {
         BlockState state = this.mc.level.getBlockState(pos);
         double delta = BlockUtils.getBreakDelta(slot == -1 ? this.mc.player.getInventory().getSelectedSlot() : slot, state);
         return delta <= 0.0 ? Double.MAX_VALUE : 1.0 / delta;
      } else {
         return 20.0;
      }
   }

   private void renderAnimation(Render3DEvent event) {
      double max = this.getMineTicks(targetPos, this.getTool(targetPos));
      double p = this.animationProgress(progress, max);
      double size = p / 2.0;
      AABB box = new AABB(
         targetPos.getX() + 0.5 - size,
         targetPos.getY() + 0.5 - size,
         targetPos.getZ() + 0.5 - size,
         targetPos.getX() + 0.5 + size,
         targetPos.getY() + 0.5 + size,
         targetPos.getZ() + 0.5 + size
      );
      event.renderer
         .box(
            box,
            this.getColor(this.sideStartColor.get(), this.sideEndColor.get(), p),
            this.getColor(this.lineStartColor.get(), this.lineEndColor.get(), p),
            this.shapeMode.get(),
            0
         );
   }

   private void renderSecondAnimation(Render3DEvent event) {
      double max = this.getMineTicks(secondPos, this.getTool(secondPos));
      double p = this.animationProgress(secondProgress, max);
      double size = p / 2.0;
      AABB box = new AABB(
         secondPos.getX() + 0.5 - size,
         secondPos.getY() + 0.5 - size,
         secondPos.getZ() + 0.5 - size,
         secondPos.getX() + 0.5 + size,
         secondPos.getY() + 0.5 + size,
         secondPos.getZ() + 0.5 + size
      );
      event.renderer
         .box(
            box,
            this.getColor(this.secondSideStartColor.get(), this.secondSideEndColor.get(), p),
            this.getColor(this.secondLineStartColor.get(), this.secondLineEndColor.get(), p),
            this.shapeMode.get(),
            0
         );
   }

   private void renderProgressText() {
      if (this.renderProgress.get()) {
         if (targetPos != null) {
            this.renderText(completed ? "Done" : publicProgress + "%", targetPos, this.targetColor.get());
         }

         if (secondPos != null) {
            this.renderText(secondPublicProgress + "%", secondPos, this.secondColor.get());
         }
      }
   }

   private void renderText(String text, BlockPos pos, Color color) {
      this.textPos.set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      if (NametagUtils.to2D(this.textPos, 2.0)) {
         NametagUtils.begin(this.textPos);
         TextRenderer.get().begin(1.0, false, true);
         TextRenderer.get().render(text, -TextRenderer.get().getWidth(text) / 2.0, 0.0, color, true);
         TextRenderer.get().end();
         NametagUtils.end();
      }
   }

   private double animationProgress(double currentProgress, double max) {
      if (!(max <= 0.0) && max != Double.MAX_VALUE) {
         double p = 1.0 - this.clamp(currentProgress / (max * this.mineDamage.get()), 0.0, 1.0);
         p = Math.pow(p, this.animationExp.get());
         return 1.0 - p;
      } else {
         return 0.0;
      }
   }

   private Color getColor(Color start, Color end, double progress) {
      return new Color(
         this.lerp(start.r, end.r, progress), this.lerp(start.g, end.g, progress), this.lerp(start.b, end.b, progress), this.lerp(start.a, end.a, progress)
      );
   }

   private int lerp(double start, double end, double progress) {
      return (int)Math.round(start + (end - start) * progress);
   }

   private int getTool(BlockPos pos) {
      if (pos != null && this.mc.level != null && this.mc.player != null) {
         FindItemResult tool = InvUtils.findFastestTool(this.mc.level.getBlockState(pos));
         return tool.found() ? tool.slot() : -1;
      } else {
         return -1;
      }
   }

   private void sendPacket(Packet<?> packet) {
      if (this.mc.getConnection() != null) {
         this.mc.getConnection().send(packet);
      }
   }

   private boolean checkPause(boolean onlyMain) {
      return this.mc.options.keyUse.isDown() && (!onlyMain || this.mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND);
   }

   private double distanceTo(BlockPos pos) {
      return Math.sqrt(this.mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)));
   }

   private double deltaSeconds(boolean second) {
      long now = System.currentTimeMillis();
      long previous = second ? this.secondLastTime : this.lastTime;
      if (second) {
         this.secondLastTime = now;
      } else {
         this.lastTime = now;
      }

      return (now - previous) / 1000.0;
   }

   private void updatePrimaryProgress(double delta) {
      progress = progress + delta * (this.checkGround.get() && !this.mc.player.onGround() ? 4.0 : 20.0);
   }

   private void updateSecondProgress(double delta) {
      secondProgress = secondProgress + delta * (this.checkGround.get() && !this.mc.player.onGround() ? 4.0 : 20.0);
   }

   private int getProgressPercent(double currentProgress, double max) {
      return !(max <= 0.0) && max != Double.MAX_VALUE ? (int)this.clamp(currentProgress / (max * this.mineDamage.get()) * 100.0, 0.0, 100.0) : 0;
   }

   private double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   private void resetPrimary() {
      targetPos = null;
      this.resetPrimaryState();
   }

   private void resetPrimaryState() {
      publicProgress = 0;
      started = false;
      progress = 0.0;
   }

   private Setting<SettingColor> color(String name, SettingColor defaultValue) {
      return this.sgRender.add(new ColorSetting.Builder().name(name).description(name).defaultValue(defaultValue).build());
   }

   private enum MineSwitchMode {
      Delay,
      Silent,
      None;
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
