package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CharTypedEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
   public void onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
      int modifiers = event.modifiers();
      if (event.key() != -1) {
         if (action == 1) {
            modifiers |= Input.getModifier(event.key());
         } else if (action == 0) {
            modifiers &= ~Input.getModifier(event.key());
         }

         if (this.minecraft.screen instanceof WidgetScreen widgetScreen && action == 2) {
            widgetScreen.keyRepeated(new KeyEvent(event.key(), event.scancode(), modifiers));
         }

         if (GuiKeyEvents.canUseKeys) {
            Input.setKeyState(event.key(), action != 0);
            if (((KeyInputEvent)MeteorClient.EVENT_BUS.post(KeyInputEvent.get(new KeyEvent(event.key(), event.scancode(), modifiers), KeyAction.get(action))))
               .isCancelled()) {
               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void onChar(long handle, CharacterEvent event, CallbackInfo ci) {
      if (Utils.canUpdate()
         && !this.minecraft.isPaused()
         && (this.minecraft.screen == null || this.minecraft.screen instanceof WidgetScreen)
         && ((CharTypedEvent)MeteorClient.EVENT_BUS.post(CharTypedEvent.get((char)event.codepoint()))).isCancelled()) {
         ci.cancel();
      }
   }
}
