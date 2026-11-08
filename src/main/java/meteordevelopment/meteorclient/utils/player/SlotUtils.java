package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.mixin.AbstractMountInventoryMenuAccessor;
import meteordevelopment.meteorclient.mixin.CreativeModeInventoryScreenAccessor;
import meteordevelopment.meteorclient.mixin.CreativeModeTabsAccessor;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class SlotUtils {
   public static final int HOTBAR_START = 0;
   public static final int HOTBAR_END = 8;
   public static final int MAIN_START = 9;
   public static final int MAIN_END = 35;
   public static final int ARMOR_START = 36;
   public static final int ARMOR_END = 39;
   public static final int OFFHAND = 40;

   private SlotUtils() {
   }

   public static int indexToId(int param0) {
      if (MeteorClient.mc.player == null) return -1;
      AbstractContainerMenu menu = MeteorClient.mc.player.containerMenu;
      if (menu == null) return -1;
      if (menu instanceof net.minecraft.world.inventory.InventoryMenu) return survivalInventory(param0);
      if (menu instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu) return creativeInventory(param0);
      if (menu instanceof net.minecraft.world.inventory.ChestMenu chest) return genericContainer(param0, chest.getRowCount());
      if (menu instanceof net.minecraft.world.inventory.CraftingMenu) return craftingTable(param0);
      if (menu instanceof net.minecraft.world.inventory.FurnaceMenu || menu instanceof net.minecraft.world.inventory.BlastFurnaceMenu || menu instanceof net.minecraft.world.inventory.SmokerMenu) return furnace(param0);
      if (menu instanceof net.minecraft.world.inventory.DispenserMenu) return generic3x3(param0);
      if (menu instanceof net.minecraft.world.inventory.EnchantmentMenu) return enchantmentTable(param0);
      if (menu instanceof net.minecraft.world.inventory.BrewingStandMenu) return brewingStand(param0);
      if (menu instanceof net.minecraft.world.inventory.MerchantMenu) return villager(param0);
      if (menu instanceof net.minecraft.world.inventory.BeaconMenu) return beacon(param0);
      if (menu instanceof net.minecraft.world.inventory.AnvilMenu) return anvil(param0);
      if (menu instanceof net.minecraft.world.inventory.HopperMenu) return hopper(param0);
      if (menu instanceof net.minecraft.world.inventory.ShulkerBoxMenu) return genericContainer(param0, 3);
      if (menu instanceof net.minecraft.world.inventory.HorseInventoryMenu) return horse(menu, param0);
      if (menu instanceof net.minecraft.world.inventory.CartographyTableMenu) return cartographyTable(param0);
      if (menu instanceof net.minecraft.world.inventory.GrindstoneMenu) return grindstone(param0);
      if (menu instanceof net.minecraft.world.inventory.LecternMenu) return lectern();
      if (menu instanceof net.minecraft.world.inventory.LoomMenu) return loom(param0);
      if (menu instanceof net.minecraft.world.inventory.StonecutterMenu) return stonecutter(param0);
      if (menu instanceof net.minecraft.world.inventory.CrafterMenu) return crafter(param0);
      if (menu instanceof net.minecraft.world.inventory.SmithingMenu) return smithingTable(param0);
      return -1;
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.IllegalStateException: Invalid switch case set: [[var2_1 instanceof var5_1], [var2_1 instanceof var6_1], [var2_1 instanceof chestMenu], [var2_1 instanceof var7_1], [var2_1 instanceof var8_1, const(5), const(6)], [var2_1 instanceof var11_1], [var2_1 instanceof var12_1], [var2_1 instanceof var13_1], [var2_1 instanceof var14_1], [var2_1 instanceof var15_1], [var2_1 instanceof var16_1], [var2_1 instanceof var17_1], [var2_1 instanceof var18_1], [var2_1 instanceof var19_1], [var2_1 instanceof var20_1], [var2_1 instanceof var21_1], [var2_1 instanceof var22_1], [var2_1 instanceof var23_1], [var2_1 instanceof var24_1], [var2_1 instanceof var25_1], [var2_1 instanceof var26_1], [null]] for selector of type Lnet/minecraft/world/inventory/AbstractContainerMenu;
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchHeadExprent.checkExprTypeBounds(SwitchHeadExprent.java:63)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.checkTypeExpr(VarTypeProcessor.java:156)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.checkTypeExprent(VarTypeProcessor.java:137)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.lambda$processVarTypes$2(VarTypeProcessor.java:125)
      //   at org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph.iterateExprents(DirectGraph.java:114)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.processVarTypes(VarTypeProcessor.java:125)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.calculateVarTypes(VarTypeProcessor.java:44)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionsProcessor.setVarVersions(VarVersionsProcessor.java:68)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor.setVarVersions(VarProcessor.java:47)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:320)
      //
      // Bytecode:
      // 000: getstatic meteordevelopment/meteorclient/MeteorClient.mc Lnet/minecraft/client/Minecraft;
      // 003: getfield net/minecraft/client/Minecraft.player Lnet/minecraft/client/player/LocalPlayer;
      // 006: ifnonnull 00b
      // 009: bipush -1
      // 00a: ireturn
      // 00b: getstatic meteordevelopment/meteorclient/MeteorClient.mc Lnet/minecraft/client/Minecraft;
      // 00e: getfield net/minecraft/client/Minecraft.player Lnet/minecraft/client/player/LocalPlayer;
      // 011: getfield net/minecraft/client/player/LocalPlayer.containerMenu Lnet/minecraft/world/inventory/AbstractContainerMenu;
      // 014: astore 1
      // 015: aload 1
      // 016: dup
      // 017: invokestatic java/util/Objects.requireNonNull (Ljava/lang/Object;)Ljava/lang/Object;
      // 01a: pop
      // 01b: astore 2
      // 01c: bipush 0
      // 01d: istore 3
      // 01e: aload 2
      // 01f: iload 3
      // 020: invokedynamic typeSwitch (Lnet/minecraft/world/inventory/AbstractContainerMenu;I)I bsm=java/lang/runtime/SwitchBootstraps.typeSwitch (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[ net/minecraft/world/inventory/InventoryMenu, net/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu, net/minecraft/world/inventory/ChestMenu, net/minecraft/world/inventory/CraftingMenu, net/minecraft/world/inventory/FurnaceMenu, net/minecraft/world/inventory/BlastFurnaceMenu, net/minecraft/world/inventory/SmokerMenu, net/minecraft/world/inventory/DispenserMenu, net/minecraft/world/inventory/EnchantmentMenu, net/minecraft/world/inventory/BrewingStandMenu, net/minecraft/world/inventory/MerchantMenu, net/minecraft/world/inventory/BeaconMenu, net/minecraft/world/inventory/AnvilMenu, net/minecraft/world/inventory/HopperMenu, net/minecraft/world/inventory/ShulkerBoxMenu, net/minecraft/world/inventory/HorseInventoryMenu, net/minecraft/world/inventory/CartographyTableMenu, net/minecraft/world/inventory/GrindstoneMenu, net/minecraft/world/inventory/LecternMenu, net/minecraft/world/inventory/LoomMenu, net/minecraft/world/inventory/StonecutterMenu, net/minecraft/world/inventory/CrafterMenu, net/minecraft/world/inventory/SmithingMenu ]
      // 025: tableswitch 302 0 22 107 114 121 139 146 146 146 189 196 203 210 217 224 231 238 246 254 261 268 274 281 288 295
      // 090: iload 0
      // 091: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.survivalInventory (I)I
      // 094: goto 157
      // 097: iload 0
      // 098: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.creativeInventory (I)I
      // 09b: goto 157
      // 09e: aload 2
      // 09f: checkcast net/minecraft/world/inventory/ChestMenu
      // 0a2: astore 4
      // 0a4: iload 0
      // 0a5: aload 4
      // 0a7: invokevirtual net/minecraft/world/inventory/ChestMenu.getRowCount ()I
      // 0aa: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.genericContainer (II)I
      // 0ad: goto 157
      // 0b0: iload 0
      // 0b1: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.craftingTable (I)I
      // 0b4: goto 157
      // 0b7: aload 2
      // 0b8: instanceof net/minecraft/world/inventory/FurnaceMenu
      // 0bb: ifeq 0c1
      // 0be: goto 0db
      // 0c1: aload 2
      // 0c2: instanceof net/minecraft/world/inventory/BlastFurnaceMenu
      // 0c5: ifeq 0cb
      // 0c8: goto 0db
      // 0cb: aload 2
      // 0cc: instanceof net/minecraft/world/inventory/SmokerMenu
      // 0cf: ifeq 0d5
      // 0d2: goto 0db
      // 0d5: bipush 7
      // 0d7: istore 3
      // 0d8: goto 01e
      // 0db: iload 0
      // 0dc: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.furnace (I)I
      // 0df: goto 157
      // 0e2: iload 0
      // 0e3: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.generic3x3 (I)I
      // 0e6: goto 157
      // 0e9: iload 0
      // 0ea: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.enchantmentTable (I)I
      // 0ed: goto 157
      // 0f0: iload 0
      // 0f1: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.brewingStand (I)I
      // 0f4: goto 157
      // 0f7: iload 0
      // 0f8: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.villager (I)I
      // 0fb: goto 157
      // 0fe: iload 0
      // 0ff: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.beacon (I)I
      // 102: goto 157
      // 105: iload 0
      // 106: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.anvil (I)I
      // 109: goto 157
      // 10c: iload 0
      // 10d: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.hopper (I)I
      // 110: goto 157
      // 113: iload 0
      // 114: bipush 3
      // 115: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.genericContainer (II)I
      // 118: goto 157
      // 11b: aload 1
      // 11c: iload 0
      // 11d: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.horse (Lnet/minecraft/world/inventory/AbstractContainerMenu;I)I
      // 120: goto 157
      // 123: iload 0
      // 124: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.cartographyTable (I)I
      // 127: goto 157
      // 12a: iload 0
      // 12b: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.grindstone (I)I
      // 12e: goto 157
      // 131: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.lectern ()I
      // 134: goto 157
      // 137: iload 0
      // 138: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.loom (I)I
      // 13b: goto 157
      // 13e: iload 0
      // 13f: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.stonecutter (I)I
      // 142: goto 157
      // 145: iload 0
      // 146: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.crafter (I)I
      // 149: goto 157
      // 14c: iload 0
      // 14d: invokestatic meteordevelopment/meteorclient/utils/player/SlotUtils.smithingTable (I)I
      // 150: goto 157
      // 153: bipush -1
      // 154: goto 157
      // 157: ireturn
   }

   private static int survivalInventory(int i) {
      if (isHotbar(i)) {
         return 36 + i;
      } else if (isArmor(i)) {
         return 5 + (i - 36);
      } else {
         return i == 40 ? 45 : i;
      }
   }

   private static int creativeInventory(int i) {
      return CreativeModeInventoryScreenAccessor.meteor$getSelectedTab()
            != BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabsAccessor.meteor$getInventory())
         ? -1
         : survivalInventory(i);
   }

   private static int genericContainer(int i, int rows) {
      if (isHotbar(i)) {
         return (rows + 3) * 9 + i;
      } else {
         return isMain(i) ? rows * 9 + (i - 9) : -1;
      }
   }

   private static int craftingTable(int i) {
      if (isHotbar(i)) {
         return 37 + i;
      } else {
         return isMain(i) ? i + 1 : -1;
      }
   }

   private static int furnace(int i) {
      if (isHotbar(i)) {
         return 30 + i;
      } else {
         return isMain(i) ? 3 + (i - 9) : -1;
      }
   }

   private static int generic3x3(int i) {
      if (isHotbar(i)) {
         return 36 + i;
      } else {
         return isMain(i) ? i : -1;
      }
   }

   private static int enchantmentTable(int i) {
      if (isHotbar(i)) {
         return 29 + i;
      } else {
         return isMain(i) ? 2 + (i - 9) : -1;
      }
   }

   private static int brewingStand(int i) {
      if (isHotbar(i)) {
         return 32 + i;
      } else {
         return isMain(i) ? 5 + (i - 9) : -1;
      }
   }

   private static int villager(int i) {
      if (isHotbar(i)) {
         return 30 + i;
      } else {
         return isMain(i) ? 3 + (i - 9) : -1;
      }
   }

   private static int beacon(int i) {
      if (isHotbar(i)) {
         return 28 + i;
      } else {
         return isMain(i) ? 1 + (i - 9) : -1;
      }
   }

   private static int anvil(int i) {
      if (isHotbar(i)) {
         return 30 + i;
      } else {
         return isMain(i) ? 3 + (i - 9) : -1;
      }
   }

   private static int hopper(int i) {
      if (isHotbar(i)) {
         return 32 + i;
      } else {
         return isMain(i) ? 5 + (i - 9) : -1;
      }
   }

   private static int horse(AbstractContainerMenu handler, int i) {
      LivingEntity entity = ((AbstractMountInventoryMenuAccessor)handler).meteor$getMount();
      if (entity instanceof Llama llamaEntity) {
         int strength = llamaEntity.getStrength();
         if (isHotbar(i)) {
            return 2 + 3 * strength + 28 + i;
         }

         if (isMain(i)) {
            return 2 + 3 * strength + 1 + (i - 9);
         }
      } else if (!(entity instanceof Horse) && !(entity instanceof SkeletonHorse) && !(entity instanceof ZombieHorse) && !(entity instanceof Camel)) {
         if (entity instanceof AbstractChestedHorse abstractDonkeyEntity) {
            boolean chest = abstractDonkeyEntity.hasChest();
            if (isHotbar(i)) {
               return (chest ? 44 : 29) + i;
            }

            if (isMain(i)) {
               return (chest ? 17 : 2) + (i - 9);
            }
         }
      } else {
         if (isHotbar(i)) {
            return 29 + i;
         }

         if (isMain(i)) {
            return 2 + (i - 9);
         }
      }

      return -1;
   }

   private static int cartographyTable(int i) {
      if (isHotbar(i)) {
         return 30 + i;
      } else {
         return isMain(i) ? 3 + (i - 9) : -1;
      }
   }

   private static int grindstone(int i) {
      if (isHotbar(i)) {
         return 30 + i;
      } else {
         return isMain(i) ? 3 + (i - 9) : -1;
      }
   }

   private static int lectern() {
      return -1;
   }

   private static int loom(int i) {
      if (isHotbar(i)) {
         return 31 + i;
      } else {
         return isMain(i) ? 4 + (i - 9) : -1;
      }
   }

   private static int stonecutter(int i) {
      if (isHotbar(i)) {
         return 29 + i;
      } else {
         return isMain(i) ? 2 + (i - 9) : -1;
      }
   }

   private static int crafter(int i) {
      if (isHotbar(i)) {
         return 36 + i;
      } else {
         return isMain(i) ? i : -1;
      }
   }

   private static int smithingTable(int i) {
      if (isHotbar(i)) {
         return 31 + i;
      } else {
         return isMain(i) ? 4 + (i - 9) : -1;
      }
   }

   public static boolean isHotbar(int slotIndex) {
      return slotIndex >= 0 && slotIndex <= 8;
   }

   public static boolean isMain(int slotIndex) {
      return slotIndex >= 9 && slotIndex <= 35;
   }

   public static boolean isArmor(int slotIndex) {
      return slotIndex >= 36 && slotIndex <= 39;
   }
}
