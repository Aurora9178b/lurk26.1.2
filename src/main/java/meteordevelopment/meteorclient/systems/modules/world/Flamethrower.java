package meteordevelopment.meteorclient.systems.modules.world;

import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.HorizontalDirection;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Flamethrower extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> distance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("distance")
            .description("The maximum distance the animal has to be to be roasted.")
            .min(0.0)
            .defaultValue(5.0)
            .build()
      );
   private final Setting<Boolean> antiBreak = this.sgGeneral
      .add(new BoolSetting.Builder().name("anti-break").description("Prevents flint and steel from being broken.").defaultValue(false).build());
   private final Setting<Boolean> putOutFire = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("put-out-fire")
            .description("Tries to put out the fire when animal is low health, so the items don't burn.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> targetBabies = this.sgGeneral
      .add(new BoolSetting.Builder().name("target-babies").description("If checked babies will also be killed.").defaultValue(false).build());
   private final Setting<Integer> tickInterval = this.sgGeneral.add(new IntSetting.Builder().name("tick-interval").defaultValue(5).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Automatically faces towards the animal roasted.").defaultValue(true).build());
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(
         new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to cook.")
            .defaultValue(EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN, EntityType.RABBIT)
            .build()
      );
   private Entity entity;
   private int ticks = 0;
   private InteractionHand hand;

   public Flamethrower() {
      super(Categories.World, "flamethrower", "Ignites every alive piece of food.");
   }

   @Override
   public void onDeactivate() {
      this.entity = null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.entity = null;
      this.ticks++;

      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (this.entities.get().contains(entity.getType())
            && PlayerUtils.isWithin(entity, this.distance.get())
            && entity != this.mc.player
            && entity.isAlive()
            && !entity.isInPowderSnow
            && !entity.isInWaterOrRain()
            && !entity.fireImmune()
            && (this.targetBabies.get() || !(entity instanceof LivingEntity livingEntity) || !livingEntity.isBaby())) {
            FindItemResult item = InvUtils.findInHotbar(
               itemStack -> (itemStack.is(Items.FLINT_AND_STEEL) || itemStack.is(Items.FIRE_CHARGE))
                  && (!itemStack.isDamageableItem() || !this.antiBreak.get() || itemStack.getDamageValue() < itemStack.getMaxDamage() - 1)
            );
            if (!InvUtils.swap(item.slot(), true)) {
               return;
            }

            this.hand = item.getHand();
            this.entity = entity;
            if (this.rotate.get()) {
               Rotations.rotate(Rotations.getYaw(entity.blockPosition()), Rotations.getPitch(entity.blockPosition()), -100, this::interact);
            } else {
               this.interact();
            }

            return;
         }
      }
   }

   private void interact() {
      Block block = this.mc.level.getBlockState(this.entity.blockPosition()).getBlock();
      Block bottom = this.mc.level.getBlockState(this.entity.blockPosition().below()).getBlock();
      if (block != Blocks.WATER && bottom != Blocks.WATER && bottom != Blocks.DIRT_PATH) {
         if (block == Blocks.GRASS_BLOCK) {
            this.mc.gameMode.startDestroyBlock(this.entity.blockPosition(), Direction.DOWN);
         }

         if (this.putOutFire.get() && this.entity instanceof LivingEntity animal && animal.getHealth() < 2.0F) {
            this.mc.gameMode.startDestroyBlock(this.entity.blockPosition(), Direction.DOWN);

            for (HorizontalDirection direction : HorizontalDirection.values()) {
               this.mc.gameMode.startDestroyBlock(this.entity.blockPosition().offset(direction.offsetX, 0, direction.offsetZ), Direction.DOWN);
            }
         } else if (this.ticks >= this.tickInterval.get() && !this.entity.isOnFire()) {
            this.mc
               .gameMode
               .useItemOn(
                  this.mc.player,
                  this.hand,
                  new BlockHitResult(this.entity.position().subtract(new Vec3(0.0, 1.0, 0.0)), Direction.UP, this.entity.blockPosition().below(), false)
               );
            this.ticks = 0;
         }

         InvUtils.swapBack();
      }
   }
}
