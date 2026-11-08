package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixin.CreativeModeInventoryScreenAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTabs;

public class GUIMove extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<GUIMove.Screens> screens = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("guis")).description("Which GUIs to move in."))
               .defaultValue(GUIMove.Screens.Inventory))
            .build()
      );
   public final Setting<Boolean> jump = this.sgGeneral
      .add(new BoolSetting.Builder().name("jump").description("Allows you to jump while in GUIs.").defaultValue(true).onChanged(aBoolean -> {
         if (this.isActive() && !aBoolean) {
            this.mc.options.keyJump.setDown(false);
         }
      }).build());
   public final Setting<Boolean> sneak = this.sgGeneral
      .add(new BoolSetting.Builder().name("sneak").description("Allows you to sneak while in GUIs.").defaultValue(true).onChanged(aBoolean -> {
         if (this.isActive() && !aBoolean) {
            this.mc.options.keyShift.setDown(false);
         }
      }).build());
   public final Setting<Boolean> sprint = this.sgGeneral
      .add(new BoolSetting.Builder().name("sprint").description("Allows you to sprint while in GUIs.").defaultValue(true).onChanged(aBoolean -> {
         if (this.isActive() && !aBoolean) {
            this.mc.options.keySprint.setDown(false);
         }
      }).build());
   private final Setting<Boolean> arrowsRotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("arrows-rotate").description("Allows you to use your arrow keys to rotate while in GUIs.").defaultValue(true).build());
   private final Setting<Double> rotateSpeed = this.sgGeneral
      .add(new DoubleSetting.Builder().name("rotate-speed").description("Rotation speed while in GUIs.").defaultValue(4.0).min(0.0).build());

   public GUIMove() {
      super(Categories.Movement, "gui-move", "Allows you to perform various actions while in GUIs.");
   }

   @Override
   public void onDeactivate() {
      this.mc.options.keyUp.setDown(false);
      this.mc.options.keyDown.setDown(false);
      this.mc.options.keyLeft.setDown(false);
      this.mc.options.keyRight.setDown(false);
      if (this.jump.get()) {
         this.mc.options.keyJump.setDown(false);
      }

      if (this.sneak.get()) {
         this.mc.options.keyShift.setDown(false);
      }

      if (this.sprint.get()) {
         this.mc.options.keySprint.setDown(false);
      }
   }

   public boolean disableSpace() {
      return this.isActive() && this.jump.get() && this.mc.options.keyJump.isDefault();
   }

   public boolean disableArrows() {
      return this.isActive() && this.arrowsRotate.get();
   }

   @EventHandler
   private void onKey(KeyInputEvent event) {
      this.onInput(event.key(), event.action);
   }

   @EventHandler
   private void onButton(MouseClickEvent event) {
      this.onInput(event.button(), event.action);
   }

   private void onInput(int key, KeyAction action) {
      if (!this.skip()) {
         this.pass(this.mc.options.keyUp, key, action);
         this.pass(this.mc.options.keyDown, key, action);
         this.pass(this.mc.options.keyLeft, key, action);
         this.pass(this.mc.options.keyRight, key, action);
         if (this.jump.get()) {
            this.pass(this.mc.options.keyJump, key, action);
         }

         if (this.sneak.get()) {
            this.pass(this.mc.options.keyShift, key, action);
         }

         if (this.sprint.get()) {
            this.pass(this.mc.options.keySprint, key, action);
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (!this.skip()) {
         float rotationDelta = Math.min((float)(this.rotateSpeed.get() * event.frameTime * 20.0), 100.0F);
         Freecam freecam = Modules.get().get(Freecam.class);
         if (this.arrowsRotate.get()) {
            if (!freecam.isActive()) {
               float yaw = this.mc.player.getYRot();
               float pitch = this.mc.player.getXRot();
               if (Input.isKeyPressed(263)) {
                  yaw -= rotationDelta;
               }

               if (Input.isKeyPressed(262)) {
                  yaw += rotationDelta;
               }

               if (Input.isKeyPressed(265)) {
                  pitch -= rotationDelta;
               }

               if (Input.isKeyPressed(264)) {
                  pitch += rotationDelta;
               }

               pitch = Mth.clamp(pitch, -90.0F, 90.0F);
               this.mc.player.setYRot(yaw);
               this.mc.player.setXRot(pitch);
            } else {
               double dy = 0.0;
               double dx = 0.0;
               if (Input.isKeyPressed(263)) {
                  dy = -rotationDelta;
               }

               if (Input.isKeyPressed(262)) {
                  dy = rotationDelta;
               }

               if (Input.isKeyPressed(265)) {
                  dx = -rotationDelta;
               }

               if (Input.isKeyPressed(264)) {
                  dx = rotationDelta;
               }

               freecam.changeLookDirection(dy, dx);
            }
         }
      }
   }

   private void pass(KeyMapping bind, int key, KeyAction action) {
      if (Input.getKey(bind) == key) {
         if (action == KeyAction.Press) {
            bind.setDown(true);
         }

         if (action == KeyAction.Release) {
            bind.setDown(false);
         }
      }
   }

   public boolean skip() {
      if (this.mc.screen == null
         || this.mc.screen instanceof CreativeModeInventoryScreen
            && CreativeModeInventoryScreenAccessor.meteor$getSelectedTab() == CreativeModeTabs.searchTab()
         || this.mc.screen instanceof ChatScreen
         || this.mc.screen instanceof SignEditScreen
         || this.mc.screen instanceof AnvilScreen
         || this.mc.screen instanceof AbstractCommandBlockEditScreen
         || this.mc.screen instanceof StructureBlockEditScreen) {
         return true;
      } else {
         return this.screens.get() == GUIMove.Screens.GUI && !(this.mc.screen instanceof WidgetScreen)
            ? true
            : this.screens.get() == GUIMove.Screens.Inventory && this.mc.screen instanceof WidgetScreen;
      }
   }

   public enum Screens {
      GUI,
      Inventory,
      Both;
   }
}
