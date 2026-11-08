package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.hud.elements.ScoreboardHud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.ClientSetting;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.render.ReGlassBackgroundQueue;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Gui.class)
public abstract class GuiMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Unique
   private double meteor$reglassSlotBlobX = Double.NaN;

   @Shadow
   protected abstract void extractSlot(GuiGraphicsExtractor var1, int var2, int var3, DeltaTracker var4, Player var5, ItemStack var6, int var7);

   @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
   private void onExtractStatusEffectOverlay(CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noPotionIcons()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void onExtractPortalOverlay(GuiGraphicsExtractor graphics, float alpha, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noPortalOverlay()) {
         ci.cancel();
      }
   }

   @ModifyArgs(
      method = "extractCameraOverlays",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V",
         ordinal = 0
      )
   )
   private void onExtractPumpkinOverlay(Args args) {
      if (Modules.get().get(NoRender.class).noPumpkinOverlay()) {
         args.set(2, 0.0F);
      }
   }

   @ModifyArgs(
      method = "extractCameraOverlays",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V",
         ordinal = 1
      )
   )
   private void onExtractPowderedSnowOverlay(Args args) {
      if (Modules.get().get(NoRender.class).noPowderedSnowOverlay()) {
         args.set(2, 0.0F);
      }
   }

   @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
   private void onExtractVignetteOverlay(GuiGraphicsExtractor graphics, Entity camera, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noVignette()) {
         ci.cancel();
      }
   }

   @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   private void onExtractScoreboardSidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noScoreboard() || ScoreboardHud.shouldHideVanilla()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   private void onExtractScoreboardSidebar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noScoreboard() || ScoreboardHud.shouldHideVanilla()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
   private void onExtractSpyglassOverlay(GuiGraphicsExtractor graphics, float scale, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noSpyglassOverlay()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(method = "extractCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
   private boolean alwaysRenderCrosshairInFreecam(boolean firstPerson) {
      return Modules.get().isActive(Freecam.class) || firstPerson;
   }

   @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
   private void meteor$extractReGlassHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (LiquidGlassSetting.isEnabled()) {
         if (this.minecraft.gameMode == null || !this.minecraft.gameMode.isSpectator()) {
            if (!this.minecraft.options.hideGui) {
               Player player = this.getCameraPlayer();
               if (player != null) {
                  ci.cancel();
                  int hotbarWidth = 182;
                  int hotbarHeight = 22;
                  int x = this.minecraft.getWindow().getGuiScaledWidth() / 2 - hotbarWidth / 2;
                  int y = this.minecraft.getWindow().getGuiScaledHeight() - hotbarHeight;
                  ReGlassBackgroundQueue.add(x, y, hotbarWidth, hotbarHeight, 11.0, ClientSetting.getHotbarBackgroundColor());
                  int selectedSlot = player.getInventory().getSelectedSlot();
                  int targetX = x - 1 + selectedSlot * 20;
                  if (Double.isNaN(this.meteor$reglassSlotBlobX)) {
                     this.meteor$reglassSlotBlobX = targetX;
                  }

                  double deltaSeconds = deltaTracker.getRealtimeDeltaTicks() / 20.0;
                  double alpha = 1.0 - Math.exp(-Math.max(0.0, deltaSeconds) / 0.08);
                  alpha = Math.max(0.0, Math.min(1.0, alpha));
                  this.meteor$reglassSlotBlobX = this.meteor$reglassSlotBlobX + (targetX - this.meteor$reglassSlotBlobX) * alpha;
                  int selectedX = (int)Math.round(this.meteor$reglassSlotBlobX) + 1;
                  ReGlassBackgroundQueue.add(selectedX, y, hotbarHeight, hotbarHeight, hotbarHeight * 0.5, ClientSetting.getHotbarSelectedColor());

                  for (int i = 0; i < 9; i++) {
                     int itemX = x + 3 + i * 20;
                     int itemY = y + 3;
                     this.extractSlot(graphics, itemX, itemY, deltaTracker, player, player.getInventory().getItem(i), i + 1);
                  }

                  ItemStack offhandStack = player.getOffhandItem();
                  if (!offhandStack.isEmpty()) {
                     HumanoidArm arm = player.getMainArm().getOpposite();
                     int offhandX = arm == HumanoidArm.LEFT ? x - hotbarHeight - 4 : x + hotbarWidth + 4;
                     ReGlassBackgroundQueue.add(offhandX, y, hotbarHeight, hotbarHeight, hotbarHeight * 0.5, ClientSetting.getHotbarBackgroundColor());
                     this.extractSlot(graphics, offhandX + 3, y + 3, deltaTracker, player, offhandStack, 0);
                  }
               }
            }
         }
      }
   }

   @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
   private void onExtractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noCrosshair()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
   private void onExtractTitle(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noTitle()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
   private void onExtractHeldItemTooltip(GuiGraphicsExtractor graphics, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noHeldItemName()) {
         ci.cancel();
      }
   }

   @Inject(
      method = "onDisconnected",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"),
      cancellable = true
   )
   private void onClear(CallbackInfo ci) {
      if (Modules.get().get(BetterChat.class).keepHistory()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
   private void onExtractNausea(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noNausea()) {
         ci.cancel();
      }
   }

   @Shadow
   private Player getCameraPlayer() {
      throw new AssertionError("Mixin application failed.");
   }
}
