package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public class MixinWindow {
   @Redirect(
      method = "setIcon",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/IconSet;getStandardIcons(Lnet/minecraft/server/packs/PackResources;)Ljava/util/List;")
   )
   private List<IoSupplier<InputStream>> onSetIcon(IconSet instance, PackResources resources) throws IOException {
      InputStream stream16 = MixinWindow.class.getResourceAsStream("/assets/meteor-client/icon.png");
      InputStream stream32 = MixinWindow.class.getResourceAsStream("/assets/meteor-client/icon.png");
      return stream16 != null && stream32 != null ? List.of(() -> stream16, () -> stream32) : instance.getStandardIcons(resources);
   }
}
