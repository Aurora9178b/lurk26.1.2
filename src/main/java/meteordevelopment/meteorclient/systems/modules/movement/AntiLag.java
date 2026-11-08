package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.world.phys.Vec3;

public class AntiLag extends Module {
   private static final AntiLag INSTANCE = new AntiLag();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AntiLag.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("AntiLag teleport mode."))
               .defaultValue(AntiLag.Mode.Normal))
            .build()
      );
   private final Setting<Boolean> debug = this.sgGeneral
      .add(new BoolSetting.Builder().name("debug").description("Print debug information").defaultValue(false).build());
   public final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("Max range").defaultValue(200.0).min(0.0).sliderRange(0.0, 256.0).build());

   public static AntiLag getInstance() {
      return INSTANCE;
   }

   public AntiLag() {
      super(Categories.Movement, "AntiLag", "AntiLag");
   }

   private void doTeleport(Vec3 from, Vec3 to) {
      switch ((AntiLag.Mode)this.mode.get()) {
         case Normal:
            this.doNormalTeleport(from, to);
            break;
         case Gcore:
            this.doGcoreTeleport(from, to);
      }
   }

   private void doNormalTeleport(Vec3 from, Vec3 to) {
      TpSetting setting = TpSetting.get();
      double moveDist = this.getMoveDistance();
      if (setting != null && setting.isLTPUtils()) {
         LTpUtils.doTp(from, to, moveDist, false);
      } else {
         QTPUtils.doTp(from, to, moveDist);
      }
   }

   private void doGcoreTeleport(Vec3 from, Vec3 to) {
      double maxDist = this.getMoveDistance();
      if (!(maxDist <= 0.0)) {
         double distance = from.distanceTo(to);
         if (!(distance <= 0.001)) {
            int steps = (int)Math.ceil(distance / maxDist);
            if (steps <= 0) {
               steps = 1;
            }

            for (int i = 0; i < steps; i++) {
               double progress = (i + 1.0) / steps;
               Vec3 stepPos = from.lerp(to, progress);
               LTpUtils.sendMovePacket(stepPos, false);
            }

            LTpUtils.sendMovePacket(to, false);
         }
      }
   }

   private double getMoveDistance() {
      TpSetting setting = TpSetting.get();
      return setting != null ? setting.getMoveDistance() : 128.0;
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundPlayerPositionPacket p) {
         if (!QTPUtils.allowSendP()) {
            return;
         }

         Vec3 packetPos = p.change().position();
         Vec3 playerPos = this.mc.player.position();
         double dis = packetPos.distanceTo(playerPos);
         if (dis >= this.range.get()) {
            return;
         }

         event.cancel();
         this.mc.getConnection().send(new ServerboundAcceptTeleportationPacket(p.id()));
         if (this.debug.get()) {
            ChatUtils.info("LagBack " + packetPos);
         }

         this.doTeleport(packetPos, playerPos);
      }
   }

   public enum Mode {
      Normal,
      Gcore;
   }
}
