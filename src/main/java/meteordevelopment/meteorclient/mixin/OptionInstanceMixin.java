package meteordevelopment.meteorclient.mixin;

import java.util.Objects;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.mixininterface.IOptionInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OptionInstance.class)
public abstract class OptionInstanceMixin implements IOptionInstance {
   @Shadow
   private Object value;
   @Shadow
   @Final
   private Consumer<Object> onValueUpdate;

   @Override
   public void meteor$set(Object value) {
      if (!Minecraft.getInstance().isRunning()) {
         this.value = value;
      } else if (!Objects.equals(this.value, value)) {
         this.value = value;
         this.onValueUpdate.accept(this.value);
      }
   }
}
