package meteordevelopment.meteorclient.utils.misc;

import net.minecraft.nbt.CompoundTag;

public interface ISerializable<T> {
   CompoundTag toTag();

   T fromTag(CompoundTag var1);
}
