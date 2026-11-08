package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class AutoRun extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTP = this.settings.createGroup("TP");
   private final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("Search range for targets.").defaultValue(100.0).min(0.0).max(1.0E7).sliderMax(100.0).build());
   private final Setting<Boolean> player = this.sgGeneral
      .add(new BoolSetting.Builder().name("player").description("Runs when a player is nearby.").defaultValue(false).build());
   private final Setting<Boolean> twoTotem = this.sgGeneral
      .add(new BoolSetting.Builder().name("two-totem").description("Runs when a nearby player holds two totems.").defaultValue(false).build());
   private final Setting<Boolean> mace = this.sgGeneral
      .add(new BoolSetting.Builder().name("mace").description("Runs when a nearby player holds a mace.").defaultValue(false).build());
   private final Setting<Boolean> spear = this.sgGeneral
      .add(new BoolSetting.Builder().name("spear").description("Runs when a nearby player holds a spear.").defaultValue(false).build());
   private final Setting<Boolean> crystal = this.sgGeneral
      .add(new BoolSetting.Builder().name("crystal").description("Runs when a nearby player holds an end crystal.").defaultValue(false).build());
   private final Setting<Double> crystalRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("crystal-range")
            .description("Search range for crystal mode.")
            .defaultValue(100.0)
            .min(0.0)
            .max(1.0E7)
            .sliderMax(100.0)
            .visible(this.crystal::get)
            .build()
      );
   private final Setting<Boolean> anchor = this.sgGeneral
      .add(new BoolSetting.Builder().name("anchor").description("Runs when a nearby player holds a respawn anchor.").defaultValue(false).build());
   private final Setting<Double> anchorRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("anchor-range")
            .description("Search range for anchor mode.")
            .defaultValue(100.0)
            .min(0.0)
            .max(1.0E7)
            .sliderMax(100.0)
            .visible(this.anchor::get)
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-friends").description("Ignore friends when searching for targets.").defaultValue(true).build());
   private final Setting<Boolean> ignoreFakePlayers = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-fake-players").description("Ignore fake players when searching for targets.").defaultValue(true).build());
   private final Setting<Double> vclipHeight = this.sgTP
      .add(new DoubleSetting.Builder().name("vclip").description("VClip height per teleport.").defaultValue(30.0).min(1.0).sliderRange(1.0, 100.0).build());

   public AutoRun() {
      super(Categories.Movement, "AutoRun", "Automatically vclips away when selected threats are nearby.");
   }

   @EventHandler(priority = 1199)
   private void onTickPre(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (this.shouldTrigger()) {
            Vec3 from = this.mc.player.position();
            Vec3 to = from.add(0.0, this.vclipHeight.get(), 0.0);
            this.doTp(from, to);
            this.mc.player.setPos(to);
         }
      }
   }

   private boolean shouldTrigger() {
      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof Player target
            && target != this.mc.player
            && target.isAlive()
            && (!this.ignoreFriends.get() || Friends.get().shouldAttack(target))
            && (!this.ignoreFakePlayers.get() || !(target instanceof FakePlayerEntity))) {
            if (this.player.get() && this.isInRange(target, this.range.get())) {
               return true;
            }

            if (this.twoTotem.get() && this.isInRange(target, this.range.get()) && this.hasTwoTotems(target)) {
               return true;
            }

            if (this.mace.get() && this.isInRange(target, this.range.get()) && this.hasMace(target)) {
               return true;
            }

            if (this.spear.get() && this.isInRange(target, this.range.get()) && this.hasSpear(target)) {
               return true;
            }

            if (this.crystal.get() && this.isInRange(target, this.crystalRange.get()) && this.hasCrystal(target)) {
               return true;
            }

            if (this.anchor.get() && this.isInRange(target, this.anchorRange.get()) && this.hasAnchor(target)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isInRange(Player target, double range) {
      return this.mc.player.distanceTo(target) <= range;
   }

   private boolean hasTwoTotems(Player target) {
      return target.getMainHandItem().is(Items.TOTEM_OF_UNDYING) && target.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
   }

   private boolean hasMace(Player target) {
      return target.getMainHandItem().is(Items.MACE) || target.getOffhandItem().is(Items.MACE);
   }

   private boolean hasSpear(Player target) {
      return target.getMainHandItem().is(ItemTags.SPEARS) || target.getOffhandItem().is(ItemTags.SPEARS);
   }

   private boolean hasCrystal(Player target) {
      return target.getMainHandItem().is(Items.END_CRYSTAL) || target.getOffhandItem().is(Items.END_CRYSTAL);
   }

   private boolean hasAnchor(Player target) {
      return target.getMainHandItem().is(Items.RESPAWN_ANCHOR) || target.getOffhandItem().is(Items.RESPAWN_ANCHOR);
   }

   private void doTp(Vec3 from, Vec3 to) {
      TpSetting setting = TpSetting.get();
      double moveDistance = setting != null ? setting.getMoveDistance() : 128.0;
      if (setting != null && setting.isQTPUtils()) {
         QTPUtils.doTp(from, to, moveDistance);
      } else {
         LTpUtils.doTp(from, to, moveDistance);
      }
   }
}
