package meteordevelopment.meteorclient.systems.modules.movement;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class Blink extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> renderOriginal = this.sgGeneral
      .add(new BoolSetting.Builder().name("render-original").description("Renders your player model at the original position.").defaultValue(true).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("pulse-delay")
            .description("After the duration in ticks has elapsed, send all packets and start blinking again. 0 to disable.")
            .defaultValue(0)
            .min(0)
            .sliderMax(60)
            .build()
      );
   private final Setting<Keybind> cancelBlink = this.sgGeneral
      .add(
         new KeybindSetting.Builder()
            .name("cancel-blink")
            .description("Cancels sending packets and sends you back to your original position.")
            .defaultValue(Keybind.none())
            .action(() -> {
               this.cancelled = true;
               this.disable();
            })
            .build()
      );
   private final List<ServerboundMovePlayerPacket> packets = new ArrayList<>();
   private FakePlayerEntity model;
   private final Vector3d start = new Vector3d();
   private boolean cancelled;
   private boolean sending;
   private int timer = 0;

   public Blink() {
      super(Categories.Movement, "blink", "Allows you to essentially teleport while suspending motion updates.");
      this.runInMainMenu = true;
   }

   @Override
   public void onActivate() {
      if (Utils.canUpdate()) {
         if (this.renderOriginal.get()) {
            this.model = new FakePlayerEntity(this.mc.player, this.mc.player.getGameProfile().name(), 20.0F, true);
            this.model.doNotPush = true;
            this.model.hideWhenInsideCamera = true;
            this.model.noHit = true;
            this.model.spawn();
         }

         Utils.set(this.start, this.mc.player.position());
      }
   }

   @Override
   public void onDeactivate() {
      if (Utils.canUpdate()) {
         this.dumpPackets(!this.cancelled);
         if (this.cancelled) {
            this.mc.player.setPos(this.start.x, this.start.y, this.start.z);
            this.mc.player.setDeltaMovement(Vec3.ZERO);
         }

         this.cancelled = false;
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (Utils.canUpdate()) {
         this.timer++;
         if (this.delay.get() != 0 && this.delay.get() <= this.timer) {
            this.onDeactivate();
            this.onActivate();
         }
      }
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (Utils.canUpdate()) {
         if (!this.sending) {
            if (event.packet instanceof ServerboundMovePlayerPacket p) {
               event.cancel();
               ServerboundMovePlayerPacket prev = this.packets.isEmpty() ? null : this.packets.getLast();
               if (prev == null
                  || p.isOnGround() != prev.isOnGround()
                  || p.getYRot(-1.0F) != prev.getYRot(-1.0F)
                  || p.getXRot(-1.0F) != prev.getXRot(-1.0F)
                  || p.getX(-1.0) != prev.getX(-1.0)
                  || p.getY(-1.0) != prev.getY(-1.0)
                  || p.getZ(-1.0) != prev.getZ(-1.0)) {
                  synchronized (this.packets) {
                     this.packets.add(p);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onJoinGame(GameJoinedEvent event) {
      this.warning("Blink is currently enabled; you won't be able to interact with anything properly until you disable it!");
   }

   @EventHandler
   private void onLeaveGame(GameLeftEvent event) {
      this.onDeactivate();
   }

   @Override
   public String getInfoString() {
      return String.format("%.1f", this.timer / 20.0F);
   }

   private void dumpPackets(boolean send) {
      this.sending = true;
      synchronized (this.packets) {
         if (send) {
            this.packets.forEach(this.mc.player.connection::send);
         }

         this.packets.clear();
      }

      this.sending = false;
      if (this.model != null) {
         this.model.despawn();
         this.model = null;
      }

      this.timer = 0;
   }
}
