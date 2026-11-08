package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Sneak;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
   @Inject(method = "tick", at = @At("TAIL"))
   private void isPressed(CallbackInfo ci) {
      if (Modules.get().get(Sneak.class).doVanilla() || Modules.get().get(Freecam.class).staySneaking()) {
         this.keyPresses = new Input(
            this.keyPresses.forward(),
            this.keyPresses.backward(),
            this.keyPresses.left(),
            this.keyPresses.right(),
            this.keyPresses.jump(),
            true,
            this.keyPresses.sprint()
         );
      }

      MoveInputEvent event = (MoveInputEvent)MeteorClient.EVENT_BUS.post(new MoveInputEvent(this.keyPresses));
      this.keyPresses = event.toInput();
      this.moveVector = new Vec2(event.getStrafe(), event.getForward()).normalized();
   }
}
