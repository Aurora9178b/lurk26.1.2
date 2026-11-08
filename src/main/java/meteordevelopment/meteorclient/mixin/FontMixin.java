package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.renderer.text.MeteorFontGlyph;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.ClientSetting;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontMixin {
   @Inject(method = "getGlyph", at = @At("HEAD"), cancellable = true)
   private void onGetGlyph(int codepoint, Style style, CallbackInfoReturnable<BakedGlyph> cir) {
      if (ClientSetting.shouldReplaceMinecraftFont()) {
         MeteorFontGlyph glyph = MeteorFontGlyph.create(codepoint);
         if (glyph != null) {
            cir.setReturnValue(glyph);
         }
      }
   }

   @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
   private void onWidthString(String text, CallbackInfoReturnable<Integer> cir) {
      if (ClientSetting.shouldReplaceMinecraftFont()) {
         Float width = MeteorFontGlyph.width(text);
         if (width != null) {
            cir.setReturnValue(Mth.ceil(width));
         }
      }
   }

   @Inject(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), cancellable = true)
   private void onWidthFormattedCharSequence(FormattedCharSequence text, CallbackInfoReturnable<Integer> cir) {
      if (ClientSetting.shouldReplaceMinecraftFont()) {
         Float width = MeteorFontGlyph.width(text);
         if (width != null) {
            cir.setReturnValue(Mth.ceil(width));
         }
      }
   }

   @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true)
   private void onWidthFormattedText(FormattedText text, CallbackInfoReturnable<Integer> cir) {
      if (ClientSetting.shouldReplaceMinecraftFont()) {
         Float width = MeteorFontGlyph.width(text);
         if (width != null) {
            cir.setReturnValue(Mth.ceil(width));
         }
      }
   }

   @ModifyExpressionValue(method = "getGlyph", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;isObfuscated()Z"))
   private boolean onRenderObfuscatedStyle(boolean original) {
      return Modules.get() != null && Modules.get().get(NoRender.class) != null ? !Modules.get().get(NoRender.class).noObfuscation() && original : original;
   }
}
