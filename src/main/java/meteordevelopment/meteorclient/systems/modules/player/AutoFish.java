package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.FishingHookAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.projectile.FishingHook.FishHookState;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

public class AutoFish extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> autoSwitch = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-switch").description("Automatically switch to a fishing rod.").defaultValue(true).build());
   private final Setting<Boolean> antiBreak = this.sgGeneral
      .add(new BoolSetting.Builder().name("anti-break").description("Avoid using rods that would break if they were cast.").defaultValue(true).build());
   private final Setting<Boolean> autoCast = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-cast").description("Automatically cast the fishing rod.").defaultValue(true).build());
   private final Setting<Integer> castDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("cast-delay")
            .description("How long to wait between recasts if the bobber fails to land in water.")
            .defaultValue(14)
            .min(1)
            .sliderMax(60)
            .build()
      );
   private final Setting<Integer> castDelayVariance = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("cast-delay-variance")
            .description("Maximum amount of randomness added to cast delay.")
            .defaultValue(0)
            .min(0)
            .sliderMax(30)
            .build()
      );
   private final Setting<Integer> catchDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("catch-delay")
            .description("How long to wait after hooking a fish to reel it in.")
            .defaultValue(6)
            .min(1)
            .sliderMax(20)
            .build()
      );
   private final Setting<Integer> catchDelayVariance = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("catch-delay-variance")
            .description("Maximum amount of randomness added to catch delay.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .build()
      );
   private double castDelayLeft = 0.0;
   private double catchDelayLeft = 0.0;
   private boolean wasHooked = false;

   public AutoFish() {
      super(Categories.Player, "auto-fish", "Automatically fishes for you.");
   }

   @Override
   public void onActivate() {
      this.castDelayLeft = 0.0;
      this.catchDelayLeft = 0.0;
      this.wasHooked = false;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      int bestRodSlot = this.findBestRod();
      if (this.autoSwitch.get() && bestRodSlot != -1 && this.mc.player.getInventory().getSelectedSlot() != bestRodSlot) {
         InvUtils.swap(bestRodSlot, false);
      }

      if (this.mc.player.getMainHandItem().getItem() instanceof FishingRodItem) {
         this.tryCast();
         this.tryCatch();
      }
   }

   private void tryCast() {
      if (this.mc.player.fishing == null) {
         if (this.autoCast.get()) {
            if (this.castDelayLeft > 0.0) {
               this.castDelayLeft = this.castDelayLeft - TickRate.INSTANCE.getTickRate() / 20.0;
            } else {
               this.useRod();
            }
         }
      }
   }

   private void tryCatch() {
      if (this.mc.player.fishing != null) {
         if (this.mc.player.fishing.getHookedIn() != null) {
            this.useRod();
         } else if (this.mc.player.fishing.currentState == FishHookState.BOBBING) {
            if (!this.wasHooked) {
               if (((FishingHookAccessor)this.mc.player.fishing).meteor$hasCaughtFish()) {
                  this.catchDelayLeft = this.randomizeDelay(this.catchDelay.get(), this.catchDelayVariance.get());
                  this.wasHooked = true;
               }
            } else if (this.catchDelayLeft > 0.0) {
               this.catchDelayLeft = this.catchDelayLeft - TickRate.INSTANCE.getTickRate() / 20.0;
            } else {
               this.useRod();
            }
         }
      }
   }

   private void useRod() {
      Utils.rightClick();
      this.wasHooked = false;
      this.castDelayLeft = this.randomizeDelay(this.castDelay.get(), this.castDelayVariance.get());
   }

   private int findBestRod() {
      int bestSlot = -1;
      int bestScore = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof FishingRodItem && (!this.antiBreak.get() || stack.getDamageValue() != stack.getMaxDamage() - 1)) {
            int score = 0;
            score += Utils.getEnchantmentLevel(stack, Enchantments.LUCK_OF_THE_SEA);
            score += Utils.getEnchantmentLevel(stack, Enchantments.LURE);
            score += Utils.getEnchantmentLevel(stack, Enchantments.MENDING);
            score += Utils.getEnchantmentLevel(stack, Enchantments.UNBREAKING);
            if (score > bestScore) {
               bestScore = score;
               bestSlot = i;
            }

            if (score == 10) {
               break;
            }
         }
      }

      return bestSlot;
   }

   private double randomizeDelay(int delay, int variance) {
      if (variance == 0) {
         return delay;
      }

      double scale = Math.sqrt(-2.0 * Math.log(Utils.random(1.0E-4, 1.0)));
      double angle = (Math.PI * 2) * Utils.random(0.0, 1.0);
      double norm = scale * Math.cos(angle);
      double MAX_SD = 3.0;
      norm = Math.clamp(norm, -3.0, 3.0) / 3.0;
      delay += Math.round((float)(norm * variance));
      return Math.max(1, delay);
   }
}
