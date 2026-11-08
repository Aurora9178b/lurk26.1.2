package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.world.PacketMinePlus;
import meteordevelopment.meteorclient.utils.leaves.LeavesBlockPos;
import meteordevelopment.meteorclient.utils.leaves.LeavesBlockUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesCombatUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesTimer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class AutoCityPlus extends Module {
   public static AutoCityPlus INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> targetRange = this.sgGeneral
      .add(new IntSetting.Builder().name("TargetRange").description("Target range.").defaultValue(6).min(0).sliderMax(8).build());
   public final Setting<Integer> range = this.sgGeneral
      .add(new IntSetting.Builder().name("Range").description("Action range.").defaultValue(6).min(0).sliderMax(8).build());
   private final Setting<Boolean> doubleBreak = this.sgGeneral
      .add(new BoolSetting.Builder().name("DoubleBreak").description("Uses PacketMine+ double break.").defaultValue(true).build());
   public final Setting<Boolean> delay = this.sgGeneral
      .add(new BoolSetting.Builder().name("CityDelay").description("Waits for PacketMine+ delay.").defaultValue(true).build());
   private final Setting<Boolean> antiCrawl = this.sgGeneral
      .add(new BoolSetting.Builder().name("AntiCrawl").description("Mines crawl blocks.").defaultValue(true).build());
   private final Setting<Boolean> preferSelfClick = this.sgGeneral
      .add(new BoolSetting.Builder().name("PreferSelfClick").description("Prefers manual mining.").defaultValue(true).build());
   private final Setting<Boolean> head = this.sgGeneral
      .add(new BoolSetting.Builder().name("Head").description("Mines head blocks.").defaultValue(false).build());
   private final Setting<Boolean> burrow = this.sgGeneral
      .add(new BoolSetting.Builder().name("Burrow").description("Mines burrow blocks.").defaultValue(true).build());
   private final Setting<Boolean> face = this.sgGeneral
      .add(new BoolSetting.Builder().name("Face").description("Mines face blocks.").defaultValue(true).build());
   private final Setting<Boolean> down = this.sgGeneral
      .add(new BoolSetting.Builder().name("Down").description("Mines lower blocks.").defaultValue(false).build());
   private final Setting<Boolean> surround = this.sgGeneral
      .add(new BoolSetting.Builder().name("Surround").description("Mines surround blocks.").defaultValue(true).build());
   private final LeavesTimer cityTimer = new LeavesTimer();
   public static final List<Block> hard = Arrays.asList(
      Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
   );

   public AutoCityPlus() {
      super(Categories.Combat, "AutoCity+", "Leaves-style AutoCity using PacketMine+.", "auto-city-plus", "autocity+");
      INSTANCE = this;
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundPlayerActionPacket packet && packet.getAction() == Action.STOP_DESTROY_BLOCK) {
         this.cityTimer.reset();
      }
   }

   @EventHandler
   public void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null) {
         PacketMinePlus packetMine = PacketMinePlus.INSTANCE;
         if (packetMine != null) {
            Player player = LeavesCombatUtil.getClosestEnemy(this.targetRange.get().intValue());
            if (!this.preferSelfClick.get() || PacketMinePlus.selfClickPos == null) {
               if (!this.delay.get() || this.cityTimer.passedMs(300L)) {
                  if (this.antiCrawl.get() && this.mc.player.isCrouching()) {
                     BlockPos crawlPos = this.mc.player.blockPosition().above();
                     if (this.canBreak(crawlPos) && !crawlPos.equals(PacketMinePlus.targetPos) && !crawlPos.equals(PacketMinePlus.secondPos)) {
                        PacketMinePlus.selfClickPos = crawlPos;
                        packetMine.mine(crawlPos);
                        return;
                     }
                  }

                  if (player != null) {
                     this.doBreak(packetMine, player);
                  }
               }
            }
         }
      }
   }

   private void doBreak(PacketMinePlus packetMine, Player player) {
      BlockPos pos = player.blockPosition();
      double[] yOffset = new double[]{-0.8, 0.3, 2.3, 1.1};
      double[] xzOffset = new double[]{0.3, -0.3};
      if (!this.doubleBreak.get()) {
         for (Player entity : LeavesCombatUtil.getEnemies(this.targetRange.get().intValue())) {
            for (double y : yOffset) {
               for (double x : xzOffset) {
                  for (double z : xzOffset) {
                     BlockPos offsetPos = new LeavesBlockPos(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                     if (this.isObsidian(offsetPos) && LeavesBlockUtil.getClickSideStrict(offsetPos) != null && offsetPos.equals(PacketMinePlus.targetPos)) {
                        return;
                     }
                  }
               }
            }
         }
      } else {
         int count = 0;

         for (Player entity : LeavesCombatUtil.getEnemies(this.targetRange.get().intValue())) {
            for (double y : yOffset) {
               for (double x : xzOffset) {
                  for (double z : xzOffset) {
                     BlockPos offsetPos = new LeavesBlockPos(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                     if (this.isObsidian(offsetPos)
                        && LeavesBlockUtil.getClickSideStrict(offsetPos) != null
                        && (offsetPos.equals(PacketMinePlus.targetPos) || offsetPos.equals(PacketMinePlus.secondPos))) {
                        count++;
                     }
                  }
               }
            }
         }

         if (count == 2) {
            return;
         }
      }

      List<Float> yList = new ArrayList<>();
      if (this.down.get()) {
         yList.add(-0.8F);
      }

      if (this.head.get()) {
         yList.add(2.3F);
      }

      if (this.burrow.get()) {
         yList.add(0.3F);
      }

      if (this.face.get()) {
         yList.add(1.1F);
      }

      Iterator var28 = yList.iterator();

      while (var28.hasNext()) {
         double y = ((Float)var28.next()).floatValue();

         for (double offset : xzOffset) {
            BlockPos offsetPos = new LeavesBlockPos(player.getX() + offset, player.getY() + y, player.getZ() + offset);
            if (this.canBreak(offsetPos)) {
               packetMine.mine(offsetPos);
               return;
            }
         }
      }

      var28 = yList.iterator();

      while (var28.hasNext()) {
         double y = ((Float)var28.next()).floatValue();

         for (double offset : xzOffset) {
            for (double offset2 : xzOffset) {
               BlockPos offsetPos = new LeavesBlockPos(player.getX() + offset2, player.getY() + y, player.getZ() + offset);
               if (this.canBreak(offsetPos)) {
                  packetMine.mine(offsetPos);
                  return;
               }
            }
         }
      }

      if (this.surround.get()) {
         for (Direction direction : Direction.values()) {
            if (direction != Direction.UP && direction != Direction.DOWN) {
               BlockPos offset = pos.relative(direction);
               if (!(this.mc.player.getEyePosition().distanceTo(offset.getCenter()) > this.range.get().intValue())
                  && (this.mc.level.isEmptyBlock(offset) || offset.equals(PacketMinePlus.targetPos))
                  && this.canPlaceCrystal(offset, false)) {
                  if (!this.doubleBreak.get()) {
                     return;
                  }

                  if (PacketMinePlus.targetPos != null && PacketMinePlus.completed) {
                     return;
                  }
               }
            }
         }

         ArrayList<BlockPos> list = new ArrayList<>();

         for (Direction direction : Direction.values()) {
            if (direction != Direction.UP && direction != Direction.DOWN) {
               BlockPos offset = pos.relative(direction);
               if (!(this.mc.player.getEyePosition().distanceTo(offset.getCenter()) > this.range.get().intValue())
                  && this.canBreak(offset)
                  && this.canPlaceCrystal(offset, true)
                  && !this.isSurroundPos(offset)) {
                  list.add(offset);
               }
            }
         }

         if (list.isEmpty()) {
            for (Direction direction : Direction.values()) {
               if (direction != Direction.UP && direction != Direction.DOWN) {
                  BlockPos offset = pos.relative(direction);
                  if (!(this.mc.player.getEyePosition().distanceTo(offset.getCenter()) > this.range.get().intValue())
                     && this.canBreak(offset)
                     && this.canPlaceCrystal(offset, false)) {
                     list.add(offset);
                  }
               }
            }
         }

         if (!list.isEmpty()) {
            packetMine.mine(list.stream().min(Comparator.comparingDouble(p -> p.getCenter().distanceToSqr(this.mc.player.getEyePosition()))).get());
         }
      }
   }

   private boolean isSurroundPos(BlockPos pos) {
      for (Direction direction : Direction.values()) {
         if (direction != Direction.UP && direction != Direction.DOWN && this.getPlayerPos(true).relative(direction).equals(pos)) {
            return true;
         }
      }

      return false;
   }

   public BlockPos getPlayerPos(boolean fix) {
      return new LeavesBlockPos(this.mc.player.position(), fix);
   }

   public boolean canPlaceCrystal(BlockPos pos, boolean block) {
      BlockPos obsPos = pos.below();
      BlockPos boost = obsPos.above();
      return (LeavesBlockUtil.getBlock(obsPos) == Blocks.BEDROCK || LeavesBlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN || !block)
         && LeavesBlockUtil.noEntityBlockCrystal(boost, true, true)
         && LeavesBlockUtil.noEntityBlockCrystal(boost.above(), true, true);
   }

   private boolean isObsidian(BlockPos pos) {
      return this.mc.player.getEyePosition().distanceTo(pos.getCenter()) <= this.range.get().intValue()
         && hard.contains(this.mc.level.getBlockState(pos).getBlock())
         && LeavesBlockUtil.getClickSideStrict(pos) != null;
   }

   private boolean canBreak(BlockPos pos) {
      return this.isObsidian(pos)
         && LeavesBlockUtil.getClickSideStrict(pos) != null
         && !pos.equals(PacketMinePlus.targetPos)
         && !pos.equals(PacketMinePlus.secondPos)
         && ((PacketMinePlus.targetPos == null || PacketMinePlus.secondPos == null) && this.doubleBreak.get() || PacketMinePlus.targetPos == null);
   }
}
