package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.utils.render.ReGlassBackgroundQueue;
import meteordevelopment.meteorclient.utils.tooltip.MeteorTooltipData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
   @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", at = @At("HEAD"), cancellable = true)
   private void meteor$reglassBlitSpriteColor(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color, CallbackInfo ci) {
      if (this.meteor$reglassHandleSprite(sprite, x, y, width, height)) {
         ci.cancel();
      }
   }

   @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", at = @At("HEAD"), cancellable = true)
   private void meteor$reglassBlitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, CallbackInfo ci) {
      if (this.meteor$reglassHandleSprite(sprite, x, y, width, height)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void meteor$reglassBlitSlicedSprite(
      RenderPipeline pipeline, Identifier sprite, int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height, CallbackInfo ci
   ) {
      if (this.meteor$reglassHandleSlicedSprite(sprite, x, y, width, height)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIIII)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void meteor$reglassBlitSlicedSpriteColor(
      RenderPipeline pipeline,
      Identifier sprite,
      int spriteWidth,
      int spriteHeight,
      int u,
      int v,
      int x,
      int y,
      int width,
      int height,
      int color,
      CallbackInfo ci
   ) {
      if (this.meteor$reglassHandleSlicedSprite(sprite, x, y, width, height)) {
         ci.cancel();
      }
   }

   @Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V", at = @At("HEAD"), cancellable = true)
   private void meteor$reglassBlitTexture(
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int sourceWidth,
      int sourceHeight,
      int textureWidth,
      CallbackInfo ci
   ) {
      if (this.meteor$reglassShouldSkipContainerTexture(texture, width, height)) {
         ci.cancel();
      }
   }

   @Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V", at = @At("HEAD"), cancellable = true)
   private void meteor$reglassBlitTextureNoSourceSize(
      RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, CallbackInfo ci
   ) {
      if (this.meteor$reglassShouldSkipContainerTexture(texture, width, height)) {
         ci.cancel();
      }
   }

   @Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIII)V", at = @At("HEAD"), cancellable = true)
   private void meteor$reglassBlitTextureFull(
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int sourceWidth,
      int sourceHeight,
      int textureWidth,
      int textureHeight,
      CallbackInfo ci
   ) {
      if (this.meteor$reglassShouldSkipContainerTexture(texture, width, height)) {
         ci.cancel();
      }
   }

   @Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V", at = @At("HEAD"), cancellable = true)
   private void meteor$reglassBlitTextureFullColor(
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int sourceWidth,
      int sourceHeight,
      int textureWidth,
      int textureHeight,
      int color,
      CallbackInfo ci
   ) {
      if (this.meteor$reglassShouldSkipContainerTexture(texture, width, height)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V",
      at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", shift = Shift.BEFORE)
   )
   private void onDrawTooltip(
      Font font,
      List<Component> texts,
      Optional<TooltipComponent> optionalImage,
      int xo,
      int yo,
      @Nullable Identifier style,
      CallbackInfo ci,
      @Local(name = "components") List<ClientTooltipComponent> components
   ) {
      if (optionalImage.isPresent() && optionalImage.get() instanceof MeteorTooltipData meteorTooltipData) {
         components.add(meteorTooltipData.getComponent());
      }
   }

   @ModifyReceiver(
      method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V",
      at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V")
   )
   private Optional<TooltipComponent> onDrawTooltip_modifyIfPresentReceiver(Optional<TooltipComponent> data, Consumer<TooltipComponent> action) {
      return data.isPresent() && data.get() instanceof MeteorTooltipData ? Optional.empty() : data;
   }

   private boolean meteor$reglassShouldSkipContainerTexture(Identifier texture, int width, int height) {
      if (!LiquidGlassSetting.isEnabled()) {
         return false;
      } else if (width >= 100 && height >= 54) {
         String path = texture.getPath();
         return path.contains("textures/gui/container/")
            || path.contains("textures/gui/recipe_book")
            || path.contains("textures/gui/sprites/container/")
            || path.contains("container/")
            || path.contains("recipe_book");
      } else {
         return false;
      }
   }

   private boolean meteor$reglassHandleSprite(Identifier sprite, int x, int y, int width, int height) {
      if (!LiquidGlassSetting.isEnabled()) {
         return false;
      }

      String path = sprite.getPath();
      if (path.equals("widget/text_field")
         || path.equals("widget/text_field_highlighted")
         || path.equals("container/anvil/text_field")
         || path.equals("container/anvil/text_field_disabled")) {
         boolean highlighted = path.contains("highlighted");
         if (path.startsWith("container/anvil/text_field")) {
            ReGlassBackgroundQueue.add(x - 56, y, 170.0, height, 5.0, highlighted ? ReGlassBackgroundQueue.HOVER_PANEL : ReGlassBackgroundQueue.PANEL);
         } else {
            ReGlassBackgroundQueue.add(x, y, width, height, 5.0, highlighted ? ReGlassBackgroundQueue.HOVER_PANEL : ReGlassBackgroundQueue.PANEL);
         }

         return true;
      } else if (this.meteor$reglassIsScroller(path)) {
         this.meteor$reglassRenderScroller(x, y, width, height, path.contains("disabled"));
         return true;
      } else if (path.equals("container/slot")
         || path.equals("container/horse/chest_slots")
         || path.equals("container/slot/banner")
         || path.equals("container/slot/dye")
         || path.equals("container/slot/banner_pattern")
         || path.equals("container/slot/lapis_lazuli")
         || path.equals("container/slot/brewing_fuel")
         || path.equals("container/slot/potion")
         || path.startsWith("container/beacon/button")
         || path.startsWith("container/loom/pattern")
         || path.startsWith("container/enchanting_table/enchantment_slot")
         || path.startsWith("container/stonecutter/recipe")) {
         return true;
      } else if (path.equals("recipe_book/button")
         || path.equals("recipe_book/button_highlighted")
         || path.equals("recipe_book/tab")
         || path.equals("recipe_book/tab_selected")) {
         this.meteor$reglassRoundSprite(x, y, width, height, path.contains("highlighted") || path.contains("selected"));
         return true;
      } else if (path.startsWith("recipe_book/filter") || path.startsWith("recipe_book/furnace_filter")) {
         ReGlassBackgroundQueue.add(
            x,
            y,
            width,
            height,
            5.0,
            !path.contains("enabled") && !path.contains("highlighted") ? ReGlassBackgroundQueue.PANEL : ReGlassBackgroundQueue.HOVER_PANEL
         );
         return true;
      } else if (path.startsWith("recipe_book/slot_")) {
         int size = Math.max(16, Math.min(width, height) - 4);
         int cx = x + width / 2;
         int cy = y + height / 2;
         boolean craftable = path.contains("craftable") && !path.contains("uncraftable");
         ReGlassBackgroundQueue.add(
            cx - size / 2 - 1, cy - size / 2 - 1, size, size, 5.0, craftable ? ReGlassBackgroundQueue.HOVER_PANEL : ReGlassBackgroundQueue.PANEL
         );
         return true;
      } else if (path.startsWith("container/creative_inventory/tab_")) {
         int size = Math.min(24, Math.min(width, height));
         int cx = x + width / 2;
         int cy = y + height / 2;
         boolean selected = path.contains("_selected_");
         ReGlassBackgroundQueue.add(
            cx - size / 2, cy - size / 2, size, size, size * 0.5, selected ? ReGlassBackgroundQueue.HOVER_PANEL : ReGlassBackgroundQueue.PANEL
         );
         return true;
      } else {
         return false;
      }
   }

   private boolean meteor$reglassHandleSlicedSprite(Identifier sprite, int x, int y, int width, int height) {
      if (!LiquidGlassSetting.isEnabled()) {
         return false;
      } else {
         String path = sprite.getPath();
         if (this.meteor$reglassIsScroller(path)) {
            this.meteor$reglassRenderScroller(x, y, width, height, path.contains("disabled"));
            return true;
         } else {
            return path.equals("container/horse/chest_slots")
               || path.equals("container/brewing_stand/fuel_length")
               || path.equals("container/brewing_stand/brew_progress")
               || path.equals("container/brewing_stand/bubbles");
         }
      }
   }

   private boolean meteor$reglassIsScroller(String path) {
      return path.equals("container/villager/scroller")
         || path.equals("container/villager/scroller_disabled")
         || path.equals("container/creative_inventory/scroller")
         || path.equals("container/creative_inventory/scroller_disabled")
         || path.equals("container/loom/scroller")
         || path.equals("container/loom/scroller_disabled")
         || path.equals("container/stonecutter/scroller")
         || path.equals("container/stonecutter/scroller_disabled");
   }

   private void meteor$reglassRenderScroller(int x, int y, int width, int height, boolean disabled) {
      if (!disabled) {
         int thumbWidth = Math.max(5, Math.min(width, 8));
         int thumbHeight = Math.max(12, height);
         int cx = x + width / 2;
         ReGlassBackgroundQueue.add(cx - thumbWidth / 2, y, thumbWidth, thumbHeight, thumbWidth * 0.5, ReGlassBackgroundQueue.HOVER_PANEL);
      }
   }

   private void meteor$reglassRoundSprite(int x, int y, int width, int height, boolean highlighted) {
      int size = Math.min(width, height);
      if (size > 18) {
         size -= 2;
      }

      int cx = x + width / 2;
      int cy = y + height / 2;
      ReGlassBackgroundQueue.add(
         cx - size / 2, cy - size / 2, size, size, size * 0.5, highlighted ? ReGlassBackgroundQueue.HOVER_PANEL : ReGlassBackgroundQueue.PANEL
      );
   }
}
