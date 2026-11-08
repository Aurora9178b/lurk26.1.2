package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoEat extends Module {
   private static final Class<? extends Module>[] AURAS = new Class[]{KillAura.class, CrystalAura.class, BedAura.class};
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgThreshold = this.settings.createGroup("Threshold");
   public final Setting<List<Item>> blacklist = this.sgGeneral
      .add(
         new ItemListSetting.Builder()
            .name("blacklist")
            .description("Which items to not eat.")
            .defaultValue(
               Items.ENCHANTED_GOLDEN_APPLE,
               Items.GOLDEN_APPLE,
               Items.CHORUS_FRUIT,
               Items.POISONOUS_POTATO,
               Items.PUFFERFISH,
               Items.CHICKEN,
               Items.ROTTEN_FLESH,
               Items.SPIDER_EYE,
               Items.SUSPICIOUS_STEW
            )
            .filter(Utils::isFood)
            .bypassFilterWhenSavingAndLoading()
            .build()
      );
   private final Setting<Boolean> pauseAuras = this.sgGeneral
      .add(new BoolSetting.Builder().name("pause-auras").description("Pauses all auras when eating.").defaultValue(true).build());
   private final Setting<Boolean> pauseBaritone = this.sgGeneral
      .add(new BoolSetting.Builder().name("pause-baritone").description("Pause baritone when eating.").defaultValue(true).build());
   private final Setting<Boolean> searchInventory = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("search-inventory").description("Search the full inventory for food, not only the hotbar.").defaultValue(false).build()
      );
   private final Setting<AutoEat.Priority> prioritise = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("food-priority"))
                  .description("Which aspect of the food to prioritise selecting for."))
               .defaultValue(AutoEat.Priority.Saturation))
            .build()
      );
   private final Setting<AutoEat.ThresholdMode> thresholdMode = this.sgThreshold
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("threshold-mode"))
                  .description("The threshold mode to trigger auto eat.\n'Both' == health AND hunger, 'Any' == health OR hunger"))
               .defaultValue(AutoEat.ThresholdMode.Any))
            .build()
      );
   private final Setting<Double> healthThreshold = this.sgThreshold
      .add(
         new DoubleSetting.Builder()
            .name("health-threshold")
            .description("The level of health you eat at.")
            .defaultValue(10.0)
            .range(1.0, 19.0)
            .sliderRange(1.0, 19.0)
            .visible(() -> this.thresholdMode.get() != AutoEat.ThresholdMode.Hunger)
            .build()
      );
   private final Setting<Integer> hungerThreshold = this.sgThreshold
      .add(
         new IntSetting.Builder()
            .name("hunger-threshold")
            .description("The level of hunger you eat at.")
            .defaultValue(16)
            .range(1, 19)
            .sliderRange(1, 19)
            .visible(() -> this.thresholdMode.get() != AutoEat.ThresholdMode.Health)
            .build()
      );
   public boolean eating;
   private int slot;
   private int prevSlot;
   private final List<Class<? extends Module>> wasAura = new ReferenceArrayList<>();
   private boolean wasBaritone = false;

   public AutoEat() {
      super(Categories.Player, "auto-eat", "Automatically eats food.");
   }

   @Override
   public void onDeactivate() {
      if (this.eating) {
         this.stopEating();
      }
   }

   @EventHandler(priority = -100)
   private void onTick(TickEvent.Pre event) {
      if (!Modules.get().get(AutoGap.class).isEating()) {
         if (this.eating) {
            if (!this.shouldEat()) {
               this.stopEating();
            } else {
               if (!Utils.isFood(this.mc.player.getInventory().getItem(this.slot))) {
                  int newSlot = this.findSlot();
                  if (newSlot == -1) {
                     this.stopEating();
                     return;
                  }

                  this.changeSlot(newSlot);
               }

               this.eat();
            }
         } else {
            if (this.shouldEat()) {
               this.startEating();
            }
         }
      }
   }

   @EventHandler
   private void onItemUseCrosshairTarget(ItemUseCrosshairTargetEvent event) {
      if (this.eating) {
         event.target = null;
      }
   }

   private void startEating() {
      this.prevSlot = this.mc.player.getInventory().getSelectedSlot();
      this.eat();
      this.wasAura.clear();
      if (this.pauseAuras.get()) {
         for (Class<? extends Module> klass : AURAS) {
            Module module = Modules.get().get(klass);
            if (module.isActive()) {
               this.wasAura.add(klass);
               module.toggle();
            }
         }
      }

      if (this.pauseBaritone.get() && PathManagers.get().isPathing() && !this.wasBaritone) {
         this.wasBaritone = true;
         PathManagers.get().pause();
      }
   }

   private void eat() {
      if (this.changeSlot(this.slot)) {
         this.setPressed(true);
         if (!this.mc.player.isUsingItem()) {
            Utils.rightClick();
         }

         this.eating = true;
      }
   }

   private void stopEating() {
      if (this.prevSlot != 40) {
         this.changeSlot(this.prevSlot);
      }

      this.setPressed(false);
      this.eating = false;
      if (this.pauseAuras.get()) {
         for (Class<? extends Module> klass : AURAS) {
            if (this.wasAura.contains(klass)) {
               Modules.get().get(klass).enable();
            }
         }
      }

      if (this.pauseBaritone.get() && this.wasBaritone) {
         this.wasBaritone = false;
         PathManagers.get().resume();
      }
   }

   private void setPressed(boolean pressed) {
      this.mc.options.keyUse.setDown(pressed);
   }

   private boolean changeSlot(int slot) {
      if (slot == 40) {
         this.slot = 40;
         return true;
      }

      if (SlotUtils.isHotbar(slot)) {
         InvUtils.swap(slot, false);
         this.slot = slot;
         return true;
      }

      int emptySlot = InvUtils.find(ItemStack::isEmpty, 0, 8).slot();
      if (emptySlot == -1) {
         return false;
      }

      InvUtils.move().from(slot).toHotbar(emptySlot);
      InvUtils.swap(emptySlot, false);
      this.slot = emptySlot;
      return true;
   }

   public boolean shouldEat() {
      boolean healthLow = this.mc.player.getHealth() <= this.healthThreshold.get();
      boolean hungerLow = this.mc.player.getFoodData().getFoodLevel() <= this.hungerThreshold.get();
      if (!this.thresholdMode.get().test(healthLow, hungerLow)) {
         return false;
      }

      this.slot = this.findSlot();
      if (this.slot == -1) {
         return false;
      }

      ItemStack item = this.mc.player.getInventory().getItem(this.slot);
      FoodProperties prop = item.get(DataComponents.FOOD);
      return prop != null && Utils.isFood(item) ? this.mc.player.getFoodData().needsFood() || prop.canAlwaysEat() : false;
   }

   private int findSlot() {
      Item offHandItem = this.mc.player.getOffhandItem().getItem();
      if (Utils.isFood(offHandItem) && !this.blacklist.get().contains(offHandItem)) {
         return 40;
      } else {
         int slot = this.findBestFood(0, 8);
         if (slot != -1) {
            return slot;
         } else {
            return this.searchInventory.get() ? this.findBestFood(9, 35) : -1;
         }
      }
   }

   private int findBestFood(int start, int end) {
      int best = -1;
      float bestHunger = -1.0F;

      for (int i = start; i <= end; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         FoodProperties food = stack.get(DataComponents.FOOD);
         if (Utils.isFood(stack) && food != null) {
            Item item = stack.getItem();
            if (!this.blacklist.get().contains(item)) {
               float hunger = this.prioritise.get().value(food);
               if (hunger > bestHunger) {
                  bestHunger = hunger;
                  best = i;
               }
            }
         }
      }

      return best;
   }

   public enum Priority {
      Combined,
      Hunger,
      Saturation;

      public float value(FoodProperties food) {
         return switch (this) {
            case Combined -> food.nutrition() + food.saturation();
            case Hunger -> food.nutrition();
            case Saturation -> food.saturation();
         };
      }
   }

   public enum ThresholdMode {
      Health((health, hunger) -> health),
      Hunger((health, hunger) -> hunger),
      Any((health, hunger) -> health || hunger),
      Both((health, hunger) -> health && hunger);

      private final BiPredicate<Boolean, Boolean> predicate;

      ThresholdMode(BiPredicate<Boolean, Boolean> predicate) {
         this.predicate = predicate;
      }

      public boolean test(boolean health, boolean hunger) {
         return this.predicate.test(health, hunger);
      }
   }
}
