package meteordevelopment.meteorclient.utils.rotation.ThunderHack;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.RotationAnimationEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class ThunderRotationManager {
   public static final ThunderRotationManager INSTANCE = new ThunderRotationManager();
   private static final Minecraft mc = Minecraft.getInstance();
   private ThunderRotation rotation;
   private ThunderRotation lastRotation;
   private ThunderRotation animationRotation;
   private ThunderRotation lastAnimationRotation;
   private ThunderMoveFixMode moveFix = ThunderMoveFixMode.Off;
   private boolean initialized;
   private boolean active;
   private boolean rotationRequested;

   private ThunderRotationManager() {
   }

   public void init() {
      if (!this.initialized) {
         MeteorClient.EVENT_BUS.subscribe(this);
         this.initialized = true;
      }
   }

   public void setRotation(float yaw, float pitch) {
      this.setRotation(new ThunderRotation(yaw, pitch), ThunderMoveFixMode.Off);
   }

   public void setRotation(float yaw, float pitch, ThunderMoveFixMode moveFix) {
      this.setRotation(new ThunderRotation(yaw, pitch), moveFix);
   }

   public void setRotation(ThunderRotation rotation) {
      this.setRotation(rotation, ThunderMoveFixMode.Off);
   }

   public void setRotation(ThunderRotation rotation, ThunderMoveFixMode moveFix) {
      if (rotation != null && rotation.valid()) {
         this.rotation = rotation.copy();
         this.moveFix = moveFix == null ? ThunderMoveFixMode.Off : moveFix;
         this.active = true;
         this.rotationRequested = true;
      }
   }

   public void setRotation(ThunderRotationSystem system, ThunderMoveFixMode moveFix) {
      if (system != null) {
         this.setRotation(system.rotation(), moveFix);
      }
   }

   public ThunderRotation updateAndSet(ThunderRotationSystem system, Entity target, ThunderRotationSystem.Settings settings) {
      return this.updateAndSet(system, target, settings, this.moveFix);
   }

   public ThunderRotation updateAndSet(ThunderRotationSystem system, Entity target, ThunderRotationSystem.Settings settings, ThunderMoveFixMode moveFix) {
      if (system != null && target != null && settings != null) {
         ThunderRotation next = system.update(target, settings);
         this.setRotation(next, moveFix);
         return next;
      } else {
         this.reset();
         return this.getRotation();
      }
   }

   public void setMoveFix(ThunderMoveFixMode moveFix) {
      this.moveFix = moveFix == null ? ThunderMoveFixMode.Off : moveFix;
   }

   public void reset() {
      this.active = false;
      this.rotationRequested = false;
      this.moveFix = ThunderMoveFixMode.Off;
      this.animationRotation = null;
      this.lastAnimationRotation = null;
      if (mc.player != null) {
         ThunderRotation current = new ThunderRotation(mc.player.getYRot(), mc.player.getXRot());
         this.rotation = current.copy();
         this.lastRotation = current.copy();
      } else {
         this.rotation = null;
         this.lastRotation = null;
      }
   }

   public boolean isActive() {
      return this.active && this.rotation != null && this.rotation.valid();
   }

   public ThunderRotation getRotation() {
      if (this.isActive()) {
         return this.rotation;
      } else {
         return mc.player == null ? new ThunderRotation() : new ThunderRotation(mc.player.getYRot(), mc.player.getXRot());
      }
   }

   public ThunderRotation getLastRotation() {
      if (this.lastRotation != null && this.lastRotation.valid()) {
         return this.lastRotation;
      } else {
         return mc.player == null ? new ThunderRotation() : new ThunderRotation(mc.player.yRotO, mc.player.xRotO);
      }
   }

   public float getYaw() {
      return this.getRotation().yaw();
   }

   public float getPitch() {
      return this.getRotation().pitch();
   }

   public float getFixRotation() {
      return this.isActive() ? this.rotation.yaw() : Float.NaN;
   }

   public ThunderMoveFixMode getMoveFix() {
      return this.moveFix;
   }

   public boolean shouldFixMovementInput() {
      return this.isActive() && this.moveFix == ThunderMoveFixMode.Free && mc.player != null && !mc.player.isPassenger();
   }

   public boolean shouldFixMoveRelative() {
      return this.isActive() && this.moveFix != ThunderMoveFixMode.Off && mc.player != null && !mc.player.isPassenger();
   }

   @EventHandler(priority = -450)
   private void onTick(TickEvent.Pre event) {
      if (mc.player != null && mc.level != null) {
         if (!this.rotationRequested) {
            this.active = false;
            this.moveFix = ThunderMoveFixMode.Off;
         }

         this.rotationRequested = false;
      } else {
         this.reset();
      }
   }

   @EventHandler(priority = -450)
   private void onSendMovementPacketsPre(SendMovementPacketsEvent.Pre event) {
      if (this.isActive()) {
         float yaw = this.rotation.yaw();
         float pitch = Mth.clamp(this.rotation.pitch(), -90.0F, 90.0F);
         if (!Float.isNaN(yaw) && !Float.isNaN(pitch)) {
            event.setYaw(yaw);
            event.setPitch(pitch);
            this.lastRotation = this.rotation.copy();
            this.lastAnimationRotation = this.animationRotation == null ? this.getLastRotation().copy() : this.animationRotation.copy();
            this.animationRotation = new ThunderRotation(yaw, pitch);
         }
      }
   }

   @EventHandler(priority = -450)
   private void onMoveInput(MoveInputEvent event) {
      if (this.shouldFixMovementInput()) {
         ThunderRotationUtil.fixMovement(event, this.rotation.yaw());
      }
   }

   @EventHandler(priority = -450)
   private void onAnimation(RotationAnimationEvent event) {
      if (this.isActive() && this.animationRotation != null) {
         ThunderRotation last = this.lastAnimationRotation == null ? this.getLastRotation() : this.lastAnimationRotation;
         event.setYaw(this.animationRotation.yaw());
         event.setLastYaw(last.yaw());
         event.setPitch(this.animationRotation.pitch());
         event.setLastPitch(last.pitch());
      }
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      this.reset();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.reset();
   }
}
