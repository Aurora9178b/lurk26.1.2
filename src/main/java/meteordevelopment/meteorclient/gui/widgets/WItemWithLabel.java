package meteordevelopment.meteorclient.gui.widgets;

import java.util.Iterator;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public class WItemWithLabel extends WHorizontalList {
   private ItemStack itemStack;
   private String name;
   private WItem item;
   private WLabel label;

   public WItemWithLabel(ItemStack itemStack, String name) {
      this.itemStack = itemStack;
      this.name = name;
   }

   @Override
   public void init() {
      this.item = this.add(this.theme.item(this.itemStack)).widget();
      this.label = this.add(this.theme.label(this.name + this.getStringToAppend())).widget();
   }

   private String getStringToAppend() {
      String str = "";
      if (this.itemStack.getItem() == Items.POTION) {
         PotionContents potionContents = this.itemStack.get(DataComponents.POTION_CONTENTS);
         if (potionContents == null) {
            return str;
         }

         Iterator<MobEffectInstance> effects = potionContents.getAllEffects().iterator();
         if (!effects.hasNext()) {
            return str;
         }

         str = str + " ";
         MobEffectInstance effect = effects.next();
         if (effect.getAmplifier() > 0) {
            str = str + "%d ".formatted(effect.getAmplifier() + 1);
         }

         str = str
            + "(%s)"
               .formatted(
                  MobEffectUtil.formatDuration(effect, 1.0F, MeteorClient.mc.level != null ? MeteorClient.mc.level.tickRateManager().tickrate() : 20.0F)
                     .getString()
               );
      }

      return str;
   }

   public void set(ItemStack itemStack) {
      this.itemStack = itemStack;
      this.item.itemStack = itemStack;
      this.name = Names.get(itemStack);
      this.label.set(this.name + this.getStringToAppend());
   }

   public String getLabelText() {
      return this.label == null ? this.name : this.label.get();
   }
}
