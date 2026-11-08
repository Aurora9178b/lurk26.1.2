package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.ClientSetting;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.systems.modules.render.ItemHighlight;
import meteordevelopment.meteorclient.utils.render.ReGlassBackgroundQueue;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
   @Shadow
   protected Slot hoveredSlot;
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;
   @Shadow
   protected int titleLabelX;
   @Shadow
   protected int titleLabelY;
   @Shadow
   private boolean doubleclick;
   @Unique
   private final Map<Slot, int[]> meteor$reglassOriginalSlotPositions = new IdentityHashMap<>();

   @Shadow
   @Nullable
   protected abstract Slot getHoveredSlot(double var1, double var3);

   @Shadow
   @Override
   public abstract T getMenu();

   @Shadow
   protected abstract void slotClicked(Slot var1, int var2, int var3, ContainerInput var4);

   @Shadow
   @Override
   public abstract void onClose();

   public AbstractContainerScreenMixin(Component title) {
      super(title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      this.meteor$reglassRememberOriginalSlots();
      this.meteor$reglassRestoreOriginalSlots();
      if (LiquidGlassSetting.isEnabled()) {
         this.meteor$reglassSpaceSlots();
      }

      InventoryTweaks invTweaks = Modules.get().get(InventoryTweaks.class);
      if (invTweaks.isActive() && invTweaks.showButtons() && invTweaks.canSteal(this.getMenu())) {
         this.addRenderableWidget(
            new Builder(Component.literal("Steal"), var2x -> invTweaks.steal(this.getMenu())).pos(this.leftPos, this.topPos - 22).size(40, 20).build()
         );
         this.addRenderableWidget(
            new Builder(Component.literal("Dump"), var2x -> invTweaks.dump(this.getMenu())).pos(this.leftPos + 42, this.topPos - 22).size(40, 20).build()
         );
      }
   }

   @Inject(
      method = "extractContents",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"
      )
   )
   private void meteor$extractReGlassContainer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (LiquidGlassSetting.isEnabled()) {
         this.meteor$reglassRenderSlotGroups();
      }
   }

   @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
   private void meteor$hideReGlassInventoryLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
      if (LiquidGlassSetting.isEnabled()) {
         if (!this.meteor$reglassIsPlayerInventory()) {
            graphics.text(this.font, this.title, this.meteor$reglassLabelX(this.titleLabelX), this.meteor$reglassLabelY(this.titleLabelY), -1, true);
         }

         ci.cancel();
      }
   }

   @Inject(method = "mouseDragged", at = @At("TAIL"))
   private void onMouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
      if (event.button() == 0 && !this.doubleclick && Modules.get().get(InventoryTweaks.class).mouseDragItemMove()) {
         Slot slot = this.getHoveredSlot(event.x(), event.y());
         if (slot != null && slot.hasItem() && MeteorClient.mc.hasShiftDown()) {
            this.slotClicked(slot, slot.index, event.button(), ContainerInput.QUICK_MOVE);
         }
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);
      if (tooltips.shouldOpenContents(event)
         && this.hoveredSlot != null
         && !this.hoveredSlot.getItem().isEmpty()
         && this.getMenu().getCarried().isEmpty()
         && tooltips.openContent(this.hoveredSlot.getItem())) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   private void keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
      BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);
      if (tooltips.shouldOpenContents(event)
         && this.hoveredSlot != null
         && !this.hoveredSlot.getItem().isEmpty()
         && this.getMenu().getCarried().isEmpty()
         && tooltips.openContent(this.hoveredSlot.getItem())) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "extractSlot", at = @At("HEAD"))
   private void onRenderSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
      int color = Modules.get().get(ItemHighlight.class).getColor(slot.getItem());
      if (color != -1) {
         graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
      }
   }

   @ModifyReturnValue(method = "showTooltipWithItemInHand", at = @At("RETURN"))
   private boolean showTooltipWithItemInHand(boolean original, ItemStack item) {
      return !(item.getTooltipImage().orElse(null) instanceof ClientTooltipComponent component) ? original : original || component.showTooltipWithItemInHand();
   }

   @Unique
   private void meteor$reglassRenderSlotGroups() {
      for (int[] group : this.meteor$reglassGetSlotGroups()) {
         this.meteor$reglassRenderGroup(group);
      }
   }

   @Unique
   private List<int[]> meteor$reglassGetSlotGroups() {
      ArrayList<Slot> slots = new ArrayList<>();

      for (Slot slot : this.getMenu().slots) {
         if (slot.isActive()) {
            slots.add(slot);
         }
      }

      slots.sort(Comparator.<Slot>comparingInt(slotx -> slotx.y).thenComparingInt(slotx -> slotx.x));
      ArrayList<int[]> groups = new ArrayList<>();

      for (Slot slot : slots) {
         int[] rect = new int[]{slot.x, slot.y, slot.x + 18, slot.y + 18};
         int target = -1;

         for (int i = 0; i < groups.size(); i++) {
            if (this.meteor$reglassIsConnected(groups.get(i), rect)) {
               target = i;
               break;
            }
         }

         if (target == -1) {
            groups.add(rect);
         } else {
            int[] group = groups.get(target);
            this.meteor$reglassInclude(group, rect);

            for (int i = groups.size() - 1; i >= 0; i--) {
               if (i != target && this.meteor$reglassIsConnected(group, groups.get(i))) {
                  this.meteor$reglassInclude(group, groups.remove(i));
                  if (i < target) {
                     target--;
                  }
               }
            }
         }
      }

      groups.sort(Comparator.<int[]>comparingInt(groupx -> groupx[1]).thenComparingInt(groupx -> groupx[0]));
      this.meteor$reglassMergeVillagerCostSlots(groups);
      return this.meteor$reglassRemoveContainedGroups(groups);
   }

   @Unique
   private boolean meteor$reglassIsConnected(int[] a, int[] b) {
      boolean overlapsX = a[0] < b[2] && a[2] > b[0];
      boolean overlapsY = a[1] < b[3] && a[3] > b[1];
      boolean touchesX = a[2] == b[0] || b[2] == a[0];
      boolean touchesY = a[3] == b[1] || b[3] == a[1];
      return touchesX && overlapsY || touchesY && overlapsX;
   }

   @Unique
   private void meteor$reglassInclude(int[] target, int[] rect) {
      target[0] = Math.min(target[0], rect[0]);
      target[1] = Math.min(target[1], rect[1]);
      target[2] = Math.max(target[2], rect[2]);
      target[3] = Math.max(target[3], rect[3]);
   }

   @Unique
   private void meteor$reglassMergeVillagerCostSlots(List<int[]> groups) {
      if (this.getMenu() instanceof MerchantMenu && this.getMenu().slots.size() >= 2) {
         Slot first = this.getMenu().slots.get(0);
         Slot second = this.getMenu().slots.get(1);
         if (first.isActive() && second.isActive()) {
            int[] merged = new int[]{
               Math.min(first.x, second.x), Math.min(first.y, second.y), Math.max(first.x, second.x) + 18, Math.max(first.y, second.y) + 18
            };
            groups.removeIf(group -> this.meteor$reglassIsSingleSlotGroup(group, first) || this.meteor$reglassIsSingleSlotGroup(group, second));
            groups.add(merged);
            groups.sort(Comparator.<int[]>comparingInt(group -> group[1]).thenComparingInt(group -> group[0]));
         }
      }
   }

   @Unique
   private boolean meteor$reglassIsSingleSlotGroup(int[] group, Slot slot) {
      return group[0] == slot.x && group[1] == slot.y && group[2] == slot.x + 18 && group[3] == slot.y + 18;
   }

   @Unique
   private void meteor$reglassRenderGroup(int[] group) {
      int groupWidth = group[2] - group[0];
      int groupHeight = group[3] - group[1];
      if (this.meteor$reglassIsHotbarGroup(groupWidth, groupHeight)) {
         ReGlassBackgroundQueue.add(this.leftPos + group[0] - 4, this.topPos + group[1] - 2, 170.0, 22.0, 11.0, ClientSetting.getHotbarBackgroundColor());
      } else {
         int padding = groupWidth == 18 && groupHeight == 18 ? this.meteor$reglassSingleSlotPadding() : 4;
         int x = this.leftPos + group[0] - padding - 1;
         int y = this.topPos + group[1] - padding - 1;
         int width = groupWidth + padding * 2;
         int height = groupHeight + padding * 2;
         ReGlassBackgroundQueue.add(
            x, y, width, height, Math.min(12.0, Math.max(6.0, Math.min(width, height) * 0.22)), this.meteor$reglassPanelColor(group, groupWidth, groupHeight)
         );
      }
   }

   @Unique
   private boolean meteor$reglassIsHotbarGroup(int groupWidth, int groupHeight) {
      return groupWidth == 162 && groupHeight == 18;
   }

   @Unique
   private Color meteor$reglassPanelColor(int[] group, int groupWidth, int groupHeight) {
      Integer shulkerTint = this.meteor$reglassShulkerMainTint(group, groupWidth, groupHeight);
      return shulkerTint == null ? ReGlassBackgroundQueue.PANEL : new Color(shulkerTint >> 16 & 0xFF, shulkerTint >> 8 & 0xFF, shulkerTint & 0xFF, 115);
   }

   @Unique
   private Integer meteor$reglassShulkerMainTint(int[] group, int groupWidth, int groupHeight) {
      if (this.getMenu() instanceof ShulkerBoxMenu && groupWidth == 162 && groupHeight == 54 && group[1] < 80) {
         DyeColor color = null;
         if (((ShulkerBoxMenuAccessor)(Object)this.getMenu()).meteor$getContainer() instanceof ShulkerBoxBlockEntity shulkerBox) {
            color = shulkerBox.getColor();
         }

         if (color == null) {
            color = this.meteor$reglassShulkerColorFromTitle();
         }

         return color == null ? 9330085 : this.meteor$reglassShulkerTintColor(color);
      } else {
         return null;
      }
   }

   @Unique
   private DyeColor meteor$reglassShulkerColorFromTitle() {
      String title = this.title.getString().toLowerCase(Locale.ROOT).replace('_', ' ');
      ArrayList<DyeColor> colors = new ArrayList<>(List.of(DyeColor.values()));
      colors.sort(Comparator.<DyeColor>comparingInt(colorx -> colorx.getName().length()).reversed());

      for (DyeColor color : colors) {
         if (title.contains(color.getName().replace('_', ' '))) {
            return color;
         }
      }

      return null;
   }

   @Unique
   private int meteor$reglassShulkerTintColor(DyeColor color) {
      return switch (color) {
         case WHITE -> 15330541;
         case ORANGE -> 15758867;
         case MAGENTA -> 12403891;
         case LIGHT_BLUE -> 3846105;
         case YELLOW -> 16303655;
         case LIME -> 7387417;
         case PINK -> 15568300;
         case GRAY -> 4080711;
         case LIGHT_GRAY -> 9342598;
         case CYAN -> 1411473;
         case PURPLE -> 7940780;
         case BLUE -> 3488157;
         case BROWN -> 7489320;
         case GREEN -> 5532955;
         case RED -> 10561314;
         case BLACK -> 1316121;
      };
   }

   @Unique
   private int meteor$reglassSingleSlotPadding() {
      return this.getMenu() instanceof BeaconMenu ? 2 : 3;
   }

   @Unique
   private int meteor$reglassLabelY(int y) {
      if (this.getMenu() instanceof LoomMenu) {
         return 32;
      } else if (this.getMenu() instanceof CraftingMenu || this.getMenu() instanceof CrafterMenu || this.getMenu() instanceof DispenserMenu) {
         return y - 4;
      } else {
         return this.getMenu() instanceof BrewingStandMenu ? y - 10 : y - 2;
      }
   }

   @Unique
   private int meteor$reglassLabelX(int x) {
      return this.getMenu() instanceof LoomMenu ? -8 : x;
   }

   @Unique
   private void meteor$reglassRememberOriginalSlots() {
      for (Slot slot : this.getMenu().slots) {
         this.meteor$reglassOriginalSlotPositions.putIfAbsent(slot, new int[]{slot.x, slot.y});
      }
   }

   @Unique
   private void meteor$reglassRestoreOriginalSlots() {
      for (Entry<Slot, int[]> entry : this.meteor$reglassOriginalSlotPositions.entrySet()) {
         this.meteor$reglassSetSlotPosition(entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
      }
   }

   @Unique
   private void meteor$reglassSpaceSlots() {
      if (this.meteor$reglassIsPlayerInventory()) {
         this.meteor$reglassSpaceSurvivalInventorySlots();
      } else {
         if (this.getMenu() instanceof BeaconMenu) {
            this.meteor$reglassSpaceBeaconSlots();
         } else if (this.getMenu() instanceof BrewingStandMenu) {
            this.meteor$reglassSpaceBrewingStandSlots();
         } else if (this.getMenu() instanceof GrindstoneMenu) {
            this.meteor$reglassSpaceGrindstoneSlots();
         } else if (this.getMenu() instanceof LoomMenu) {
            this.meteor$reglassSpaceLoomSlots();
         } else if (this.getMenu() instanceof EnchantmentMenu) {
            this.meteor$reglassSpaceEnchantmentSlots();
         }

         this.meteor$reglassSpaceGenericHotbar();
      }
   }

   @Unique
   private void meteor$reglassSpaceSurvivalInventorySlots() {
      for (int i = 5; i <= 8; i++) {
         this.meteor$reglassSetSlotPosition(i, 8, 2 + (i - 5) * 18);
      }

      this.meteor$reglassSetSlotPosition(45, 69, 56);

      for (int i = 36; i <= 44; i++) {
         this.meteor$reglassSetSlotPosition(i, 8 + (i - 36) * 18, 150);
      }
   }

   @Unique
   private void meteor$reglassSpaceGenericHotbar() {
      ArrayList<Slot> candidates = new ArrayList<>();
      int bestY = Integer.MIN_VALUE;

      for (Slot slot : this.getMenu().slots) {
         int containerSlot = slot.getContainerSlot();
         if (slot.isActive() && containerSlot >= 0 && containerSlot <= 8 && slot.y > bestY) {
            bestY = slot.y;
         }
      }

      if (bestY != Integer.MIN_VALUE) {
         for (Slot slot : this.getMenu().slots) {
            int containerSlot = slot.getContainerSlot();
            if (slot.isActive() && containerSlot >= 0 && containerSlot <= 8 && slot.y == bestY) {
               candidates.add(slot);
            }
         }

         if (candidates.size() == 9) {
            candidates.sort(Comparator.comparingInt(Slot::getContainerSlot));

            for (Slot slot : candidates) {
               int[] original = this.meteor$reglassOriginalSlotPositions.get(slot);
               if (original != null) {
                  this.meteor$reglassSetSlotPosition(slot, original[0], original[1] + 8);
               }
            }
         }
      }
   }

   @Unique
   private void meteor$reglassSpaceBrewingStandSlots() {
      this.meteor$reglassSetSlotPosition(0, 46, 45);
      this.meteor$reglassSetSlotPosition(1, 79, 51);
      this.meteor$reglassSetSlotPosition(2, 112, 45);
      this.meteor$reglassSetSlotPosition(3, 79, 10);
      this.meteor$reglassSetSlotPosition(4, 23, 26);
   }

   @Unique
   private void meteor$reglassSpaceBeaconSlots() {
      this.meteor$reglassSetSlotPosition(0, 143, 107);
   }

   @Unique
   private void meteor$reglassSpaceGrindstoneSlots() {
      this.meteor$reglassSetSlotPosition(0, 40, 42);
      this.meteor$reglassSetSlotPosition(1, 58, 42);
   }

   @Unique
   private void meteor$reglassSpaceLoomSlots() {
      this.meteor$reglassSetSlotPosition(0, -4, 48);
      this.meteor$reglassSetSlotPosition(1, 14, 48);
      this.meteor$reglassSetSlotPosition(2, 32, 48);
      this.meteor$reglassSetSlotPosition(3, 139, 48);
   }

   @Unique
   private void meteor$reglassSpaceEnchantmentSlots() {
      this.meteor$reglassSetSlotPosition(0, 15, 52);
      this.meteor$reglassSetSlotPosition(1, 33, 52);
   }

   @Unique
   private List<int[]> meteor$reglassRemoveContainedGroups(List<int[]> groups) {
      ArrayList<int[]> result = new ArrayList<>();

      for (int i = 0; i < groups.size(); i++) {
         int[] group = groups.get(i);
         boolean contained = false;

         for (int j = 0; j < groups.size(); j++) {
            if (i != j && this.meteor$reglassContainsWithPadding(groups.get(j), group)) {
               contained = true;
               break;
            }
         }

         if (!contained) {
            result.add(group);
         }
      }

      return result;
   }

   @Unique
   private boolean meteor$reglassContainsWithPadding(int[] outer, int[] inner) {
      int outerWidth = outer[2] - outer[0];
      int outerHeight = outer[3] - outer[1];
      int innerWidth = inner[2] - inner[0];
      int innerHeight = inner[3] - inner[1];
      if (outerWidth <= innerWidth && outerHeight <= innerHeight) {
         return false;
      }

      int outerPadding = outerWidth == 18 && outerHeight == 18 ? this.meteor$reglassSingleSlotPadding() : 4;
      int innerPadding = innerWidth == 18 && innerHeight == 18 ? this.meteor$reglassSingleSlotPadding() : 4;
      return outer[0] - outerPadding <= inner[0] - innerPadding
         && outer[1] - outerPadding <= inner[1] - innerPadding
         && outer[2] + outerPadding >= inner[2] + innerPadding
         && outer[3] + outerPadding >= inner[3] + innerPadding;
   }

   @Unique
   private void meteor$reglassSetSlotPosition(int index, int x, int y) {
      if (index >= 0 && index < this.getMenu().slots.size()) {
         this.meteor$reglassSetSlotPosition(this.getMenu().slots.get(index), x, y);
      }
   }

   @Unique
   private void meteor$reglassSetSlotPosition(Slot slot, int x, int y) {
      ((SlotAccessor)slot).meteor$setX(x);
      ((SlotAccessor)slot).meteor$setY(y);
   }

   @Unique
   private boolean meteor$reglassIsPlayerInventory() {
      return (Object)this instanceof InventoryScreen;
   }
}
