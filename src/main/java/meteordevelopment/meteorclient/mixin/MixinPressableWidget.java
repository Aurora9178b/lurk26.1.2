package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.ButtonSetting;
import meteordevelopment.meteorclient.utils.render.ButtonRenderQueue;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class MixinPressableWidget extends AbstractWidget {
   @Unique
   private float progress = 0.0F;

   public MixinPressableWidget(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
   }

   @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
   private void onRenderWidget(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ButtonSetting buttonColors = Modules.get().get(ButtonSetting.class);
      if (buttonColors != null && buttonColors.enabled.get()) {
         int x = this.getX();
         int y = this.getY();
         int width = this.getWidth();
         int height = this.getHeight();
         float targetProgress = this.active && this.isHovered() ? 1.0F : 0.0F;
         float speed = 5.0F * buttonColors.animationSpeed.get().floatValue();
         this.progress = AnimationUtility.ease(this.progress, targetProgress, speed);
         SettingColor baseColorSetting = this.active ? buttonColors.normalColor.get() : buttonColors.disabledColor.get();
         SettingColor hoverColorSetting = this.active ? buttonColors.hoverColor.get() : buttonColors.disabledColor.get();
         int baseR = baseColorSetting.r;
         int baseG = baseColorSetting.g;
         int baseB = baseColorSetting.b;
         int baseA = baseColorSetting.a;
         int hoverR = hoverColorSetting.r;
         int hoverG = hoverColorSetting.g;
         int hoverB = hoverColorSetting.b;
         int hoverA = hoverColorSetting.a;
         int animatedR = (int)(baseR + (hoverR - baseR) * this.progress);
         int animatedG = (int)(baseG + (hoverG - baseG) * this.progress);
         int animatedB = (int)(baseB + (hoverB - baseB) * this.progress);
         int animatedA = (int)(baseA + (hoverA - baseA) * this.progress);
         ButtonRenderQueue.add(
            x,
            y,
            width,
            height,
            buttonColors.backgroundRadius.get(),
            buttonColors.backgroundMode.get(),
            buttonColors.enableGlow.get(),
            new Color(animatedR, animatedG, animatedB, animatedA),
            this.getMessage(),
            this.active
         );
         ci.cancel();
      }
   }
}
