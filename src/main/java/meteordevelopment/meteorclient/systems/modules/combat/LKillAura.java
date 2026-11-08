package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.math.PredictUtil;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class LKillAura extends Module {
   public static LKillAura I;
   private SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private SettingGroup sgTP = this.settings.createGroup("Miss Totem VClips");
   private Entity target = null;
   private long lastAttackTime = 0L;
   private final Setting<Set<EntityType<?>>> targetTypes = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("target-type").description("Entities to attack.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("Entity sorting priority"))
               .defaultValue(SortPriority.LowestDistance))
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Max distance of a target.")
            .defaultValue(3.0)
            .min(0.1)
            .max(10.0)
            .sliderMin(0.1)
            .sliderMax(10.0)
            .build()
      );
   private final Setting<Boolean> swingHand = this.sgGeneral
      .add(new BoolSetting.Builder().name("swing-hand").description("Whether swing hand when attacking.").defaultValue(false).build());
   private final Setting<LKillAura.SwitchMode> switchMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("switch-mode")).description("How to switch to mace."))
               .defaultValue(LKillAura.SwitchMode.Silent))
            .build()
      );
   private Setting<Boolean> useCooldown = this.sgGeneral
      .add(new BoolSetting.Builder().name("Onhurt").description("Whether use the item cooldown to determine when attack.").defaultValue(true).build());
   private final Setting<Double> useCooldownBaseTime = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("Onlyhurt")
            .description("The base time for using cooldown.")
            .defaultValue(0.75)
            .min(0.4)
            .sliderMax(1.0)
            .sliderMin(0.1)
            .visible(() -> this.useCooldown.get())
            .build()
      );
   private final Setting<Integer> attackDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("attack-delay")
            .description("attack delay.")
            .defaultValue(50)
            .sliderMax(2000)
            .sliderMin(1)
            .visible(() -> !this.useCooldown.get())
            .build()
      );
   private final Setting<List<Double>> vClip1 = this.sgTP
      .add(
         new DoubleListSetting.Builder()
            .name("v-clip")
            .description("Distance of clip.")
            .defaultValue(10.0)
            .range(0.0, 1000.0)
            .sliderRange(0.0, 1000.0)
            .decimalPlaces(2)
            .onSliderRelease()
            .build()
      );
   boolean isNaked = false;

   public LKillAura() {
      super(Categories.Combat, "LKillAura", "Killaura.");
      I = this;
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.lastAttackTime = 0L;
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      this.lastAttackTime = 0L;
   }

   private boolean isReadyToAttack() {
      if (this.useCooldown.get()) {
         float cooldownProgress = this.mc.player.getAttackStrengthScale(0.5F);
         return cooldownProgress >= this.useCooldownBaseTime.get().floatValue();
      } else {
         return System.currentTimeMillis() - this.lastAttackTime >= this.attackDelay.get().intValue();
      }
   }

   @EventHandler
   public void onTick(TickEvent.Post event) {
      this.updateTarget();
      this.doAura();
   }

   private void updateTarget() {
      this.target = TargetUtils.get(this::_targetCheck, this.priority.get());
   }

   private boolean _targetCheck(Entity t) {
      if (!this.targetTypes.get().contains(t.getType())) {
         return false;
      }

      if (t instanceof Player p) {
         if (p.isCreative()) {
            return false;
         } else {
            return p == this.mc.player ? false : Friends.get().shouldAttack(p);
         }
      } else {
         return true;
      }
   }

   private void doAura() {
      if (this.isReadyToAttack() && this.target != null) {
         if (!(this.mc.player.distanceTo(this.target) > this.range.get())) {
            if (this.target != this.mc.player) {
               if (!(this.target instanceof Player) || !((Player)this.target).isDeadOrDying()) {
                  if (!this.target.isAlive()) {
                     this.isNaked = false;
                  } else {
                     this.doTpAura(this.target);
                  }
               }
            }
         }
      }
   }

   public void doTpAura(Entity target) {
      Vec3 targetVec = PredictUtil.predictEntity(target);
      Vec3 playerPos = new Vec3(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      double[] attackHeights = this.vClip1.get().stream().mapToDouble(Double::doubleValue).toArray();
      double moveDistance = TpSetting.get() != null ? TpSetting.get().getMoveDistance() : 128.0;

      for (double attackVclip : attackHeights) {
         Vec3 clipPos = new Vec3(playerPos.x, playerPos.y + attackVclip, playerPos.z);
         FindItemResult mace = InvUtils.find(Items.MACE);
         if (mace.found()) {
            int prevSlot = this.mc.player.getInventory().getSelectedSlot();
            boolean needsSwapBack = false;

            try {
               if (this.switchMode.get() != LKillAura.SwitchMode.None) {
                  this.swap(mace.slot(), this.switchMode.get());
                  needsSwapBack = true;
               }

               this.doTpWithTpSetting(playerPos, clipPos, moveDistance);
               this.sendMovePacketWithTpSetting(playerPos.x, playerPos.y, playerPos.z, false);
               if (this.swingHand.get()) {
                  this.mc.player.swing(InteractionHand.MAIN_HAND);
               }

               this.mc.gameMode.attack(this.mc.player, target);
               this.lastAttackTime = System.currentTimeMillis();
            } finally {
               if (needsSwapBack && this.switchMode.get() != LKillAura.SwitchMode.None) {
                  this.swapBack(prevSlot, this.switchMode.get());
               }
            }
         }
      }
   }

   private void swap(int slot, LKillAura.SwitchMode mode) {
      if (mode == LKillAura.SwitchMode.Silent) {
         InvUtils.swap(slot, true);
      } else if (mode == LKillAura.SwitchMode.Inventory) {
         InvUtils.swap(slot, false);
      }
   }

   private void swapBack(int slot, LKillAura.SwitchMode mode) {
      if (mode == LKillAura.SwitchMode.Silent) {
         InvUtils.swapBack();
      } else if (mode == LKillAura.SwitchMode.Inventory) {
         InvUtils.swap(slot, false);
      }
   }

   private void doTpWithTpSetting(Vec3 from, Vec3 to, double moveDistance) {
      if (TpSetting.get().isLTPUtils()) {
         LTpUtils.doTp(from, to, moveDistance);
      } else {
         QTPUtils.doTp(from, to, moveDistance);
      }
   }

   private void sendMovePacketWithTpSetting(double x, double y, double z, boolean onGround) {
      if (TpSetting.get().isLTPUtils()) {
         LTpUtils.sendMovePacket(x, y, z, onGround);
      } else {
         QTPUtils.sendMovePacket(x, y, z, onGround);
      }
   }

   public enum SwitchMode {
      Silent,
      None,
      Inventory;
   }
}
