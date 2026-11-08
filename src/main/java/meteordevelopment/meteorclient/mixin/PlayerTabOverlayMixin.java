package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import java.util.List;
import meteordevelopment.meteorclient.systems.hud.elements.DynamicIslandHud;
import meteordevelopment.meteorclient.systems.hud.elements.DynamicIslandV2;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
   @Shadow
   protected abstract List<PlayerInfo> getPlayerInfos();

   @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
   private void hideVanillaTabListForDynamicIsland(
      GuiGraphicsExtractor graphics, int scaledWindowWidth, Scoreboard scoreboard, Objective objective, CallbackInfo ci
   ) {
      if (DynamicIslandHud.shouldHideVanillaTabList() || DynamicIslandV2.shouldHideVanillaTabList()) {
         ci.cancel();
      }
   }

   @ModifyConstant(constant = @Constant(longValue = 80L), method = "getPlayerInfos")
   private long modifyCount(long count) {
      BetterTab module = Modules.get().get(BetterTab.class);
      return module.isActive() ? module.tabSize.get().intValue() : count;
   }

   @Inject(method = "getNameForDisplay", at = @At("HEAD"), cancellable = true)
   public void getNameForDisplay(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
      BetterTab betterTab = Modules.get().get(BetterTab.class);
      if (betterTab.isActive()) {
         cir.setReturnValue(betterTab.getPlayerName(info));
      }
   }

   @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
   private int modifyWidth(int width) {
      BetterTab module = Modules.get().get(BetterTab.class);
      return module.isActive() && module.accurateLatency.get() ? width + 30 : width;
   }

   @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = Shift.BEFORE))
   private void modifyHeight(CallbackInfo ci, @Local(name = "rows") LocalIntRef rows, @Local(name = "cols") LocalIntRef cols) {
      BetterTab module = Modules.get().get(BetterTab.class);
      if (module.isActive()) {
         int newCols = 1;

         int newRows;
         for (int totalPlayers = newRows = this.getPlayerInfos().size(); newRows > module.tabHeight.get(); newRows = (totalPlayers + newCols - 1) / newCols) {
            newCols++;
         }

         rows.set(newRows);
         cols.set(newCols);
      }
   }

   @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
   private void onExtractPingIcon(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info, CallbackInfo ci) {
      BetterTab betterTab = Modules.get().get(BetterTab.class);
      if (betterTab.isActive() && betterTab.accurateLatency.get()) {
         Minecraft mc = Minecraft.getInstance();
         Font font = mc.font;
         int latency = Mth.clamp(info.getLatency(), 0, 9999);
         int color = latency < 150 ? -16717456 : (latency < 300 ? -1585120 : -2670024);
         String text = latency + "ms";
         graphics.text(font, text, xo + slotWidth - font.width(text), yo, color);
         ci.cancel();
      }
   }
}
