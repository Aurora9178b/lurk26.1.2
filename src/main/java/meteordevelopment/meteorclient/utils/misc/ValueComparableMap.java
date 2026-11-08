package meteordevelopment.meteorclient.utils.misc;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ValueComparableMap<K extends Comparable<K>, V> extends TreeMap<K, V> {
   private final transient Map<K, V> valueMap;

   public ValueComparableMap(Comparator<? super V> partialValueComparator) {
      this(partialValueComparator, new HashMap<>());
   }

   private ValueComparableMap(Comparator<? super V> partialValueComparator, HashMap<K, V> valueMap) {
      super((k1, k2) -> {
         int cmp = partialValueComparator.compare(valueMap.get(k1), valueMap.get(k2));
         return cmp != 0 ? cmp : k1.compareTo((K)k2);
      });
      this.valueMap = valueMap;
   }

   public V put(K k, V v) {
      if (this.valueMap.containsKey(k)) {
         this.remove(k);
      }

      this.valueMap.put(k, v);
      return super.put(k, v);
   }

   @Override
   public boolean containsKey(Object key) {
      return this.valueMap.containsKey(key);
   }

   @Override
   public V getOrDefault(Object key, V defaultValue) {
      return this.containsKey(key) ? this.get(key) : defaultValue;
   }
}
