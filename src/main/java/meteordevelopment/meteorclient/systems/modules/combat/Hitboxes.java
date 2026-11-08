package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.Set;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;

public class Hitboxes extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgWeapon = this.settings.createGroup("Weapon Options");
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("entities").description("Which entities to target.").defaultValue(EntityType.PLAYER).build());
   private final Setting<Double> value = this.sgGeneral
      .add(new DoubleSetting.Builder().name("expand").description("How much to expand the hitbox of the entity.").defaultValue(0.5).build());
   private final Setting<Boolean> ignoreFriends = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-friends").description("Doesn't expand the hitboxes of friends.").defaultValue(true).build());
   private final Setting<Boolean> onlyOnWeapon = this.sgWeapon
      .add(new BoolSetting.Builder().name("only-on-weapon").description("Only modifies hitbox when holding a weapon in hand.").defaultValue(false).build());
   private final Setting<Boolean> sword = this.sgWeapon
      .add(new BoolSetting.Builder().name("sword").description("Enable when holding a sword.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> axe = this.sgWeapon
      .add(new BoolSetting.Builder().name("axe").description("Enable when holding an axe.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> pickaxe = this.sgWeapon
      .add(new BoolSetting.Builder().name("pickaxe").description("Enable when holding a pickaxe.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> shovel = this.sgWeapon
      .add(new BoolSetting.Builder().name("shovel").description("Enable when holding a shovel.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> hoe = this.sgWeapon
      .add(new BoolSetting.Builder().name("hoe").description("Enable when holding a hoe.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> mace = this.sgWeapon
      .add(new BoolSetting.Builder().name("mace").description("Enable when holding a mace.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> spear = this.sgWeapon
      .add(new BoolSetting.Builder().name("spear").description("Enable when holding a spear.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> trident = this.sgWeapon
      .add(new BoolSetting.Builder().name("trident").description("Enable when holding a trident.").defaultValue(true).visible(this.onlyOnWeapon::get).build());

   public Hitboxes() {
      super(Categories.Combat, "hitboxes", "Expands an entity's hitboxes.");
   }

   public double getEntityValue(Entity entity) {
      if (!this.isActive() || !this.testWeapon() || this.ignoreFriends.get() && entity instanceof Player playerEntity && Friends.get().isFriend(playerEntity)) {
         return 0.0;
      } else {
         return this.entities.get().contains(entity.getType()) ? this.value.get() : 0.0;
      }
   }

   private boolean testWeapon() {
      return !this.onlyOnWeapon.get() ? true : InvUtils.testInMainHand(itemStack -> {
         if (this.sword.get() && itemStack.is(ItemTags.SWORDS)) {
            return true;
         } else if (this.axe.get() && itemStack.is(ItemTags.AXES)) {
            return true;
         } else if (this.pickaxe.get() && itemStack.is(ItemTags.PICKAXES)) {
            return true;
         } else if (this.shovel.get() && itemStack.is(ItemTags.SHOVELS)) {
            return true;
         } else if (this.hoe.get() && itemStack.is(ItemTags.HOES)) {
            return true;
         } else if (this.mace.get() && itemStack.getItem() instanceof MaceItem) {
            return true;
         } else {
            return this.spear.get() && itemStack.is(ItemTags.SPEARS) ? true : this.trident.get() && itemStack.getItem() instanceof TridentItem;
         }
      });
   }
}
