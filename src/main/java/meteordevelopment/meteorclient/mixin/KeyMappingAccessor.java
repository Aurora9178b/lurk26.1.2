package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.platform.InputConstants.Key;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
   @Accessor("ALL")
   static Map<String, KeyMapping> getKeysById() {
      return null;
   }

   @Accessor("key")
   Key meteor$getKey();

   @Accessor("clickCount")
   int meteor$getClickCount();

   @Accessor("clickCount")
   void meteor$setClickCount(int var1);

   @Invoker("release")
   void meteor$invokeRelease();
}
