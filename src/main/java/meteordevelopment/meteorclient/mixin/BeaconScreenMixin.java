package meteordevelopment.meteorclient.mixin;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterBeacons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen.BeaconCancelButton;
import net.minecraft.client.gui.screens.inventory.BeaconScreen.BeaconConfirmButton;
import net.minecraft.client.gui.screens.inventory.BeaconScreen.BeaconPowerButton;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends AbstractContainerScreen<BeaconMenu> {
   @Shadow
   protected abstract <T extends AbstractWidget> void addBeaconButton(T var1);

   public BeaconScreenMixin(BeaconMenu handler, Inventory inventory, Component title) {
      super(handler, inventory, title);
   }

   @Inject(method = "init", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", shift = Shift.AFTER), cancellable = true)
   private void changeButtons(CallbackInfo ci) {
      if (Modules.get().get(BetterBeacons.class).isActive()) {
         List<Holder<MobEffect>> effects = BeaconBlockEntity.BEACON_EFFECTS.stream().flatMap(Collection::stream).toList();
         if (Minecraft.getInstance().screen instanceof BeaconScreen beaconScreen) {
            Objects.requireNonNull(beaconScreen);
            this.addBeaconButton(beaconScreen.new BeaconConfirmButton(this.leftPos + 164, this.topPos + 107));
            Objects.requireNonNull(beaconScreen);
            this.addBeaconButton(beaconScreen.new BeaconCancelButton(this.leftPos + 190, this.topPos + 107));

            for (int x = 0; x < 3; x++) {
               for (int y = 0; y < 2; y++) {
                  Holder<MobEffect> effect = effects.get(x * 2 + y);
                  int xMin = this.leftPos + x * 25;
                  int yMin = this.topPos + y * 25;
                  Objects.requireNonNull(beaconScreen);
                  this.addBeaconButton(beaconScreen.new BeaconPowerButton(xMin + 27, yMin + 32, effect, true, -1));
                  Objects.requireNonNull(beaconScreen);
                  BeaconPowerButton secondaryWidget = beaconScreen.new BeaconPowerButton(xMin + 133, yMin + 32, effect, false, 3);
                  if (this.getMenu().getLevels() != 4) {
                     secondaryWidget.active = false;
                  }

                  this.addBeaconButton(secondaryWidget);
               }
            }
         }

         ci.cancel();
      }
   }

   @Inject(method = "extractBackground", at = @At("TAIL"))
   private void onExtractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
      if (Modules.get().get(BetterBeacons.class).isActive()) {
         graphics.fill(this.leftPos + 10, this.topPos + 7, this.leftPos + 220, this.topPos + 98, -14606047);
      }
   }
}
