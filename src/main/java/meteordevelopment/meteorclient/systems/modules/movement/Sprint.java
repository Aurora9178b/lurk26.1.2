package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

public class Sprint extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Sprint.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("sprint-mode"))
                  .description("What mode of sprinting."))
               .defaultValue(Sprint.Mode.Strict))
            .build()
      );
   private final Setting<Boolean> keepSprint = this.sgGeneral
      .add(new BoolSetting.Builder().name("keep-sprint").description("Whether to keep sprinting after attacking.").defaultValue(false).build());
   private final Setting<Boolean> unsprintOnHit = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("unsprint-on-hit")
            .description("Whether to stop sprinting before attacking, to ensure you get crits and sweep attacks.")
            .defaultValue(false)
            .build()
      );
   public final Setting<Boolean> unsprintInWater = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("unsprint-in-water")
            .description("Whether to stop sprinting when in water.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == Sprint.Mode.Rage)
            .build()
      );
   private final Setting<Boolean> permaSprint = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("sprint-while-stationary")
            .description("Sprint even when not moving.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == Sprint.Mode.Rage)
            .build()
      );

   public Sprint() {
      super(Categories.Movement, "sprint", "Automatically sprints.");
   }

   @EventHandler(priority = 100)
   private void onTickMovement(TickEvent.Post event) {
      if (!this.unsprintInWater.get() || !this.mc.player.isInWater()) {
         this.mc.player.setSprinting(this.shouldSprint());
      }
   }

   @EventHandler(priority = 100)
   private void onPacketSend(PacketEvent.Send event) {
      if (this.unsprintOnHit.get()) {
         if (event.packet instanceof ServerboundAttackPacket) {
            this.mc.getConnection().send(new ServerboundPlayerCommandPacket(this.mc.player, Action.STOP_SPRINTING));
            this.mc.player.setSprinting(false);
         }
      }
   }

   @EventHandler
   private void onPacketSent(PacketEvent.Sent event) {
      if (this.unsprintOnHit.get() && this.keepSprint.get()) {
         if (event.packet instanceof ServerboundAttackPacket) {
            if (this.shouldSprint() && !this.mc.player.isSprinting()) {
               this.mc.getConnection().send(new ServerboundPlayerCommandPacket(this.mc.player, Action.START_SPRINTING));
               this.mc.player.setSprinting(true);
            }
         }
      }
   }

   public boolean shouldSprint() {
      if (this.mc.screen != null && !Modules.get().get(GUIMove.class).sprint.get()) {
         return false;
      } else {
         float movement = this.mode.get() == Sprint.Mode.Rage ? Math.abs(this.mc.player.zza) + Math.abs(this.mc.player.xxa) : this.mc.player.zza;
         if (!(movement <= (this.mc.player.isUnderWater() ? 1.0E-5F : 0.8)) || this.mode.get() != Sprint.Mode.Strict && this.permaSprint.get()) {
            boolean strictSprint = !this.mc.player.isInShallowWater() && !this.mc.player.isMobilityRestricted() && this.mc.player.isPassenger()
               ? this.mc.player.getVehicle().canSprint() && this.mc.player.getVehicle().isLocalInstanceAuthoritative()
               : this.mc.player.getFoodData().hasEnoughFood() && (!this.mc.player.horizontalCollision || this.mc.player.minorHorizontalCollision);
            return this.isActive() && (this.mode.get() == Sprint.Mode.Rage || strictSprint);
         } else {
            return false;
         }
      }
   }

   public boolean rageSprint() {
      return this.isActive() && this.mode.get() == Sprint.Mode.Rage;
   }

   public boolean unsprintInWater() {
      return this.isActive() && this.unsprintInWater.get();
   }

   public boolean stopSprinting() {
      return !this.isActive() || !this.keepSprint.get();
   }

   public enum Mode {
      Strict,
      Rage;
   }
}
