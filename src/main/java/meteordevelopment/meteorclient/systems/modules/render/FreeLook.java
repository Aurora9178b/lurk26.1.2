package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;

public class FreeLook extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgArrows = this.settings.createGroup("Arrows");
   public final Setting<FreeLook.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Which entity to rotate."))
               .defaultValue(FreeLook.Mode.Player))
            .build()
      );
   public final Setting<Boolean> togglePerspective = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-perspective").description("Changes your perspective on toggle.").defaultValue(true).build());
   public final Setting<Double> sensitivity = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("camera-sensitivity")
            .description("How fast the camera moves in camera mode.")
            .defaultValue(8.0)
            .min(0.0)
            .sliderMax(10.0)
            .build()
      );
   public final Setting<Boolean> arrows = this.sgArrows
      .add(
         new BoolSetting.Builder()
            .name("arrows-control-opposite")
            .description("Allows you to control the other entities rotation with the arrow keys.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Double> arrowSpeed = this.sgArrows
      .add(new DoubleSetting.Builder().name("arrow-speed").description("Rotation speed with arrow keys.").defaultValue(4.0).min(0.0).build());
   public float cameraYaw;
   public float cameraPitch;
   private CameraType prePers;

   public FreeLook() {
      super(Categories.Render, "free-look", "Allows more rotation options in third person.");
   }

   @Override
   public void onActivate() {
      this.cameraYaw = this.mc.player.getYRot();
      this.cameraPitch = this.mc.player.getXRot();
      this.prePers = this.mc.options.getCameraType();
      if (this.prePers != CameraType.THIRD_PERSON_BACK && this.togglePerspective.get()) {
         this.mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      }
   }

   @Override
   public void onDeactivate() {
      if (this.mc.options.getCameraType() != this.prePers && this.togglePerspective.get()) {
         this.mc.options.setCameraType(this.prePers);
      }
   }

   public boolean playerMode() {
      return this.isActive() && this.mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK && this.mode.get() == FreeLook.Mode.Player;
   }

   public boolean cameraMode() {
      return this.isActive() && this.mode.get() == FreeLook.Mode.Camera;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.arrows.get()) {
         for (int i = 0; i < this.arrowSpeed.get() * 2.0; i++) {
            switch ((FreeLook.Mode)this.mode.get()) {
               case Player:
                  if (Input.isKeyPressed(263)) {
                     this.cameraYaw = (float)(this.cameraYaw - 0.5);
                  }

                  if (Input.isKeyPressed(262)) {
                     this.cameraYaw = (float)(this.cameraYaw + 0.5);
                  }

                  if (Input.isKeyPressed(265)) {
                     this.cameraPitch = (float)(this.cameraPitch - 0.5);
                  }

                  if (Input.isKeyPressed(264)) {
                     this.cameraPitch = (float)(this.cameraPitch + 0.5);
                  }
                  break;
               case Camera:
                  float yaw = this.mc.player.getYRot();
                  float pitch = this.mc.player.getXRot();
                  if (Input.isKeyPressed(263)) {
                     yaw = (float)(yaw - 0.5);
                  }

                  if (Input.isKeyPressed(262)) {
                     yaw = (float)(yaw + 0.5);
                  }

                  if (Input.isKeyPressed(265)) {
                     pitch = (float)(pitch - 0.5);
                  }

                  if (Input.isKeyPressed(264)) {
                     pitch = (float)(pitch + 0.5);
                  }

                  this.mc.player.setYRot(yaw);
                  this.mc.player.setXRot(pitch);
            }
         }
      }

      this.mc.player.setXRot(Mth.clamp(this.mc.player.getXRot(), -90.0F, 90.0F));
      this.cameraPitch = Mth.clamp(this.cameraPitch, -90.0F, 90.0F);
   }

   public enum Mode {
      Player,
      Camera;
   }
}
