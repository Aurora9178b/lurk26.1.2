package meteordevelopment.meteorclient.systems.modules.player;

import java.util.Set;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class NoMiningTrace extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("blacklisted-entities").description("Entities you will interact with as normal.").defaultValue().build());
   private final Setting<Boolean> onlyWhenHoldingPickaxe = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("only-when-holding-a-pickaxe")
            .description("Whether or not to work only when holding a pickaxe.")
            .defaultValue(true)
            .build()
      );

   public NoMiningTrace() {
      super(Categories.Player, "no-mining-trace", "Allows you to mine blocks through entities.");
   }

   public boolean canWork(Entity entity) {
      return !this.isActive()
         ? false
         : (
               !this.onlyWhenHoldingPickaxe.get()
                  || this.mc.player.getMainHandItem().is(ItemTags.PICKAXES)
                  || this.mc.player.getOffhandItem().is(ItemTags.PICKAXES)
            )
            && (entity == null || !this.entities.get().contains(entity.getType()));
   }
}
