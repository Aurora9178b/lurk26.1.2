package meteordevelopment.meteorclient.systems.modules.render;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PopChams extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> onlyOne = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-one").description("Only allow one ghost per player.").defaultValue(false).build());
   private final Setting<Double> renderTime = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("render-time")
            .description("How long the ghost is rendered in seconds.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Double> yModifier = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("y-modifier")
            .description("How much should the Y position of the ghost change per second.")
            .defaultValue(0.75)
            .sliderRange(-4.0, 4.0)
            .build()
      );
   private final Setting<Double> scaleModifier = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("scale-modifier")
            .description("How much should the scale of the ghost change per second.")
            .defaultValue(-0.25)
            .sliderRange(-4.0, 4.0)
            .build()
      );
   private final Setting<Boolean> fadeOut = this.sgGeneral
      .add(new BoolSetting.Builder().name("fade-out").description("Fades out the color.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(255, 255, 255, 25)).build());
   private final Setting<SettingColor> lineColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(255, 255, 255, 127)).build());
   private final List<PopChams.GhostPlayer> ghosts = new ArrayList<>();

   public PopChams() {
      super(Categories.Render, "pop-chams", "Renders a ghost where players pop totem.");
   }

   @Override
   public void onDeactivate() {
      synchronized (this.ghosts) {
         this.ghosts.clear();
      }
   }

   @EventHandler
   private void onReceivePacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundEntityEventPacket p) {
         if (p.getEventId() == 35) {
            Entity entity = p.getEntity(this.mc.level);
            if (entity instanceof Player player && entity != this.mc.player) {
               synchronized (this.ghosts) {
                  if (this.onlyOne.get()) {
                     this.ghosts.removeIf(ghostPlayer -> ghostPlayer.uuid.equals(entity.getUUID()));
                  }

                  this.ghosts.add(new PopChams.GhostPlayer(player));
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      synchronized (this.ghosts) {
         this.ghosts.removeIf(ghostPlayer -> ghostPlayer.render(event));
      }
   }

   private class GhostPlayer extends FakePlayerEntity {
      private final UUID uuid;
      private double timer;
      private double scale = 1.0;

      public GhostPlayer(Player player) {
         super(player, "ghost", 20.0F, false);
         this.uuid = player.getUUID();
      }

      public boolean render(Render3DEvent event) {
         this.timer = this.timer + event.frameTime;
         if (this.timer > PopChams.this.renderTime.get()) {
            return true;
         }

         this.yOld = this.getY();
         ((IVec3)this.position()).meteor$setY(this.getY() + PopChams.this.yModifier.get() * event.frameTime);
         this.scale = this.scale + PopChams.this.scaleModifier.get() * event.frameTime;
         int preSideA = PopChams.this.sideColor.get().a;
         int preLineA = PopChams.this.lineColor.get().a;
         if (PopChams.this.fadeOut.get()) {
            SettingColor var10000 = PopChams.this.sideColor.get();
            var10000.a = (int)(var10000.a * (1.0 - this.timer / PopChams.this.renderTime.get()));
            var10000 = PopChams.this.lineColor.get();
            var10000.a = (int)(var10000.a * (1.0 - this.timer / PopChams.this.renderTime.get()));
         }

         WireframeEntityRenderer.render(event, this, this.scale, PopChams.this.sideColor.get(), PopChams.this.lineColor.get(), PopChams.this.shapeMode.get());
         PopChams.this.sideColor.get().a = preSideA;
         PopChams.this.lineColor.get().a = preLineA;
         return false;
      }
   }
}
