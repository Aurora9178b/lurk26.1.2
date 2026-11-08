package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
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
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class LurkAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private List<Entity> targets = new ArrayList<>();
   private int timer = 0;
   private int slot;
   private volatile boolean attackQueued;
   private final Setting<Set<EntityType<?>>> targetTypes = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("target-type").description("Entities to attack.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("Entity sorting priority"))
               .defaultValue(SortPriority.LowestDistance))
            .build()
      );
   private final Setting<Integer> maxTarget = this.sgGeneral
      .add(new IntSetting.Builder().name("max-target").description("Maximum number of targets.").defaultValue(10).sliderMax(25).sliderMin(1).build());
   private final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("Max distance of a target.").defaultValue(50.0).sliderMax(200.0).sliderMin(1.0).build());
   private final Setting<Double> delay = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("delay")
            .description("Tick delay between attack cycles.")
            .defaultValue(1.0)
            .sliderMax(10.0)
            .sliderMin(1.0)
            .decimalPlaces(0)
            .build()
      );
   private final Setting<Boolean> swing = this.sgGeneral
      .add(new BoolSetting.Builder().name("swing-hand").description("Swing hand when attacking.").defaultValue(false).build());
   private final Setting<List<Double>> vclip = this.sgGeneral
      .add(
         new DoubleListSetting.Builder()
            .name("v-clip")
            .description("Fall heights.")
            .defaultValue(List.of(10.0, 15.0, 20.0, 25.0, 30.0))
            .sliderRange(0.0, 200.0)
            .decimalPlaces(2)
            .build()
      );

   public LurkAura() {
      super(Categories.Combat, "LurkAura", "yeet");
   }

   @Override
   public void onActivate() {
      this.slot = this.mc.player != null ? this.mc.player.getInventory().getSelectedSlot() : 0;
      this.attackQueued = false;
   }

   @Override
   public void onDeactivate() {
      this.attackQueued = false;
      if (this.mc.player != null && this.slot != -1) {
         for (int i = 0; i < 3; i++) {
            this.mc.player.connection.send(new ServerboundSetCarriedItemPacket(this.slot));
         }
      }
   }

   private boolean check(Entity e) {
      if (!this.targetTypes.get().contains(e.getType())) {
         return false;
      }

      if (e instanceof LivingEntity le) {
         if (le.isDeadOrDying() || le.getHealth() <= 0.0F) {
            return false;
         }

         if (e instanceof Player p) {
            if (p.isDeadOrDying() || p.getHealth() <= 0.0F) {
               return false;
            } else if (p.isCreative()) {
               return false;
            } else {
               return p == this.mc.player ? false : Friends.get().shouldAttack(p);
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   @EventHandler
   public void onRender(Render2DEvent event) {
      if (!this.attackQueued) {
         List<Entity> list = new ArrayList<>();
         TargetUtils.getList(list, this::check, this.priority.get(), this.maxTarget.get());
         this.targets.clear();
         this.targets.addAll(list);
         if (!this.targets.isEmpty()) {
            if (this.timer > 0) {
               this.timer--;
            } else {
               for (Entity e : this.targets) {
                  if (e.position().distanceToSqr(this.mc.player.position()) >= this.range.get() * this.range.get()) {
                     return;
                  }
               }

               FindItemResult mace = InvUtils.find(Items.MACE);
               if (mace.found()) {
                  Entity target = this.targets.removeFirst();
                  List<Double> raw = this.vclip.get();
                  double[] heights = new double[raw.size()];

                  for (int i = 0; i < raw.size(); i++) {
                     heights[i] = raw.get(i);
                  }

                  this.timer = this.delay.get().intValue();
                  this.attackQueued = true;
                  this.mc
                     .execute(
                        () -> {
                           try {
                              if (this.isActive()
                                 && this.mc.player != null
                                 && this.mc.level != null
                                 && this.mc.gameMode != null
                                 && target != null
                                 && !target.isRemoved()) {
                                 this.doAura(target, heights, TpSetting.get().getMoveDistance(), this.swing.get());
                                 return;
                              }
                           } finally {
                              this.attackQueued = false;
                           }
                        }
                     );
               }
            }
         }
      }
   }

   private void doAura(Entity target, double[] h, double d, boolean swing) {
      if (this.isActive() && this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         Vec3 tv = PredictUtil.predictEntity(target);
         Vec3 pPos = new Vec3(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         Vec3 safe = this.findVClip(tv, pPos);
         if (safe == null) {
            safe = pPos;
         }

         int maxP = (int)Math.ceil(pPos.distanceTo(safe) / d);
         int maxPackets = Math.max(maxP, 1);
         if (maxPackets > 20) {
            ChatUtils.sendMsg(Component.literal("TP packet limit exceeded"));
         } else {
            for (int i = 1; i < maxPackets; i++) {
               this.doMovePacket(pPos);
            }

            FindItemResult mace = InvUtils.find(Items.MACE);
            if (mace.found()) {
               int prev = this.mc.player.getInventory().getSelectedSlot();
               InvUtils.swap(mace.slot(), true);

               for (double v : h) {
                  Vec3 upPos = new Vec3(this.mc.player.getX(), this.mc.player.getY() + v, this.mc.player.getZ());
                  this.doTeleport(pPos, upPos, d);
                  this.doTeleport(upPos, safe, d);
                  this.mc.player.fallDistance = (float)v;
                  if (swing) {
                     this.mc.player.swing(InteractionHand.MAIN_HAND);
                  }

                  this.mc.gameMode.attack(this.mc.player, target);
               }

               InvUtils.swap(prev, true);
               this.doTeleport(pPos, pPos, d);
            }
         }
      }
   }

   private void doTeleport(Vec3 from, Vec3 to, double moveDist) {
      if (TpSetting.get().isLTPUtils()) {
         LTpUtils.doTp(from, to, moveDist);
      } else {
         QTPUtils.doTp(from, to, moveDist);
      }
   }

   private void doMovePacket(Vec3 pos) {
      if (TpSetting.get().isLTPUtils()) {
         LTpUtils.sendMovePacket(pos.x, pos.y, pos.z, false);
      } else {
         QTPUtils.sendMovePacket(pos.x, pos.y, pos.z, false);
      }
   }

   private Vec3 findVClip(Vec3 from, Vec3 to) {
      return TpSetting.get().isLTPUtils()
         ? LTpUtils.findVClipVecToMove(from, to, 1.8, false)
         : QTPUtils.findVClipVecToMove(from, to, QTPUtils.TpVClipFindType.All);
   }

   private Vec3 findSafe(BlockPos start, double vclip) {
      if (vclip > 0.0) {
         for (int y = (int)vclip + start.getY(); y >= start.getY(); y--) {
            BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
            if (BlockUtils.isSafeBlock(pos) && BlockUtils.isSafeBlock(pos.above())) {
               return Vec3.atCenterOf(pos);
            }
         }
      } else {
         for (int y = (int)vclip + start.getY(); y <= start.getY(); y++) {
            BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
            if (BlockUtils.isSafeBlock(pos) && BlockUtils.isSafeBlock(pos.above())) {
               return Vec3.atCenterOf(pos);
            }
         }
      }

      return Vec3.atCenterOf(start);
   }

   @Override
   public String getInfoString() {
      return this.targets.isEmpty() ? "None" : this.targets.getFirst().getName().getString();
   }
}
