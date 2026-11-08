package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BowSpam extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgCrossbows = this.settings.createGroup("Crossbows");
   private final Setting<Integer> charge = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("charge")
            .description("How long to charge the bow before releasing in ticks.")
            .defaultValue(5)
            .range(4, 20)
            .sliderRange(4, 20)
            .build()
      );
   private final Setting<Boolean> onlyWhenHoldingRightClick = this.sgGeneral
      .add(new BoolSetting.Builder().name("when-holding-right-click").description("Works only when holding right click.").defaultValue(false).build());
   private final Setting<Boolean> spamCrossbows = this.sgCrossbows
      .add(
         new BoolSetting.Builder()
            .name("spam-crossbows")
            .description("Whether to spam loaded crossbows; takes priority over charging bows.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Integer> crossbowDelay = this.sgCrossbows
      .add(
         new IntSetting.Builder()
            .name("crossbow-delay")
            .description("Delay between shooting crossbows in ticks.")
            .defaultValue(10)
            .sliderRange(0, 20)
            .min(0)
            .build()
      );
   private final Setting<Boolean> searchInventory = this.sgCrossbows
      .add(
         new BoolSetting.Builder()
            .name("search-inventory")
            .description("Whether to search your inventory to find loaded crossbows.")
            .defaultValue(true)
            .build()
      );
   private boolean wasBow = false;
   private boolean wasHoldingRightClick = false;
   private int ticks = 0;

   public BowSpam() {
      super(Categories.Combat, "bow-spam", "Spams bows and crossbows.", "auto-bow", "crossbow-spam", "auto-crossbow");
   }

   @Override
   public void onActivate() {
      this.wasBow = false;
      this.wasHoldingRightClick = false;
   }

   @Override
   public void onDeactivate() {
      this.setPressed(false);
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      FindItemResult crossbow = this.searchInventory.get() ? InvUtils.find(this::crossbow) : InvUtils.find(this::crossbow, 0, 8);
      if (this.spamCrossbows.get() && crossbow.found()) {
         if (this.ticks >= this.crossbowDelay.get()) {
            int slot = crossbow.slot();
            if (!crossbow.isHotbar()) {
               FindItemResult valid = InvUtils.find(stack -> stack.isEmpty() || stack.is(Items.CROSSBOW) || stack.is(Items.ARROW), 0, 8);
               if (!valid.found()) {
                  return;
               }

               InvUtils.quickSwap().fromId(valid.slot()).to(crossbow.slot());
               slot = valid.slot();
            }

            InvUtils.swap(slot, true);
            this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
            InvUtils.swapBack();
            this.ticks = 0;
         } else {
            this.ticks++;
         }
      } else if (this.mc.player.getAbilities().instabuild || InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem).found()) {
         if (!this.onlyWhenHoldingRightClick.get() || this.mc.options.keyUse.isDown()) {
            boolean isBow = InvUtils.testInHands(Items.BOW);
            if (!isBow && this.wasBow) {
               this.setPressed(false);
            }

            this.wasBow = isBow;
            if (!isBow) {
               return;
            }

            if (this.mc.player.getTicksUsingItem() >= this.charge.get()) {
               this.mc.gameMode.releaseUsingItem(this.mc.player);
            } else {
               this.setPressed(true);
            }

            this.wasHoldingRightClick = this.mc.options.keyUse.isDown();
         } else if (this.wasHoldingRightClick) {
            this.setPressed(false);
            this.wasHoldingRightClick = false;
         }
      }
   }

   private void setPressed(boolean pressed) {
      this.mc.options.keyUse.setDown(pressed);
   }

   private boolean crossbow(ItemStack stack) {
      return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
   }
}
