package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.game.ChangePerspectiveEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;

public class CameraTweaks extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScrolling = this.settings.createGroup("Scrolling");
   private final Setting<Boolean> clip = this.sgGeneral
      .add(new BoolSetting.Builder().name("clip").description("Allows the camera to clip through blocks.").defaultValue(true).build());
   private final Setting<Double> cameraDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("camera-distance")
            .description("The distance the third person camera is from the player.")
            .defaultValue(4.0)
            .min(0.0)
            .onChanged(value -> this.distance = value)
            .build()
      );
   private final Setting<Boolean> scrollingEnabled = this.sgScrolling
      .add(new BoolSetting.Builder().name("scrolling").description("Allows you to scroll to change camera distance.").defaultValue(true).build());
   private final Setting<Keybind> scrollKeybind = this.sgScrolling
      .add(
         new KeybindSetting.Builder()
            .name("bind")
            .description("Binds camera distance scrolling to a key.")
            .visible(this.scrollingEnabled::get)
            .defaultValue(Keybind.fromKey(342))
            .build()
      );
   private final Setting<Double> scrollSensitivity = this.sgScrolling
      .add(
         new DoubleSetting.Builder()
            .name("sensitivity")
            .description("Sensitivity of the scroll wheel when changing the cameras distance.")
            .visible(this.scrollingEnabled::get)
            .defaultValue(1.0)
            .min(0.01)
            .build()
      );
   public double distance;

   public CameraTweaks() {
      super(Categories.Render, "camera-tweaks", "Allows modification of the third person camera.");
   }

   @Override
   public void onActivate() {
      this.distance = this.cameraDistance.get();
   }

   @EventHandler
   private void onPerspectiveChanged(ChangePerspectiveEvent event) {
      this.distance = this.cameraDistance.get();
   }

   @EventHandler
   private void onMouseScroll(MouseScrollEvent event) {
      if (this.mc.options.getCameraType() != CameraType.FIRST_PERSON
         && this.mc.screen == null
         && this.scrollingEnabled.get()
         && (!this.scrollKeybind.get().isSet() || this.scrollKeybind.get().isPressed())) {
         if (this.scrollSensitivity.get() > 0.0) {
            this.distance = this.distance - event.value * 0.25 * (this.scrollSensitivity.get() * this.distance);
            event.cancel();
         }
      }
   }

   public boolean clip() {
      return this.isActive() && this.clip.get();
   }
}
