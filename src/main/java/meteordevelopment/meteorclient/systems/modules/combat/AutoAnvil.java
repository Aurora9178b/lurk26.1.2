package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;

public class AutoAnvil extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderMax(5.0)
            .build()
      );
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to select the player to target."))
               .defaultValue(SortPriority.LowestHealth))
            .build()
      );
   private final Setting<Integer> height = this.sgGeneral
      .add(new IntSetting.Builder().name("height").description("The height to place anvils at.").defaultValue(2).range(0, 5).sliderMax(5).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("The delay in between anvil placements.").defaultValue(10).min(0).sliderMax(50).build());
   private final Setting<Boolean> placeButton = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("place-at-feet")
            .description("Automatically places a button or pressure plate at the targets feet to break the anvils.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> multiPlace = this.sgGeneral
      .add(new BoolSetting.Builder().name("multi-place").description("Places multiple anvils at once.").defaultValue(true).build());
   private final Setting<Boolean> toggleOnBreak = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-on-break").description("Toggles when the target's helmet slot is empty.").defaultValue(false).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position anvils/pressure plates/buttons are placed.")
            .defaultValue(true)
            .build()
      );
   private Player target;
   private int timer;

   public AutoAnvil() {
      super(Categories.Combat, "auto-anvil", "Automatically places anvils above players to destroy helmets.");
   }

   @Override
   public void onActivate() {
      this.timer = 0;
      this.target = null;
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      if (event.screen instanceof AnvilScreen) {
         event.cancel();
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.toggleOnBreak.get() && this.target != null && this.target.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
         this.error("Target head slot is empty... disabling.");
         this.toggle();
      } else {
         if (TargetUtils.isBadTarget(this.target, this.range.get())) {
            this.target = TargetUtils.getPlayerTarget(this.range.get(), this.priority.get());
            if (TargetUtils.isBadTarget(this.target, this.range.get())) {
               return;
            }
         }

         if (this.placeButton.get()) {
            FindItemResult floorBlock = InvUtils.findInHotbar(
               itemStack -> Block.byItem(itemStack.getItem()) instanceof BasePressurePlateBlock || Block.byItem(itemStack.getItem()) instanceof ButtonBlock
            );
            BlockUtils.place(this.target.blockPosition(), floorBlock, this.rotate.get(), 0, false);
         }

         if (this.timer >= this.delay.get()) {
            this.timer = 0;
            FindItemResult anvil = InvUtils.findInHotbar(itemStack -> Block.byItem(itemStack.getItem()) instanceof AnvilBlock);
            if (!anvil.found()) {
               return;
            }

            for (int i = this.height.get(); i > 1; i--) {
               BlockPos blockPos = this.target.blockPosition().above().offset(0, i, 0);
               int j = 0;

               while (j < i && this.mc.level.getBlockState(this.target.blockPosition().above(j + 1)).canBeReplaced()) {
                  j++;
               }

               if (BlockUtils.place(blockPos, anvil, this.rotate.get(), 0) && !this.multiPlace.get()) {
                  break;
               }
            }
         } else {
            this.timer++;
         }
      }
   }

   @Override
   public String getInfoString() {
      return EntityUtils.getName(this.target);
   }
}
