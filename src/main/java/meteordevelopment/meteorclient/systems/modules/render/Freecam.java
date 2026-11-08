package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class Freecam extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPathing = this.settings.createGroup("Pathing");
   private final Setting<Double> speed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("speed")
            .description("Your speed while in freecam.")
            .onChanged(aDouble -> this.speedValue = aDouble)
            .defaultValue(1.0)
            .min(0.0)
            .build()
      );
   private final Setting<Double> speedScrollSensitivity = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("speed-scroll-sensitivity")
            .description("Allows you to change speed value using scroll wheel. 0 to disable.")
            .defaultValue(0.0)
            .min(0.0)
            .sliderMax(2.0)
            .build()
      );
   private final Setting<Boolean> staySneaking = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("stay-sneaking")
            .description("If you are sneaking when you enter freecam, whether your player should remain sneaking.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> toggleOnDamage = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-on-damage").description("Disables freecam when you take damage.").defaultValue(false).build());
   private final Setting<Boolean> toggleOnDeath = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-on-death").description("Disables freecam when you die.").defaultValue(false).build());
   private final Setting<Boolean> toggleOnLog = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-on-log").description("Disables freecam when you disconnect from a server.").defaultValue(true).build());
   private final Setting<Boolean> reloadChunks = this.sgGeneral
      .add(new BoolSetting.Builder().name("reload-chunks").description("Disables cave culling.").defaultValue(true).build());
   private final Setting<Boolean> renderHands = this.sgGeneral
      .add(new BoolSetting.Builder().name("show-hands").description("Whether or not to render your hands in freecam.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates to the block or entity you are looking at.").defaultValue(false).build());
   private final Setting<Boolean> staticView = this.sgGeneral
      .add(new BoolSetting.Builder().name("static").description("Disables settings that move the view.").defaultValue(true).build());
   private final Setting<Boolean> baritoneClick = this.sgPathing
      .add(new BoolSetting.Builder().name("click-to-path").description("Sets a pathfinding goal to any block/entity you click at.").defaultValue(false).build());
   private final Setting<Boolean> requireDoubleClick = this.sgPathing
      .add(new BoolSetting.Builder().name("double-click").description("Require two clicks to start pathing.").defaultValue(false).build());
   public final Vector3d pos = new Vector3d();
   public final Vector3d prevPos = new Vector3d();
   private CameraType perspective;
   private double speedValue;
   public float yaw;
   public float pitch;
   public float lastYaw;
   public float lastPitch;
   private double fovScale;
   private boolean bobView;
   private boolean forward;
   private boolean backward;
   private boolean right;
   private boolean left;
   private boolean up;
   private boolean down;
   private boolean isSneaking;
   private long clickTs = 0L;

   public Freecam() {
      super(Categories.Render, "freecam", "Allows the camera to move away from the player.");
   }

   @Override
   public void onActivate() {
      this.fovScale = this.mc.options.fovEffectScale().get();
      this.bobView = this.mc.options.bobView().get();
      if (this.staticView.get()) {
         this.mc.options.fovEffectScale().set(0.0);
         this.mc.options.bobView().set(false);
      }

      this.yaw = this.mc.player.getYRot();
      this.pitch = this.mc.player.getXRot();
      this.perspective = this.mc.options.getCameraType();
      this.speedValue = this.speed.get();
      Utils.set(this.pos, this.mc.gameRenderer.getMainCamera().position());
      Utils.set(this.prevPos, this.mc.gameRenderer.getMainCamera().position());
      if (this.mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
         this.yaw += 180.0F;
         this.pitch *= -1.0F;
      }

      this.lastYaw = this.yaw;
      this.lastPitch = this.pitch;
      this.isSneaking = this.mc.options.keyShift.isDown();
      this.forward = Input.isPressed(this.mc.options.keyUp);
      this.backward = Input.isPressed(this.mc.options.keyDown);
      this.right = Input.isPressed(this.mc.options.keyRight);
      this.left = Input.isPressed(this.mc.options.keyLeft);
      this.up = Input.isPressed(this.mc.options.keyJump);
      this.down = Input.isPressed(this.mc.options.keyShift);
      this.unpress();
      if (this.reloadChunks.get()) {
         this.mc.levelRenderer.allChanged();
      }
   }

   @Override
   public void onDeactivate() {
      if (this.reloadChunks.get()) {
         this.mc.execute(this.mc.levelRenderer::allChanged);
      }

      this.mc.options.setCameraType(this.perspective);
      if (this.staticView.get()) {
         this.mc.options.fovEffectScale().set(this.fovScale);
         this.mc.options.bobView().set(this.bobView);
      }

      this.isSneaking = false;
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      this.unpress();
      this.prevPos.set(this.pos);
      this.lastYaw = this.yaw;
      this.lastPitch = this.pitch;
   }

   private void unpress() {
      this.mc.options.keyUp.setDown(false);
      this.mc.options.keyDown.setDown(false);
      this.mc.options.keyRight.setDown(false);
      this.mc.options.keyLeft.setDown(false);
      this.mc.options.keyJump.setDown(false);
      this.mc.options.keyShift.setDown(false);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.getCameraEntity().isInWall()) {
         this.mc.getCameraEntity().noPhysics = true;
      }

      if (!this.perspective.isFirstPerson()) {
         this.mc.options.setCameraType(CameraType.FIRST_PERSON);
      }

      Vec3 forward = Vec3.directionFromRotation(0.0F, this.yaw);
      Vec3 right = Vec3.directionFromRotation(0.0F, this.yaw + 90.0F);
      double velX = 0.0;
      double velY = 0.0;
      double velZ = 0.0;
      if (this.rotate.get()) {
         if (this.mc.hitResult instanceof EntityHitResult ehr) {
            BlockPos crossHairPos = ehr.getEntity().blockPosition();
            Rotations.rotate(Rotations.getYaw(crossHairPos), Rotations.getPitch(crossHairPos), 0, null);
         } else {
            Vec3 crossHairPosition = this.mc.hitResult.getLocation();
            BlockPos crossHairPos = ((BlockHitResult)this.mc.hitResult).getBlockPos();
            if (!this.mc.level.getBlockState(crossHairPos).isAir()) {
               Rotations.rotate(Rotations.getYaw(crossHairPosition), Rotations.getPitch(crossHairPosition), 0, null);
            }
         }
      }

      double s = 0.5;
      if (Input.isPressed(this.mc.options.keySprint)) {
         s = 1.0;
      }

      boolean a = false;
      if (this.forward) {
         velX += forward.x * s * this.speedValue;
         velZ += forward.z * s * this.speedValue;
         a = true;
      }

      if (this.backward) {
         velX -= forward.x * s * this.speedValue;
         velZ -= forward.z * s * this.speedValue;
         a = true;
      }

      boolean b = false;
      if (this.right) {
         velX += right.x * s * this.speedValue;
         velZ += right.z * s * this.speedValue;
         b = true;
      }

      if (this.left) {
         velX -= right.x * s * this.speedValue;
         velZ -= right.z * s * this.speedValue;
         b = true;
      }

      if (a && b) {
         double diagonal = 1.0 / Math.sqrt(2.0);
         velX *= diagonal;
         velZ *= diagonal;
      }

      if (this.up) {
         velY += s * this.speedValue;
      }

      if (this.down) {
         velY -= s * this.speedValue;
      }

      this.prevPos.set(this.pos);
      this.pos.set(this.pos.x + velX, this.pos.y + velY, this.pos.z + velZ);
   }

   @EventHandler(priority = 100)
   public void onKey(KeyInputEvent event) {
      if (!Input.isKeyPressed(292)) {
         if (!this.checkGuiMove()) {
            if (this.onInput(event.key(), event.action)) {
               event.cancel();
            }
         }
      }
   }

   @Nullable
   private BlockPos rayCastEntity(Vec3 posVec, Vec3 max, short maxDist) {
      EntityHitResult res = ProjectileUtil.getEntityHitResult(
         this.mc.player,
         posVec,
         max,
         AABB.encapsulatingFullBlocks(BlockPos.containing(posVec.x, posVec.y, posVec.z), BlockPos.containing(max.x, max.y, max.z)),
         entity -> true,
         maxDist
      );
      if (res == null) {
         return null;
      }

      Vec3 vec = res.getLocation();
      return BlockPos.containing(vec.x, vec.y, vec.z);
   }

   @Nullable
   private BlockPos rayCastBlock(Vec3 posVec, Vec3 max) {
      ClipContext ctx = new ClipContext(posVec, max, Block.VISUAL, Fluid.SOURCE_ONLY, CollisionContext.empty());
      BlockHitResult res = this.mc.level.clip(ctx);
      return res.getType() == Type.MISS ? null : res.getBlockPos().offset(res.getDirection().getUnitVec3i());
   }

   private void setGoal() {
      long prevClick = this.clickTs;
      this.clickTs = System.currentTimeMillis();
      if (!this.requireDoubleClick.get() || this.clickTs - prevClick <= 500L) {
         Camera cam = this.mc.gameRenderer.getMainCamera();
         Vec3 posVec = cam.position();
         Vec3 lookVec = Vec3.directionFromRotation(cam.xRot(), cam.yRot());
         short maxDist = 256;
         Vec3 max = posVec.add(lookVec.scale(maxDist));
         BlockPos pos = this.rayCastEntity(posVec, max, maxDist);
         if (pos == null) {
            pos = this.rayCastBlock(posVec, max);
         }

         if (pos != null) {
            PathManagers.get().moveTo(pos);
         }
      }
   }

   @EventHandler(priority = 100)
   private void onMouseClick(MouseClickEvent event) {
      if (!this.checkGuiMove()) {
         if (this.baritoneClick.get() && event.action == KeyAction.Press && this.mc.options.keyAttack.matchesMouse(event.click)) {
            this.setGoal();
         }

         if (this.onInput(event.button(), event.action)) {
            event.cancel();
         }
      }
   }

   private boolean onInput(int key, KeyAction action) {
      if (Input.getKey(this.mc.options.keyUp) == key) {
         this.forward = action != KeyAction.Release;
         this.mc.options.keyUp.setDown(false);
      } else if (Input.getKey(this.mc.options.keyDown) == key) {
         this.backward = action != KeyAction.Release;
         this.mc.options.keyDown.setDown(false);
      } else if (Input.getKey(this.mc.options.keyRight) == key) {
         this.right = action != KeyAction.Release;
         this.mc.options.keyRight.setDown(false);
      } else if (Input.getKey(this.mc.options.keyLeft) == key) {
         this.left = action != KeyAction.Release;
         this.mc.options.keyLeft.setDown(false);
      } else if (Input.getKey(this.mc.options.keyJump) == key) {
         this.up = action != KeyAction.Release;
         this.mc.options.keyJump.setDown(false);
      } else {
         if (Input.getKey(this.mc.options.keyShift) != key) {
            return false;
         }

         this.down = action != KeyAction.Release;
         this.mc.options.keyShift.setDown(false);
      }

      return true;
   }

   @EventHandler(priority = -100)
   private void onMouseScroll(MouseScrollEvent event) {
      if (this.speedScrollSensitivity.get() > 0.0 && this.mc.screen == null) {
         this.speedValue = this.speedValue + event.value * 0.25 * (this.speedScrollSensitivity.get() * this.speedValue);
         if (this.speedValue < 0.1) {
            this.speedValue = 0.1;
         }

         event.cancel();
      }
   }

   @EventHandler
   private void onChunkOcclusion(ChunkOcclusionEvent event) {
      event.cancel();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      if (this.toggleOnLog.get()) {
         this.toggle();
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundPlayerCombatKillPacket packet) {
         Entity entity = this.mc.level.getEntity(packet.playerId());
         if (entity == this.mc.player && this.toggleOnDeath.get()) {
            this.toggle();
            this.info("Toggled off because you died.");
         }
      } else if (event.packet instanceof ClientboundSetHealthPacket packet) {
         if (this.mc.player.getHealth() - packet.getHealth() > 0.0F && this.toggleOnDamage.get()) {
            this.toggle();
            this.info("Toggled off because you took damage.");
         }
      } else if (event.packet instanceof ClientboundRespawnPacket && this.isActive()) {
         this.toggle();
         this.info("Toggled off because you changed dimensions.");
      }
   }

   private boolean checkGuiMove() {
      GUIMove guiMove = Modules.get().get(GUIMove.class);
      return this.mc.screen != null && !guiMove.isActive() ? true : this.mc.screen != null && guiMove.isActive() && guiMove.skip();
   }

   public void changeLookDirection(double deltaX, double deltaY) {
      this.lastYaw = this.yaw;
      this.lastPitch = this.pitch;
      this.yaw += (float)deltaX;
      this.pitch += (float)deltaY;
      this.pitch = Mth.clamp(this.pitch, -90.0F, 90.0F);
   }

   public boolean renderHands() {
      return !this.isActive() || this.renderHands.get();
   }

   public boolean staySneaking() {
      return this.isActive() && !this.mc.player.getAbilities().flying && this.staySneaking.get() && this.isSneaking;
   }

   public double getX(float tickDelta) {
      return Mth.lerp(tickDelta, this.prevPos.x, this.pos.x);
   }

   public double getY(float tickDelta) {
      return Mth.lerp(tickDelta, this.prevPos.y, this.pos.y);
   }

   public double getZ(float tickDelta) {
      return Mth.lerp(tickDelta, this.prevPos.z, this.pos.z);
   }

   public double getYaw(float tickDelta) {
      return Mth.lerp(tickDelta, this.lastYaw, this.yaw);
   }

   public double getPitch(float tickDelta) {
      return Mth.lerp(tickDelta, this.lastPitch, this.pitch);
   }
}
