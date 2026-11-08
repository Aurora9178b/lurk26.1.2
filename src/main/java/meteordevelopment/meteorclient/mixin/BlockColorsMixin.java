package meteordevelopment.meteorclient.mixin;

import java.util.List;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {
   @ModifyArg(
      method = "createDefault",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/color/block/BlockColors;register(Ljava/util/List;[Lnet/minecraft/world/level/block/Block;)V",
         ordinal = 3
      ),
      index = 0
   )
   private static List<BlockTintSource> modifySpruceLeavesColor(List<BlockTintSource> layers) {
      return List.of(var0x -> getModifiedColor(-10380959));
   }

   @ModifyArg(
      method = "createDefault",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/color/block/BlockColors;register(Ljava/util/List;[Lnet/minecraft/world/level/block/Block;)V",
         ordinal = 4
      ),
      index = 0
   )
   private static List<BlockTintSource> modifyBirchLeavesColor(List<BlockTintSource> layers) {
      return List.of(var0x -> getModifiedColor(-8345771));
   }

   @Unique
   private static int getModifiedColor(int original) {
      if (Modules.get() == null) {
         return original;
      }

      Ambience ambience = Modules.get().get(Ambience.class);
      return ambience.isActive() && ambience.customFoliageColor.get() ? ambience.foliageColor.get().getPacked() : original;
   }
}
