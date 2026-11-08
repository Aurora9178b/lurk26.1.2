package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;

public class WindowConfig implements ISerializable<WindowConfig> {
   public boolean expanded = true;
   public double x = -1.0;
   public double y = -1.0;

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.putBoolean("expanded", this.expanded);
      tag.putDouble("x", this.x);
      tag.putDouble("y", this.y);
      return tag;
   }

   public WindowConfig fromTag(CompoundTag tag) {
      tag.getBoolean("expanded").ifPresent(bool -> this.expanded = bool);
      tag.getDouble("x").ifPresent(x1 -> this.x = x1);
      tag.getDouble("y").ifPresent(y1 -> this.y = y1);
      return this;
   }
}
