package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MultiPlayerGameModeAccessor;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;

public class SpeedMine extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<SpeedMine.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).defaultValue(SpeedMine.Mode.Damage))
               .onChanged(var1 -> this.removeHaste()))
            .build()
      );
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("blocks")
            .description("Selected blocks.")
            .filter(block -> block.defaultDestroyTime() > 0.0F)
            .visible(() -> this.mode.get() != SpeedMine.Mode.Haste)
            .build()
      );
   private final Setting<SpeedMine.ListMode> blocksFilter = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("blocks-filter"))
                     .description("How to use the blocks setting."))
                  .defaultValue(SpeedMine.ListMode.Blacklist))
               .visible(() -> this.mode.get() != SpeedMine.Mode.Haste))
            .build()
      );
   public final Setting<Double> modifier = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("modifier")
            .description("Mining speed modifier. An additional value of 0.2 is equivalent to one haste level (1.2 = haste 1).")
            .defaultValue(1.4)
            .visible(() -> this.mode.get() == SpeedMine.Mode.Normal)
            .min(0.0)
            .build()
      );
   private final Setting<Integer> hasteAmplifier = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("haste-amplifier")
            .description("What value of haste to give you. Above 2 not recommended.")
            .defaultValue(2)
            .min(1)
            .visible(() -> this.mode.get() == SpeedMine.Mode.Haste)
            .onChanged(var1 -> this.removeHaste())
            .build()
      );
   private final Setting<Boolean> instamine = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("instamine")
            .description("Whether or not to instantly mine blocks under certain conditions.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == SpeedMine.Mode.Damage)
            .build()
      );
   private final Setting<Boolean> grimBypass = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("grim-bypass")
            .description("Bypasses Grim's fastbreak check, working as of 2.3.58")
            .defaultValue(false)
            .visible(() -> this.mode.get() == SpeedMine.Mode.Damage)
            .build()
      );

   public SpeedMine() {
      super(Categories.Player, "speed-mine", "Allows you to quickly mine blocks.");
   }

   @Override
   public void onDeactivate() {
      this.removeHaste();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate()) {
         if (this.mode.get() == SpeedMine.Mode.Haste) {
            MobEffectInstance haste = this.mc.player.getEffect(MobEffects.HASTE);
            if (haste == null || haste.getAmplifier() <= this.hasteAmplifier.get() - 1) {
               this.mc.player.addEffect(new MobEffectInstance(MobEffects.HASTE, -1, this.hasteAmplifier.get() - 1, false, false, false), null);
            }
         } else if (this.mode.get() == SpeedMine.Mode.Damage) {
            MultiPlayerGameModeAccessor im = (MultiPlayerGameModeAccessor)this.mc.gameMode;
            float progress = im.meteor$getBreakingProgress();
            BlockPos pos = im.meteor$getCurrentBreakingBlockPos();
            if (pos == null || progress <= 0.0F) {
               return;
            }

            if (progress + this.mc.level.getBlockState(pos).getDestroyProgress(this.mc.player, this.mc.level, pos) >= 0.7F) {
               im.meteor$setDestroyProgress(1.0F);
            }
         }
      }
   }

   @EventHandler
   private void onPacket(PacketEvent.Send event) {
      if (this.mode.get() == SpeedMine.Mode.Damage && this.grimBypass.get()) {
         if (event.packet instanceof ServerboundPlayerActionPacket packet && packet.getAction() == Action.STOP_DESTROY_BLOCK) {
            this.mc.getConnection().send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, packet.getPos().above(), packet.getDirection()));
         }
      }
   }

   private void removeHaste() {
      if (Utils.canUpdate()) {
         MobEffectInstance haste = this.mc.player.getEffect(MobEffects.HASTE);
         if (haste != null && !haste.showIcon()) {
            this.mc.player.removeEffect(MobEffects.HASTE);
         }
      }
   }

   public boolean filter(Block block) {
      return this.blocksFilter.get() == SpeedMine.ListMode.Blacklist && !this.blocks.get().contains(block)
         ? true
         : this.blocksFilter.get() == SpeedMine.ListMode.Whitelist && this.blocks.get().contains(block);
   }

   public boolean instamine() {
      return this.isActive() && this.mode.get() == SpeedMine.Mode.Damage && this.instamine.get();
   }

   public enum ListMode {
      Whitelist,
      Blacklist;
   }

   public enum Mode {
      Normal,
      Haste,
      Damage;
   }
}
