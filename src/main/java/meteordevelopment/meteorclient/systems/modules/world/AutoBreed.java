package meteordevelopment.meteorclient.systems.modules.world;

import java.util.LinkedHashMap;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.EntityHitResult;

public class AutoBreed extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(
         new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to breed.")
            .defaultValue(
               EntityType.HORSE,
               EntityType.DONKEY,
               EntityType.COW,
               EntityType.MOOSHROOM,
               EntityType.SHEEP,
               EntityType.PIG,
               EntityType.CHICKEN,
               EntityType.WOLF,
               EntityType.CAT,
               EntityType.OCELOT,
               EntityType.RABBIT,
               EntityType.LLAMA,
               EntityType.TURTLE,
               EntityType.PANDA,
               EntityType.FOX,
               EntityType.BEE,
               EntityType.STRIDER,
               EntityType.HOGLIN
            )
            .onlyAttackable()
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("How far away the animals can be to be bred.").min(0.0).defaultValue(4.5).build());
   private final Setting<InteractionHand> hand = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("hand-for-breeding"))
                  .description("The hand to use for breeding."))
               .defaultValue(InteractionHand.MAIN_HAND))
            .build()
      );
   private final Setting<AutoBreed.EntityAge> mobAgeFilter = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mob-age-filter"))
                  .description("Determines the age of the mobs to target (baby, adult, or both)."))
               .defaultValue(AutoBreed.EntityAge.Adult))
            .build()
      );
   private final Setting<Boolean> continuousBreeding = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("continuous-breeding")
            .description("Whether to feed the same animal again after a certain time period.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Integer> breedingInterval = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("breeding-interval")
            .description("Determines how often the same animal is fed in ticks.")
            .min(1)
            .sliderMax(24000)
            .defaultValue(6600)
            .visible(this.continuousBreeding::get)
            .build()
      );
   private final LinkedHashMap<Entity, Integer> animalsFed = new LinkedHashMap<>();
   private int tickCounter = 0;

   public AutoBreed() {
      super(Categories.World, "auto-breed", "Automatically breeds specified animals.");
   }

   @Override
   public void onActivate() {
      this.animalsFed.clear();
      this.tickCounter = 0;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof Animal animal
            && this.entities.get().contains(animal.getType())
            && this.isCorrectAge(animal)
            && !this.animalsFed.containsKey(animal)
            && PlayerUtils.isWithin(animal, this.range.get())
            && animal.isFood(this.hand.get() == InteractionHand.MAIN_HAND ? this.mc.player.getMainHandItem() : this.mc.player.getOffhandItem())) {
            Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, () -> {
               EntityHitResult location = new EntityHitResult(animal, animal.getBoundingBox().getCenter());
               this.mc.gameMode.interact(this.mc.player, animal, location, this.hand.get());
               this.mc.player.swing(this.hand.get());
               this.animalsFed.putLast(animal, this.tickCounter);
            });
            break;
         }
      }

      if (this.continuousBreeding.get()) {
         while (!this.animalsFed.isEmpty() && this.animalsFed.firstEntry().getValue() < this.tickCounter - this.breedingInterval.get()) {
            this.animalsFed.pollFirstEntry();
         }

         this.tickCounter++;
      }
   }

   private boolean isCorrectAge(Animal animal) {
      return switch ((AutoBreed.EntityAge)this.mobAgeFilter.get()) {
         case Baby -> animal.isBaby();
         case Adult -> !animal.isBaby();
         case Both -> true;
      };
   }

   public enum EntityAge {
      Baby,
      Adult,
      Both;
   }
}
