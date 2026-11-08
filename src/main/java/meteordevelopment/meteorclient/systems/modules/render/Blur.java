package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.RenderAfterWorldEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class Blur extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScreens = this.settings.createGroup("Screens");
   private final Setting<Integer> strength = this.sgGeneral
      .add(new IntSetting.Builder().name("strength").description("How strong the blur should be.").defaultValue(5).min(1).max(20).sliderRange(1, 20).build());
   private final Setting<Integer> fadeTime = this.sgGeneral
      .add(
         new IntSetting.Builder().name("fade-time").description("How long the fade will last in milliseconds.").defaultValue(100).min(0).sliderMax(500).build()
      );
   private final Setting<Boolean> meteor = this.sgScreens
      .add(new BoolSetting.Builder().name("meteor").description("Applies blur to Meteor screens.").defaultValue(true).build());
   private final Setting<Boolean> inventories = this.sgScreens
      .add(new BoolSetting.Builder().name("inventories").description("Applies blur to inventory screens.").defaultValue(true).build());
   private final Setting<Boolean> chat = this.sgScreens
      .add(new BoolSetting.Builder().name("chat").description("Applies blur when in chat.").defaultValue(false).build());
   private final Setting<Boolean> other = this.sgScreens
      .add(new BoolSetting.Builder().name("other").description("Applies blur to all other screen types.").defaultValue(true).build());
   private boolean enabled;
   private long fadeEndAt = -1L;

   public Blur() {
      super(Categories.Render, "blur", "Blurs background when in GUI screens.");
      MeteorClient.EVENT_BUS.subscribe(new ConsumerListener(RenderAfterWorldEvent.class, event -> this.onRenderAfterWorld()));
   }

   private void onRenderAfterWorld() {
      boolean shouldRender = this.shouldRender();
      long time = System.currentTimeMillis();
      if (this.enabled) {
         if (!shouldRender) {
            if (this.fadeEndAt == -1L) {
               this.fadeEndAt = time + this.fadeTime.get().intValue();
            }

            if (time >= this.fadeEndAt) {
               this.enabled = false;
               this.fadeEndAt = -1L;
            }
         }
      } else if (shouldRender) {
         this.enabled = true;
         this.fadeEndAt = time + this.fadeTime.get().intValue();
      }

      if (this.enabled) {
         double progress = 1.0;
         int fade = this.fadeTime.get();
         if (fade <= 0 || time >= this.fadeEndAt) {
            this.fadeEndAt = -1L;
         } else if (shouldRender) {
            progress = 1.0 - (double)(this.fadeEndAt - time) / fade;
         } else {
            progress = (double)(this.fadeEndAt - time) / fade;
         }

         float blurStrength = (float)(this.strength.get().intValue() * Math.max(0.0, Math.min(1.0, progress)));
         BlurUtil.beginFrame();
         BlurUtil.render(0, 0, this.mc.getWindow().getWidth(), this.mc.getWindow().getHeight(), 0.0F, blurStrength);
      }
   }

   private boolean shouldRender() {
      if (!this.isActive()) {
         return false;
      } else {
         Screen screen = this.mc.screen;
         if (screen instanceof WidgetScreen) {
            return this.meteor.get();
         } else if (screen instanceof AbstractContainerScreen) {
            return this.inventories.get();
         } else if (screen instanceof ChatScreen) {
            return this.chat.get();
         } else {
            return screen != null ? this.other.get() : false;
         }
      }
   }
}
