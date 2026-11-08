package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BedAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTargeting = this.settings.createGroup("Targeting");
   private final SettingGroup sgAutoMove = this.settings.createGroup("Inventory");
   private final SettingGroup sgPause = this.settings.createGroup("Pause");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("The delay between placing beds in ticks.").defaultValue(9).min(0).sliderMax(20).build());
   private final Setting<Boolean> strictDirection = this.sgGeneral
      .add(new BoolSetting.Builder().name("strict-direction").description("Only places beds in the direction you are facing.").defaultValue(false).build());
   private final Setting<Double> targetRange = this.sgTargeting
      .add(
         new DoubleSetting.Builder()
            .name("target-range")
            .description("The range at which players can be targeted.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderMax(5.0)
            .build()
      );
   private final Setting<SortPriority> priority = this.sgTargeting
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to filter targets within range."))
               .defaultValue(SortPriority.LowestHealth))
            .build()
      );
   private final Setting<Double> minDamage = this.sgTargeting
      .add(
         new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage to inflict on your target.")
            .defaultValue(7.0)
            .range(0.0, 36.0)
            .sliderMax(36.0)
            .build()
      );
   private final Setting<Double> maxSelfDamage = this.sgTargeting
      .add(
         new DoubleSetting.Builder()
            .name("max-self-damage")
            .description("The maximum damage to inflict on yourself.")
            .defaultValue(7.0)
            .range(0.0, 36.0)
            .sliderMax(36.0)
            .build()
      );
   private final Setting<Boolean> antiSuicide = this.sgTargeting
      .add(new BoolSetting.Builder().name("anti-suicide").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
   private final Setting<Boolean> autoMove = this.sgAutoMove
      .add(new BoolSetting.Builder().name("auto-move").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());
   private final Setting<Integer> autoMoveSlot = this.sgAutoMove
      .add(
         new IntSetting.Builder()
            .name("auto-move-slot")
            .description("The slot auto move moves beds to.")
            .defaultValue(9)
            .range(1, 9)
            .sliderRange(1, 9)
            .visible(this.autoMove::get)
            .build()
      );
   private final Setting<Boolean> autoSwitch = this.sgAutoMove
      .add(new BoolSetting.Builder().name("auto-switch").description("Switches to and from beds automatically.").defaultValue(true).build());
   private final Setting<Boolean> pauseOnEat = this.sgPause
      .add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
   private final Setting<Boolean> pauseOnDrink = this.sgPause
      .add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(true).build());
   private final Setting<Boolean> pauseOnMine = this.sgPause
      .add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining.").defaultValue(true).build());
   private final Setting<Boolean> swing = this.sgRender
      .add(new BoolSetting.Builder().name("swing").description("Whether to swing hand client-side.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders the block where it is placing a bed.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The side color for positions to be placed.")
            .defaultValue(new SettingColor(15, 255, 211, 75))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color for positions to be placed.")
            .defaultValue(new SettingColor(15, 255, 211))
            .build()
      );
   private CardinalDirection direction;
   private Player target;
   private BlockPos placePos;
   private BlockPos breakPos;
   private int timer;

   public BedAura() {
      super(Categories.Combat, "bed-aura", "Automatically places and explodes beds in the Nether and End.");
   }

   @Override
   public void onActivate() {
      this.timer = this.delay.get();
      this.direction = CardinalDirection.North;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.level.dimension() == Level.OVERWORLD) {
         this.error("You can't blow up beds in this dimension, disabling.");
         this.toggle();
      } else if (!PlayerUtils.shouldPause(this.pauseOnMine.get(), this.pauseOnEat.get(), this.pauseOnDrink.get())) {
         this.target = TargetUtils.getPlayerTarget(this.targetRange.get(), this.priority.get());
         if (this.target == null) {
            this.placePos = null;
            this.breakPos = null;
         } else {
            if (this.autoMove.get()) {
               FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
               if (bed.found() && bed.slot() != this.autoMoveSlot.get() - 1) {
                  InvUtils.move().from(bed.slot()).toHotbar(this.autoMoveSlot.get() - 1);
               }
            }

            if (this.breakPos == null) {
               this.placePos = this.findPlace(this.target);
            }

            if (this.timer <= 0 && this.placeBed(this.placePos)) {
               this.timer = this.delay.get();
            } else {
               this.timer--;
            }

            if (this.breakPos == null) {
               this.breakPos = this.findBreak();
            }

            this.breakBed(this.breakPos);
         }
      }
   }

   private BlockPos findPlace(Player target) {
      if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) {
         return null;
      }

      for (int index = 0; index < 3; index++) {
         int i = index == 0 ? 1 : (index == 1 ? 0 : 2);

         for (CardinalDirection dir : CardinalDirection.values()) {
            if (!this.strictDirection.get()
               || dir.toDirection() == this.mc.player.getDirection()
               || dir.toDirection().getOpposite() == this.mc.player.getDirection()) {
               BlockPos centerPos = target.blockPosition().above(i);
               float headSelfDamage = DamageUtils.bedDamage(this.mc.player, Utils.vec3(centerPos));
               float offsetSelfDamage = DamageUtils.bedDamage(this.mc.player, Utils.vec3(centerPos.relative(dir.toDirection())));
               if (this.mc.level.getBlockState(centerPos).canBeReplaced()
                  && BlockUtils.canPlace(centerPos.relative(dir.toDirection()))
                  && DamageUtils.bedDamage(target, Utils.vec3(centerPos)) >= this.minDamage.get()
                  && offsetSelfDamage < this.maxSelfDamage.get()
                  && headSelfDamage < this.maxSelfDamage.get()
                  && (!this.antiSuicide.get() || PlayerUtils.getTotalHealth() - headSelfDamage > 0.0F)
                  && (!this.antiSuicide.get() || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0.0F)) {
                  return centerPos.relative((this.direction = dir).toDirection());
               }
            }
         }
      }

      return null;
   }

   private BlockPos findBreak() {
      for (BlockEntity blockEntity : Utils.blockEntities()) {
         if (blockEntity instanceof BedBlockEntity) {
            BlockPos bedPos = blockEntity.getBlockPos();
            Vec3 bedVec = Utils.vec3(bedPos);
            if (PlayerUtils.isWithinReach(bedVec)
               && DamageUtils.bedDamage(this.target, bedVec) >= this.minDamage.get()
               && DamageUtils.bedDamage(this.mc.player, bedVec) < this.maxSelfDamage.get()
               && (!this.antiSuicide.get() || PlayerUtils.getTotalHealth() - DamageUtils.bedDamage(this.mc.player, bedVec) > 0.0F)) {
               return bedPos;
            }
         }
      }

      return null;
   }

   private boolean placeBed(BlockPos pos) {
      if (pos == null) {
         return false;
      }

      FindItemResult bed = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
      if (bed.getHand() == null && !this.autoSwitch.get()) {
         return false;
      }

      double yaw = switch (this.direction) {
         case East -> 90.0;
         case South -> 180.0;
         case West -> -90.0;
         default -> 0.0;
      };
      Rotations.rotate(yaw, Rotations.getPitch(pos), () -> {
         BlockUtils.place(pos, bed, false, 0, this.swing.get(), true);
         this.breakPos = pos;
      });
      return true;
   }

   private void breakBed(BlockPos pos) {
      if (pos != null) {
         this.breakPos = null;
         if (this.mc.level.getBlockState(pos).getBlock() instanceof BedBlock) {
            boolean wasSneaking = this.mc.player.isShiftKeyDown();
            if (wasSneaking) {
               this.mc.player.setShiftKeyDown(false);
            }

            this.mc.gameMode.useItemOn(this.mc.player, InteractionHand.OFF_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            this.mc.player.setShiftKeyDown(wasSneaking);
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get() && this.placePos != null && this.breakPos == null) {
         int x = this.placePos.getX();
         int y = this.placePos.getY();
         int z = this.placePos.getZ();
         switch (this.direction) {
            case East:
               event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
               break;
            case South:
               event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
               break;
            case West:
               event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
               break;
            case North:
               event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
         }
      }
   }

   @Override
   public String getInfoString() {
      return EntityUtils.getName(this.target);
   }
}
