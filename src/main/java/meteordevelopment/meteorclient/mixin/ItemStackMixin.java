package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
   @ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"))
   private List<Component> onGetTooltipLines(List<Component> original) {
      if (Utils.canUpdate()) {
         ItemStackTooltipEvent event = (ItemStackTooltipEvent)MeteorClient.EVENT_BUS.post(new ItemStackTooltipEvent((ItemStack)(Object)this, original));
         return event.list();
      } else {
         return original;
      }
   }

   @Inject(method = "finishUsingItem", at = @At("HEAD"))
   private void onFinishUsingItem(Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
      if (livingEntity == MeteorClient.mc.player) {
         MeteorClient.EVENT_BUS.post(FinishUsingItemEvent.get((ItemStack)(Object)(Object)this));
      }
   }

   @Inject(method = "releaseUsing", at = @At("HEAD"))
   private void onReleaseUsing(Level level, LivingEntity entity, int remainingTime, CallbackInfo ci) {
      if (entity == MeteorClient.mc.player) {
         MeteorClient.EVENT_BUS.post(StoppedUsingItemEvent.get((ItemStack)(Object)(Object)this));
      }
   }
}
