package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.BlockBehaviourAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class Anchor extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> maxHeight = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-height")
            .description("The maximum height Anchor will work at.")
            .defaultValue(10)
            .range(0, 255)
            .sliderMax(20)
            .build()
      );
   private final Setting<Integer> minPitch = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("min-pitch")
            .description("The minimum pitch at which anchor will work.")
            .defaultValue(0)
            .range(-90, 90)
            .sliderRange(-90, 90)
            .build()
      );
   private final Setting<Boolean> cancelMove = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("cancel-jump-in-hole")
            .description("Prevents you from jumping when Anchor is active and Min Pitch is met.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> pull = this.sgGeneral
      .add(new BoolSetting.Builder().name("pull").description("The pull strength of Anchor.").defaultValue(false).build());
   private final Setting<Double> pullSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("pull-speed")
            .description("How fast to pull towards the hole in blocks per second.")
            .defaultValue(0.3)
            .min(0.0)
            .sliderMax(5.0)
            .build()
      );
   private final MutableBlockPos blockPos = new MutableBlockPos();
   private boolean wasInHole;
   private boolean foundHole;
   private int holeX;
   private int holeZ;
   public boolean cancelJump;
   public boolean controlMovement;
   public double deltaX;
   public double deltaZ;

   public Anchor() {
      super(Categories.Movement, "anchor", "Helps you get into holes by stopping your movement completely over a hole.");
   }

   @Override
   public void onActivate() {
      this.wasInHole = false;
      this.holeX = this.holeZ = 0;
   }

   @EventHandler
   private void onPreTick(TickEvent.Pre event) {
      this.cancelJump = this.foundHole && this.cancelMove.get() && this.mc.player.getXRot() >= this.minPitch.get().intValue();
   }

   @EventHandler
   private void onPostTick(TickEvent.Post event) {
      this.controlMovement = false;
      int x = Mth.floor(this.mc.player.getX());
      int y = Mth.floor(this.mc.player.getY());
      int z = Mth.floor(this.mc.player.getZ());
      if (this.isHole(x, y, z)) {
         this.wasInHole = true;
         this.holeX = x;
         this.holeZ = z;
      } else if (!this.wasInHole || this.holeX != x || this.holeZ != z) {
         if (this.wasInHole) {
            this.wasInHole = false;
         }

         if (!(this.mc.player.getXRot() < this.minPitch.get().intValue())) {
            this.foundHole = false;
            double holeX = 0.0;
            double holeZ = 0.0;

            for (int i = 0; i < this.maxHeight.get(); i++) {
               y--;
               if (y <= this.mc.level.getMinY() || !this.isAir(x, y, z)) {
                  break;
               }

               if (this.isHole(x, y, z)) {
                  this.foundHole = true;
                  holeX = x + 0.5;
                  holeZ = z + 0.5;
                  break;
               }
            }

            if (this.foundHole) {
               this.controlMovement = true;
               this.deltaX = Mth.clamp(holeX - this.mc.player.getX(), -0.05, 0.05);
               this.deltaZ = Mth.clamp(holeZ - this.mc.player.getZ(), -0.05, 0.05);
               ((IVec3)this.mc.player.getDeltaMovement())
                  .meteor$set(this.deltaX, this.mc.player.getDeltaMovement().y - (this.pull.get() ? this.pullSpeed.get() : 0.0), this.deltaZ);
            }
         }
      }
   }

   private boolean isHole(int x, int y, int z) {
      return this.isHoleBlock(x, y - 1, z)
         && this.isHoleBlock(x + 1, y, z)
         && this.isHoleBlock(x - 1, y, z)
         && this.isHoleBlock(x, y, z + 1)
         && this.isHoleBlock(x, y, z - 1);
   }

   private boolean isHoleBlock(int x, int y, int z) {
      this.blockPos.set(x, y, z);
      Block block = this.mc.level.getBlockState(this.blockPos).getBlock();
      return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN;
   }

   private boolean isAir(int x, int y, int z) {
      this.blockPos.set(x, y, z);
      return !((BlockBehaviourAccessor)this.mc.level.getBlockState(this.blockPos).getBlock()).meteor$isHasCollision();
   }
}
