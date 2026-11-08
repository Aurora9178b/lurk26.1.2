package meteordevelopment.meteorclient.systems.modules.player;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;

public class InvManager extends Module {
   private static final List<Item> DEFAULT_ALLOWED_ITEMS = List.of(
      Items.ENDER_PEARL,
      Items.ENDER_EYE,
      Items.TRIDENT,
      Items.MACE,
      Items.BOW,
      Items.CROSSBOW,
      Items.ARROW,
      Items.GOLDEN_APPLE,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.APPLE,
      Items.MUSHROOM_STEW,
      Items.BREAD,
      Items.PORKCHOP,
      Items.COOKED_PORKCHOP,
      Items.GOLDEN_CARROT,
      Items.CARROT,
      Items.POTATO,
      Items.BAKED_POTATO,
      Items.COOKED_BEEF,
      Items.BEEF,
      Items.COOKED_CHICKEN,
      Items.CHICKEN,
      Items.COOKED_MUTTON,
      Items.MUTTON,
      Items.COOKED_RABBIT,
      Items.RABBIT,
      Items.RABBIT_STEW,
      Items.BEETROOT,
      Items.BEETROOT_SOUP,
      Items.MELON_SLICE,
      Items.PUMPKIN_PIE,
      Items.COOKIE,
      Items.SWEET_BERRIES,
      Items.COD,
      Items.COOKED_COD,
      Items.SALMON,
      Items.COOKED_SALMON,
      Items.TROPICAL_FISH,
      Items.PUFFERFISH,
      Items.HONEY_BOTTLE,
      Items.GLOW_BERRIES,
      Items.DRIED_KELP,
      Items.ROTTEN_FLESH,
      Items.POISONOUS_POTATO,
      Items.COMPASS,
      Items.RECOVERY_COMPASS,
      Items.WATER_BUCKET,
      Items.ELYTRA,
      Items.IRON_INGOT,
      Items.DIAMOND,
      Items.EMERALD,
      Items.GOLD_INGOT,
      Items.NETHERITE_INGOT,
      Items.PLAYER_HEAD
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("Delay after inventory actions in ticks.").defaultValue(0).min(0).sliderRange(0, 10).build());
   private final Setting<Integer> armorDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("armor-delay").description("Delay after armor actions in ticks.").defaultValue(0).min(0).sliderRange(0, 10).build());
   private final Setting<Boolean> cleaner = this.sgGeneral
      .add(new BoolSetting.Builder().name("cleaner").description("Drops unwanted items.").defaultValue(true).build());
   private final Setting<Boolean> onlyInventory = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("only-inventory").description("Only manages inventory while the inventory screen is open.").defaultValue(true).build()
      );
   private final Setting<Boolean> custom = this.sgGeneral
      .add(new BoolSetting.Builder().name("custom").description("Uses the custom allowed item list.").defaultValue(true).build());
   private final Setting<List<Item>> allowedItem = this.sgGeneral
      .add(
         new ItemListSetting.Builder()
            .name("allowed-item")
            .description("Items the cleaner should keep.")
            .defaultValue(DEFAULT_ALLOWED_ITEMS)
            .visible(this.custom::get)
            .build()
      );
   private final Map<Item, Integer> itemSlots = new HashMap<>();
   private final Map<Item, Integer> swordMaterialRank = new HashMap<>();
   private final Map<Item, Integer> pickaxeMaterialRank = new HashMap<>();
   private final Map<Item, Integer> axeMaterialRank = new HashMap<>();
   private final Map<Item, Integer> shovelMaterialRank = new HashMap<>();
   public static boolean cleaning;
   private int tickCounter;

   public InvManager() {
      super(Categories.Player, "inv-manager", "Sorts tools, equips armor and cleans junk items.");
      this.registerTool(Items.NETHERITE_SWORD, 0, this.swordMaterialRank, 6);
      this.registerTool(Items.DIAMOND_SWORD, 0, this.swordMaterialRank, 5);
      this.registerTool(Items.IRON_SWORD, 0, this.swordMaterialRank, 4);
      this.registerTool(Items.GOLDEN_SWORD, 0, this.swordMaterialRank, 3);
      this.registerTool(Items.STONE_SWORD, 0, this.swordMaterialRank, 2);
      this.registerTool(Items.WOODEN_SWORD, 0, this.swordMaterialRank, 1);
      this.registerTool(Items.NETHERITE_PICKAXE, 1, this.pickaxeMaterialRank, 6);
      this.registerTool(Items.DIAMOND_PICKAXE, 1, this.pickaxeMaterialRank, 5);
      this.registerTool(Items.IRON_PICKAXE, 1, this.pickaxeMaterialRank, 4);
      this.registerTool(Items.GOLDEN_PICKAXE, 1, this.pickaxeMaterialRank, 3);
      this.registerTool(Items.STONE_PICKAXE, 1, this.pickaxeMaterialRank, 2);
      this.registerTool(Items.WOODEN_PICKAXE, 1, this.pickaxeMaterialRank, 1);
      this.registerTool(Items.NETHERITE_AXE, 2, this.axeMaterialRank, 6);
      this.registerTool(Items.DIAMOND_AXE, 2, this.axeMaterialRank, 5);
      this.registerTool(Items.IRON_AXE, 2, this.axeMaterialRank, 4);
      this.registerTool(Items.GOLDEN_AXE, 2, this.axeMaterialRank, 3);
      this.registerTool(Items.STONE_AXE, 2, this.axeMaterialRank, 2);
      this.registerTool(Items.WOODEN_AXE, 2, this.axeMaterialRank, 1);
      this.registerTool(Items.NETHERITE_SHOVEL, 3, this.shovelMaterialRank, 6);
      this.registerTool(Items.DIAMOND_SHOVEL, 3, this.shovelMaterialRank, 5);
      this.registerTool(Items.IRON_SHOVEL, 3, this.shovelMaterialRank, 4);
      this.registerTool(Items.GOLDEN_SHOVEL, 3, this.shovelMaterialRank, 3);
      this.registerTool(Items.STONE_SHOVEL, 3, this.shovelMaterialRank, 2);
      this.registerTool(Items.WOODEN_SHOVEL, 3, this.shovelMaterialRank, 1);
   }

   @Override
   public void onActivate() {
      this.tickCounter = 0;
      cleaning = false;
   }

   @Override
   public void onDeactivate() {
      this.tickCounter = 0;
      cleaning = false;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.gameMode != null) {
         if ((!this.onlyInventory.get() || this.mc.screen instanceof InventoryScreen)
            && !(this.mc.player.containerMenu instanceof ChestMenu)
            && !(this.mc.player.containerMenu instanceof AbstractFurnaceMenu)
            && this.mc.player.containerMenu instanceof InventoryMenu) {
            if (this.tickCounter > 0) {
               cleaning = true;
               this.tickCounter--;
            } else if (!this.moveBestTools()) {
               if (!this.equipBestArmor()) {
                  if (!this.cleaner.get() || !this.cleanInventory()) {
                     cleaning = false;
                  }
               }
            }
         } else {
            cleaning = false;
            this.tickCounter = 0;
         }
      }
   }

   private boolean moveBestTools() {
      int[] targets = this.bestToolIndices();

      for (int hotbarSlot = 0; hotbarSlot < targets.length; hotbarSlot++) {
         int inventoryIndex = targets[hotbarSlot];
         if (inventoryIndex != -1 && inventoryIndex != hotbarSlot) {
            cleaning = true;
            this.swapItems(inventoryIndex, hotbarSlot);
            this.tickCounter = this.delay.get();
            return true;
         }
      }

      return false;
   }

   private boolean equipBestArmor() {
      Map<EquipmentSlot, InvManager.ArmorCandidate> candidates = new EnumMap<>(EquipmentSlot.class);

      for (int i = 0; i < 36; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         EquipmentSlot slot = this.getArmorSlot(stack);
         if (slot != null) {
            InvManager.ArmorCandidate current = candidates.get(slot);
            if (current == null || this.isBetterArmor(stack, current.stack())) {
               candidates.put(slot, new InvManager.ArmorCandidate(stack, i));
            }
         }
      }

      for (Entry<EquipmentSlot, InvManager.ArmorCandidate> entry : candidates.entrySet()) {
         EquipmentSlot slot = entry.getKey();
         InvManager.ArmorCandidate candidate = entry.getValue();
         ItemStack currentArmor = this.mc.player.getItemBySlot(slot);
         if (currentArmor.isEmpty() || this.isBetterArmor(candidate.stack(), currentArmor)) {
            cleaning = true;
            if (!currentArmor.isEmpty()) {
               InvUtils.drop().slotArmor(slot.getIndex());
               this.tickCounter = this.armorDelay.get();
            } else {
               this.quickMove(candidate.inventoryIndex());
               this.tickCounter = this.delay.get();
            }

            return true;
         }
      }

      return false;
   }

   private boolean cleanInventory() {
      int[] cleanerTargets = this.bestToolIndices();
      Set<Item> allowedItems = new HashSet<>(this.custom.get() ? this.allowedItem.get() : DEFAULT_ALLOWED_ITEMS);

      for (int i = 0; i < 36; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (!this.shouldKeep(stack, i, cleanerTargets, allowedItems)) {
            cleaning = true;
            this.drop(i, stack.getCount() > 1 ? 1 : 0);
            this.tickCounter = this.delay.get();
            return true;
         }
      }

      return false;
   }

   private int[] bestToolIndices() {
      int[] targets = new int[9];
      Arrays.fill(targets, -1);

      for (int i = 0; i < 36; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         Integer hotbarSlot = this.itemSlots.get(stack.getItem());
         if (hotbarSlot != null) {
            int currentIndex = targets[hotbarSlot];
            if (currentIndex == -1 || this.compareItems(stack, this.mc.player.getInventory().getItem(currentIndex)) > 0) {
               targets[hotbarSlot] = i;
            }
         }
      }

      return targets;
   }

   private boolean shouldKeep(ItemStack stack, int index, int[] cleanerTargets, Set<Item> allowedItems) {
      if (stack.isEmpty()) {
         return true;
      }

      if (stack.getItem() instanceof BlockItem) {
         return true;
      }

      if (stack.getItem() instanceof SpawnEggItem) {
         return true;
      }

      if (stack.getItem() instanceof PotionItem) {
         return true;
      }

      if (stack.getItem() instanceof SplashPotionItem) {
         return true;
      }

      if (stack.getItem() instanceof LingeringPotionItem) {
         return true;
      }

      if (allowedItems.contains(stack.getItem())) {
         return true;
      }

      for (int target : cleanerTargets) {
         if (target == index) {
            return true;
         }
      }

      return false;
   }

   private int compareItems(ItemStack stack1, ItemStack stack2) {
      int rank1 = this.materialRank(stack1);
      int rank2 = this.materialRank(stack2);
      return rank1 != rank2 ? Integer.compare(rank1, rank2) : Integer.compare(this.enchantmentScore(stack1), this.enchantmentScore(stack2));
   }

   private int materialRank(ItemStack stack) {
      Item item = stack.getItem();
      if (stack.is(ItemTags.SWORDS)) {
         return this.swordMaterialRank.getOrDefault(item, 0);
      } else if (stack.is(ItemTags.PICKAXES)) {
         return this.pickaxeMaterialRank.getOrDefault(item, 0);
      } else if (stack.is(ItemTags.AXES)) {
         return this.axeMaterialRank.getOrDefault(item, 0);
      } else {
         return stack.is(ItemTags.SHOVELS) ? this.shovelMaterialRank.getOrDefault(item, 0) : 0;
      }
   }

   private int enchantmentScore(ItemStack stack) {
      return Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS)
         + Utils.getEnchantmentLevel(stack, Enchantments.EFFICIENCY)
         + Utils.getEnchantmentLevel(stack, Enchantments.UNBREAKING)
         + Utils.getEnchantmentLevel(stack, Enchantments.MENDING) * 2
         + Utils.getEnchantmentLevel(stack, Enchantments.FORTUNE)
         + Utils.getEnchantmentLevel(stack, Enchantments.SILK_TOUCH);
   }

   private EquipmentSlot getArmorSlot(ItemStack stack) {
      if (!stack.isEmpty() && stack.has(DataComponents.EQUIPPABLE)) {
         EquipmentSlot slot = stack.get(DataComponents.EQUIPPABLE).slot();

         return switch (slot) {
            case HEAD, CHEST, LEGS, FEET -> slot;
            default -> null;
         };
      } else {
         return null;
      }
   }

   private boolean isBetterArmor(ItemStack candidate, ItemStack current) {
      return this.armorScore(candidate) > this.armorScore(current);
   }

   private int armorScore(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0;
      }

      int score = 0;
      score += Utils.getEnchantmentLevel(stack, Enchantments.PROTECTION);
      score += Utils.getEnchantmentLevel(stack, Enchantments.BLAST_PROTECTION);
      score += Utils.getEnchantmentLevel(stack, Enchantments.FIRE_PROTECTION);
      score += Utils.getEnchantmentLevel(stack, Enchantments.PROJECTILE_PROTECTION);
      score += Utils.getEnchantmentLevel(stack, Enchantments.UNBREAKING);
      score += Utils.getEnchantmentLevel(stack, Enchantments.MENDING) * 2;
      if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
         ItemAttributeModifiers component = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);

         for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry modifier : component.modifiers()) {
            if (modifier.attribute() == Attributes.ARMOR || modifier.attribute() == Attributes.ARMOR_TOUGHNESS) {
               double amount = modifier.modifier().amount();

               score += switch (modifier.modifier().operation()) {
                  case ADD_VALUE -> (int)amount;
                  case ADD_MULTIPLIED_BASE -> (int)(amount * this.mc.player.getAttributeBaseValue(modifier.attribute()));
                  case ADD_MULTIPLIED_TOTAL -> (int)(amount * score);
               };
            }
         }
      }

      return score;
   }

   private void swapItems(int from, int toHotbarSlot) {
      int fromSlot = SlotUtils.indexToId(from);
      int toSlot = SlotUtils.indexToId(toHotbarSlot);
      if (fromSlot != -1 && fromSlot != toSlot) {
         this.mc.gameMode.handleContainerInput(this.mc.player.containerMenu.containerId, fromSlot, toHotbarSlot, ContainerInput.SWAP, this.mc.player);
      }
   }

   private void quickMove(int index) {
      int slotId = SlotUtils.indexToId(index);
      if (slotId != -1) {
         this.mc.gameMode.handleContainerInput(this.mc.player.containerMenu.containerId, slotId, 0, ContainerInput.QUICK_MOVE, this.mc.player);
      }
   }

   private void drop(int index, int button) {
      int slotId = SlotUtils.indexToId(index);
      if (slotId != -1) {
         this.mc.gameMode.handleContainerInput(this.mc.player.containerMenu.containerId, slotId, button, ContainerInput.THROW, this.mc.player);
      }
   }

   private void registerTool(Item item, int hotbarSlot, Map<Item, Integer> materialRank, int rank) {
      this.itemSlots.put(item, hotbarSlot);
      materialRank.put(item, rank);
   }

   private record ArmorCandidate(ItemStack stack, int inventoryIndex) {
   }
}
