package meteordevelopment.meteorclient.systems.modules.world;

import java.util.List;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.shapes.Shapes;

public class Collisions extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<List<Block>> blocks = this.sgGeneral
      .add(new BlockListSetting.Builder().name("blocks").description("What blocks should be added collision box.").filter(this::blockFilter).build());
   private final Setting<Boolean> magma = this.sgGeneral
      .add(new BoolSetting.Builder().name("magma").description("Prevents you from walking over magma blocks.").defaultValue(false).build());
   private final Setting<Boolean> unloadedChunks = this.sgGeneral
      .add(new BoolSetting.Builder().name("unloaded-chunks").description("Stops you from going into unloaded chunks.").defaultValue(false).build());
   private final Setting<Boolean> ignoreBorder = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-border").description("Removes world border collision.").defaultValue(false).build());

   public Collisions() {
      super(Categories.World, "collisions", "Adds collision boxes to certain blocks/areas.");
   }

   @EventHandler
   private void onCollisionShape(CollisionShapeEvent event) {
      if (this.mc.level != null && this.mc.player != null) {
         if (event.state.getFluidState().isEmpty()) {
            if (this.blocks.get().contains(event.state.getBlock())) {
               event.shape = Shapes.block();
            } else if (this.magma.get()
               && !this.mc.player.isShiftKeyDown()
               && event.state.isAir()
               && this.mc.level.getBlockState(event.pos.below()).getBlock() == Blocks.MAGMA_BLOCK) {
               event.shape = Shapes.block();
            }
         }
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      int x = (int)(this.mc.player.getX() + event.movement.x) >> 4;
      int z = (int)(this.mc.player.getZ() + event.movement.z) >> 4;
      if (this.unloadedChunks.get() && !this.mc.level.getChunkSource().hasChunk(x, z)) {
         ((IVec3)event.movement).meteor$set(0.0, event.movement.y, 0.0);
      }
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (this.unloadedChunks.get()) {
         if (event.packet instanceof ServerboundMoveVehiclePacket packet) {
            if (!this.mc.level.getChunkSource().hasChunk((int)packet.position().x() >> 4, (int)packet.position().z() >> 4)) {
               this.mc.player.getVehicle().absSnapTo(this.mc.player.getVehicle().xo, this.mc.player.getVehicle().yo, this.mc.player.getVehicle().zo);
               event.cancel();
            }
         } else if (event.packet instanceof ServerboundMovePlayerPacket packet
            && !this.mc.level.getChunkSource().hasChunk((int)packet.getX(this.mc.player.getX()) >> 4, (int)packet.getZ(this.mc.player.getZ()) >> 4)) {
            event.cancel();
         }
      }
   }

   private boolean blockFilter(Block block) {
      return block instanceof BaseFireBlock
         || block instanceof BasePressurePlateBlock
         || block instanceof TripWireBlock
         || block instanceof TripWireHookBlock
         || block instanceof WebBlock
         || block instanceof CampfireBlock
         || block instanceof SweetBerryBushBlock
         || block instanceof CactusBlock
         || block instanceof BaseRailBlock
         || block instanceof TrapDoorBlock
         || block instanceof PowderSnowBlock
         || block instanceof AbstractCauldronBlock
         || block instanceof HoneyBlock;
   }

   public boolean ignoreBorder() {
      return this.isActive() && this.ignoreBorder.get();
   }
}
