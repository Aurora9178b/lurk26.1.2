package meteordevelopment.meteorclient.utils.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

public class NbtUtils {
   public static <T extends ISerializable<?>> ListTag listToTag(Iterable<T> list) {
      ListTag tag = new ListTag();

      for (T item : list) {
         tag.add(item.toTag());
      }

      return tag;
   }

   public static <T> List<T> listFromTag(ListTag tag, NbtUtils.ToValue<T> toItem) {
      List<T> list = new ArrayList<>(tag.size());

      for (Tag itemTag : tag) {
         T value = toItem.toValue(itemTag);
         if (value != null) {
            list.add(value);
         }
      }

      return list;
   }

   public static <K, V extends ISerializable<?>> CompoundTag mapToTag(Map<K, V> map) {
      CompoundTag tag = new CompoundTag();

      for (Entry<K, V> entry : map.entrySet()) {
         tag.put(entry.getKey().toString(), entry.getValue().toTag());
      }

      return tag;
   }

   public static <K, V> Map<K, V> mapFromTag(CompoundTag tag, NbtUtils.ToKey<K> toKey, NbtUtils.ToValue<V> toValue) {
      Map<K, V> map = HashMap.newHashMap(tag.size());

      for (String key : tag.keySet()) {
         map.put(toKey.toKey(key), toValue.toValue(tag.get(key)));
      }

      return map;
   }

   public static boolean toClipboard(ISerializable<?> serializable) {
      return toClipboard(serializable.toTag());
   }

   public static boolean toClipboard(CompoundTag tag) {
      String preClipboard = MeteorClient.mc.keyboardHandler.getClipboard();

      try {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         NbtIo.writeCompressed(tag, byteArrayOutputStream);
         MeteorClient.mc.keyboardHandler.setClipboard(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
         return true;
      } catch (Exception e) {
         MeteorClient.LOG.error("Error copying NBT to clipboard!", e);
         MeteorClient.mc.keyboardHandler.setClipboard(preClipboard);
         return false;
      }
   }

   public static boolean fromClipboard(ISerializable<?> serializable) {
      CompoundTag tag = fromClipboard();
      if (tag == null) {
         return false;
      }

      CompoundTag sourceTag = serializable.toTag();

      for (String key : sourceTag.keySet()) {
         if (!tag.contains(key)) {
            return false;
         }
      }

      serializable.fromTag(tag);
      return true;
   }

   public static CompoundTag fromClipboard() {
      try {
         byte[] data = Base64.getDecoder().decode(MeteorClient.mc.keyboardHandler.getClipboard().trim());
         ByteArrayInputStream bis = new ByteArrayInputStream(data);
         return NbtIo.readCompressed(new DataInputStream(bis), NbtAccounter.unlimitedHeap());
      } catch (Exception e) {
         MeteorClient.LOG.error("Invalid NBT data pasted!", e);
         return null;
      }
   }

   public interface ToKey<T> {
      T toKey(String var1);
   }

   public interface ToValue<T> {
      T toValue(Tag var1);
   }
}
