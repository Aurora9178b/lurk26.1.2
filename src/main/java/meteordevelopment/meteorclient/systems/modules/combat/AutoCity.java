package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class AutoCity extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(5.5)
            .min(0.0)
            .sliderMax(7.0)
            .build()
      );
   private final Setting<Double> breakRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("break-range")
            .description("How close a block must be to you to be considered.")
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<AutoCity.SwitchMode> switchMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("switch-mode"))
                  .description("How to switch to a pickaxe."))
               .defaultValue(AutoCity.SwitchMode.Normal))
            .build()
      );
   private final Setting<Boolean> support = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("support")
            .description("If there is no block below a city block it will place one before mining.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Double> placeRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("place-range")
            .description("How far away to try and place a block.")
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .visible(this.support::get)
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(true).build());
   private final Setting<Boolean> chatInfo = this.sgGeneral
      .add(new BoolSetting.Builder().name("chat-info").description("Whether the module should send messages in chat.").defaultValue(true).build());
   private final Setting<Boolean> swingHand = this.sgRender
      .add(new BoolSetting.Builder().name("swing-hand").description("Whether to render your hand swinging.").defaultValue(false).build());
   private final Setting<Boolean> renderBlock = this.sgRender
      .add(new BoolSetting.Builder().name("render-block").description("Whether to render the block being broken.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                     .description("How the shapes are rendered."))
                  .defaultValue(ShapeMode.Both))
               .visible(this.renderBlock::get))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the rendering.")
            .defaultValue(new SettingColor(225, 0, 0, 75))
            .visible(() -> this.renderBlock.get() && this.shapeMode.get().sides())
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the rendering.")
            .defaultValue(new SettingColor(225, 0, 0, 255))
            .visible(() -> this.renderBlock.get() && this.shapeMode.get().lines())
            .build()
      );
   private Player target;
   private BlockPos targetPos;
   private FindItemResult pick;
   private float progress;

   public AutoCity() {
      super(Categories.Combat, "auto-city", "Automatically mine blocks next to someone's feet.");
   }

   @Override
   public void onActivate() {
      this.target = TargetUtils.getPlayerTarget(this.targetRange.get(), SortPriority.ClosestAngle);
      if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
         if (this.chatInfo.get()) {
            this.error("Couldn't find a target, disabling.");
         }

         this.toggle();
      } else {
         this.targetPos = EntityUtils.getCityBlock(this.target);
         if (this.targetPos != null && !(PlayerUtils.squaredDistanceTo(this.targetPos) > Math.pow(this.breakRange.get(), 2.0))) {
            if (this.support.get()) {
               BlockPos supportPos = this.targetPos.below();
               if (PlayerUtils.squaredDistanceTo(supportPos) <= Math.pow(this.placeRange.get(), 2.0)) {
                  BlockUtils.place(supportPos, InvUtils.findInHotbar(Items.OBSIDIAN), this.rotate.get(), 0, true);
               }
            }

            this.pick = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
            if (!this.pick.isHotbar()) {
               this.error("No pickaxe found... disabling.");
               this.toggle();
            } else {
               this.progress = 0.0F;
               this.mine(false);
            }
         } else {
            if (this.chatInfo.get()) {
               this.error("Couldn't find a good block, disabling.");
            }

            this.toggle();
         }
      }
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      this.targetPos = null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
         this.toggle();
      } else if (PlayerUtils.squaredDistanceTo(this.targetPos) > Math.pow(this.breakRange.get(), 2.0)) {
         if (this.chatInfo.get()) {
            this.error("Couldn't find a target, disabling.");
         }

         this.toggle();
      } else {
         if (this.progress < 1.0F) {
            this.pick = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
            if (!this.pick.isHotbar()) {
               this.error("No pickaxe found... disabling.");
               this.toggle();
               return;
            }

            this.progress = (float)(this.progress + BlockUtils.getBreakDelta(this.pick.slot(), this.mc.level.getBlockState(this.targetPos)));
            if (this.progress < 1.0F) {
               return;
            }
         }

         this.mine(true);
         this.toggle();
      }
   }

   public void mine(boolean done) {
      InvUtils.swap(this.pick.slot(), this.switchMode.get() == AutoCity.SwitchMode.Silent);
      if (this.rotate.get()) {
         Rotations.rotate(Rotations.getYaw(this.targetPos), Rotations.getPitch(this.targetPos));
      }

      Direction direction = BlockUtils.getDirection(this.targetPos);
      if (!done) {
         this.mc
            .gameMode
            .startPrediction(this.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, this.targetPos, direction, sequence));
      }

      this.mc
         .gameMode
         .startPrediction(this.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, this.targetPos, direction, sequence));
      if (this.swingHand.get()) {
         this.mc.player.swing(InteractionHand.MAIN_HAND);
      } else {
         this.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      }

      if (this.switchMode.get() == AutoCity.SwitchMode.Silent) {
         InvUtils.swapBack();
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.targetPos != null && this.renderBlock.get()) {
         event.renderer.box(this.targetPos, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
      }
   }

   @Override
   public String getInfoString() {
      return EntityUtils.getName(this.target);
   }

   public enum SwitchMode {
      Normal,
      Silent;
   }
}
