package meteordevelopment.meteorclient.systems.modules.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ShulkerBoxMenuAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.systems.modules.player.AutoTool;
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak;
import meteordevelopment.meteorclient.systems.modules.player.SpeedMine;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.HorizontalDirection;
import meteordevelopment.meteorclient.utils.misc.MBlockPos;
import meteordevelopment.meteorclient.utils.player.CustomPlayerInput;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.joml.Vector3d;

public class HighwayBuilder extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgDigging = this.settings.createGroup("Digging");
   private final SettingGroup sgPaving = this.settings.createGroup("Paving");
   private final SettingGroup sgInventory = this.settings.createGroup("Inventory");
   private final SettingGroup sgRenderDigging = this.settings.createGroup("Render Digging");
   private final SettingGroup sgRenderPaving = this.settings.createGroup("Render Paving");
   private final Setting<Integer> width = this.sgGeneral
      .add(new IntSetting.Builder().name("width").description("Width of the highway.").defaultValue(4).range(1, 5).sliderRange(1, 5).build());
   private final Setting<Integer> height = this.sgGeneral
      .add(new IntSetting.Builder().name("height").description("Height of the highway.").defaultValue(3).range(2, 5).sliderRange(2, 5).build());
   private final Setting<HighwayBuilder.Floor> floor = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("floor"))
                  .description("What floor placement mode to use."))
               .defaultValue(HighwayBuilder.Floor.Replace))
            .build()
      );
   private final Setting<Boolean> railings = this.sgGeneral
      .add(new BoolSetting.Builder().name("railings").description("Builds railings next to the highway.").defaultValue(true).build());
   private final Setting<Boolean> cornerBlock = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("corner-support-block")
            .description("Places a support block underneath the railings, to prevent air placing.")
            .defaultValue(true)
            .visible(this.railings::get)
            .build()
      );
   private final Setting<Boolean> mineAboveRailings = this.sgGeneral
      .add(new BoolSetting.Builder().name("mine-above-railings").description("Mines blocks above railings.").defaultValue(true).build());
   private final Setting<HighwayBuilder.Rotation> rotation = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("rotation")).description("Mode of rotation."))
               .defaultValue(HighwayBuilder.Rotation.Both))
            .build()
      );
   private final Setting<Boolean> disconnectOnToggle = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("disconnect-on-toggle")
            .description("Automatically disconnects when the module is turned off, for example for not having enough blocks.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> pauseOnLag = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("pause-on-lag").description("Pauses the current process while the server stops responding.").defaultValue(true).build()
      );
   private final Setting<Boolean> destroyCrystalTraps = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("destroy-crystal-traps")
            .description("Use a bow to defuse crystal traps safely from a distance. An infinity bow is recommended.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> doubleMine = this.sgDigging
      .add(
         new BoolSetting.Builder()
            .name("double-mine")
            .description("Whether to double mine blocks when applicable (normal mine and packet mine simultaneously).")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> fastBreak = this.sgDigging
      .add(
         new BoolSetting.Builder()
            .name("fast-break")
            .description("Whether to finish breaking blocks faster than normal while double mining.")
            .defaultValue(true)
            .visible(this.doubleMine::get)
            .build()
      );
   private final Setting<Boolean> dontBreakTools = this.sgDigging
      .add(new BoolSetting.Builder().name("dont-break-tools").description("Don't break tools.").defaultValue(false).build());
   private final Setting<Integer> breakDurability = this.sgDigging
      .add(
         new IntSetting.Builder()
            .name("durability-percentage")
            .description("The durability percentage at which to stop using a tool.")
            .defaultValue(2)
            .range(1, 100)
            .sliderRange(1, 100)
            .visible(this.dontBreakTools::get)
            .build()
      );
   private final Setting<Integer> savePickaxes = this.sgDigging
      .add(
         new IntSetting.Builder()
            .name("save-pickaxes")
            .description("How many pickaxes to ensure are saved. Hitting this number in your inventory will trigger a restock or the module toggling off.")
            .defaultValue(1)
            .range(0, 36)
            .sliderRange(0, 36)
            .visible(() -> !this.dontBreakTools.get())
            .build()
      );
   private final Setting<Integer> breakDelay = this.sgDigging
      .add(new IntSetting.Builder().name("break-delay").description("The delay between breaking blocks.").defaultValue(0).min(0).build());
   private final Setting<Integer> blocksPerTick = this.sgDigging
      .add(
         new IntSetting.Builder()
            .name("blocks-per-tick")
            .description("The maximum amount of blocks that can be mined in a tick. Only applies to blocks instantly breakable.")
            .defaultValue(1)
            .range(1, 100)
            .sliderRange(1, 25)
            .build()
      );
   public final Setting<List<Block>> blocksToPlace = this.sgPaving
      .add(
         new BlockListSetting.Builder()
            .name("blocks-to-place")
            .description("Blocks it is allowed to place.")
            .defaultValue(Blocks.OBSIDIAN)
            .filter(block -> Block.isShapeFullBlock(block.defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)))
            .build()
      );
   private final Setting<Double> placeRange = this.sgPaving
      .add(
         new DoubleSetting.Builder()
            .name("place-range")
            .description("The maximum distance at which you can place blocks.")
            .defaultValue(4.5)
            .sliderMax(5.5)
            .build()
      );
   private final Setting<Integer> placeDelay = this.sgPaving
      .add(new IntSetting.Builder().name("place-delay").description("The delay between placing blocks.").defaultValue(0).min(0).build());
   private final Setting<Integer> placementsPerTick = this.sgPaving
      .add(
         new IntSetting.Builder()
            .name("placements-per-tick")
            .description("The maximum amount of blocks that can be placed in a tick.")
            .defaultValue(1)
            .min(1)
            .build()
      );
   private final Setting<List<Item>> trashItems = this.sgInventory
      .add(
         new ItemListSetting.Builder()
            .name("trash-items")
            .description("Items that are considered trash and can be thrown out.")
            .defaultValue(
               Items.NETHERRACK,
               Items.QUARTZ,
               Items.GOLD_NUGGET,
               Items.GOLDEN_SWORD,
               Items.GLOWSTONE_DUST,
               Items.GLOWSTONE,
               Items.BLACKSTONE,
               Items.BASALT,
               Items.GHAST_TEAR,
               Items.SOUL_SAND,
               Items.SOUL_SOIL,
               Items.ROTTEN_FLESH,
               Items.MAGMA_BLOCK
            )
            .build()
      );
   private final Setting<Integer> inventoryDelay = this.sgInventory
      .add(new IntSetting.Builder().name("inventory-delay").description("Delay in ticks on inventory interactions.").defaultValue(3).min(0).build());
   private final Setting<Boolean> ejectUselessShulkers = this.sgInventory
      .add(
         new BoolSetting.Builder()
            .name("eject-useless-shulkers")
            .description(
               "Whether you should eject useless shulkers. Warning - will throw out any shulkers that don't contain blocks to place, pickaxes, or food. Be careful with your kits."
            )
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> searchEnderChest = this.sgInventory
      .add(
         new BoolSetting.Builder()
            .name("search-ender-chest")
            .description("Searches your ender chest to find items to use. Be careful with this one, especially if you let it search through shulkers.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> searchShulkers = this.sgInventory
      .add(new BoolSetting.Builder().name("search-shulkers").description("Searches through shulkers to find items to use.").defaultValue(true).build());
   private final Setting<Integer> minEmpty = this.sgInventory
      .add(
         new IntSetting.Builder()
            .name("minimum-empty-slots")
            .description("The minimum amount of empty slots you want left after mining obsidian.")
            .defaultValue(3)
            .sliderRange(0, 9)
            .min(0)
            .build()
      );
   private final Setting<Boolean> mineEnderChests = this.sgInventory
      .add(new BoolSetting.Builder().name("mine-ender-chests").description("Mines ender chests for obsidian.").defaultValue(true).build());
   private final Setting<HighwayBuilder.BlockadeType> blockadeType = this.sgInventory
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("echest-blockade-type"))
                     .description("What blockade type to use (the structure placed when mining echests)."))
                  .defaultValue(HighwayBuilder.BlockadeType.Full))
               .visible(this.mineEnderChests::get))
            .build()
      );
   public final Setting<Integer> saveEchests = this.sgInventory
      .add(
         new IntSetting.Builder()
            .name("save-ender-chests")
            .description("How many ender chests to ensure are saved. Hitting this number in your inventory will trigger a restock or the module toggling off.")
            .defaultValue(2)
            .range(0, 64)
            .sliderRange(0, 64)
            .visible(this.mineEnderChests::get)
            .build()
      );
   private final Setting<Boolean> rebreakEchests = this.sgInventory
      .add(
         new BoolSetting.Builder()
            .name("instantly-rebreak-echests")
            .description("Whether or not to use the instant rebreak exploit to break echests.")
            .defaultValue(false)
            .visible(this.mineEnderChests::get)
            .build()
      );
   private final Setting<Integer> rebreakTimer = this.sgInventory
      .add(
         new IntSetting.Builder()
            .name("rebreak-delay")
            .description("Delay between rebreak attempts.")
            .defaultValue(0)
            .sliderMax(20)
            .visible(() -> this.mineEnderChests.get() && this.rebreakEchests.get())
            .build()
      );
   private final Setting<Boolean> renderMine = this.sgRenderDigging
      .add(new BoolSetting.Builder().name("render-blocks-to-mine").description("Render blocks to be mined.").defaultValue(true).build());
   private final Setting<ShapeMode> renderMineShape = this.sgRenderDigging
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("blocks-to-mine-shape-mode"))
                  .description("How the blocks to be mined are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> renderMineSideColor = this.sgRenderDigging
      .add(
         new ColorSetting.Builder()
            .name("blocks-to-mine-side-color")
            .description("Color of blocks to be mined.")
            .defaultValue(new SettingColor(225, 25, 25, 25))
            .build()
      );
   private final Setting<SettingColor> renderMineLineColor = this.sgRenderDigging
      .add(
         new ColorSetting.Builder()
            .name("blocks-to-mine-line-color")
            .description("Color of blocks to be mined.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
      );
   private final Setting<Boolean> renderPlace = this.sgRenderPaving
      .add(new BoolSetting.Builder().name("render-blocks-to-place").description("Render blocks to be placed.").defaultValue(true).build());
   private final Setting<ShapeMode> renderPlaceShape = this.sgRenderPaving
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("blocks-to-place-shape-mode"))
                  .description("How the blocks to be placed are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> renderPlaceSideColor = this.sgRenderPaving
      .add(
         new ColorSetting.Builder()
            .name("blocks-to-place-side-color")
            .description("Color of blocks to be placed.")
            .defaultValue(new SettingColor(25, 25, 225, 25))
            .build()
      );
   private final Setting<SettingColor> renderPlaceLineColor = this.sgRenderPaving
      .add(
         new ColorSetting.Builder()
            .name("blocks-to-place-line-color")
            .description("Color of blocks to be placed.")
            .defaultValue(new SettingColor(25, 25, 225))
            .build()
      );
   private HorizontalDirection dir;
   private HorizontalDirection leftDir;
   private HorizontalDirection rightDir;
   private ClientInput prevInput;
   private CustomPlayerInput input;
   private HighwayBuilder.State state;
   private HighwayBuilder.State lastState;
   private HighwayBuilder.IBlockPosProvider blockPosProvider;
   public Vec3 start;
   public int blocksBroken;
   public int blocksPlaced;
   private final MBlockPos lastBreakingPos = new MBlockPos();
   private boolean displayInfo;
   private boolean warned;
   private boolean suspended = true;
   private boolean inventory = true;
   private int placeTimer;
   private int breakTimer;
   private int count;
   private int containerId;
   private final HighwayBuilder.RestockTask restockTask = new HighwayBuilder.RestockTask(this);
   private final ArrayList<EndCrystal> ignoreCrystals = new ArrayList<>();
   public boolean drawingBow;
   public HighwayBuilder.DoubleMineBlock normalMining;
   public HighwayBuilder.DoubleMineBlock packetMining;
   private final MBlockPos posRender2 = new MBlockPos();
   private final MBlockPos posRender3 = new MBlockPos();

   public HighwayBuilder() {
      super(Categories.World, "highway-builder", "Automatically builds highways.");
      this.runInMainMenu = true;
   }

   @Override
   public void onActivate() {
      if (Utils.canUpdate()) {
         this.updateVariables();
         this.dir = HorizontalDirection.get(this.mc.player.getYRot());
         this.leftDir = this.dir.rotateLeftSkipOne();
         this.rightDir = this.leftDir.opposite();
         this.blockPosProvider = this.dir.diagonal ? new HighwayBuilder.DiagonalBlockPosProvider() : new HighwayBuilder.StraightBlockPosProvider();
         this.state = HighwayBuilder.State.Forward;
         this.setState(HighwayBuilder.State.Center);
         this.lastBreakingPos.set(0, 0, 0);
         this.start = this.mc.player.position();
         this.blocksBroken = this.blocksPlaced = 0;
         this.displayInfo = true;
         this.suspended = false;
         this.restockTask.complete();
         if (this.blocksPerTick.get() > 1 && this.rotation.get().mine) {
            this.warning("With rotations enabled, you can break at most 1 block per tick.");
         }

         if (this.placementsPerTick.get() > 1 && this.rotation.get().place) {
            this.warning("With rotations enabled, you can place at most 1 block per tick.");
         }

         if (Modules.get().get(InstantRebreak.class).isActive()) {
            this.warning("It's recommended to disable the Instant Rebreak module and instead use the 'instantly-rebreak-echests' setting to avoid errors.");
         }

         if (Modules.get().get(Speed.class).isActive() && this.dir.diagonal) {
            this.warning("It's recommended to disable the Speed module to avoid misalignment on diagonals.");
         }

         if (!Modules.get().get(Velocity.class).isActive()) {
            this.warning("It's recommended to enable the Velocity module to avoid misalignment (entity pushing, liquid movement).");
         }

         if (!this.warned && Modules.get().get(NoGhostBlocks.class).isActive()) {
            this.info(
               "The No Ghost Blocks module is useful to prevent desyncs on laggy servers. However, it will also slow Highway Builder down, and comes with the risks of incorrect statistics and packet kicks."
            );
            this.warned = true;
         }
      }
   }

   @Override
   public void onDeactivate() {
      if (Utils.canUpdate()) {
         this.mc.player.input = this.prevInput;
         this.mc.player.setYRot(this.dir.yaw);
         this.mc.options.keyUse.setDown(false);
         if (this.displayInfo) {
            this.info("Distance: (highlight)%.0f", PlayerUtils.distanceTo(this.start));
            this.info("Blocks broken: (highlight)%d", this.blocksBroken);
            this.info("Blocks placed: (highlight)%d", this.blocksPlaced);
         }
      }
   }

   @Override
   public void error(String message, Object... args) {
      super.error(message, args);
      this.toggle();
      if (this.disconnectOnToggle.get()) {
         this.disconnect(message, args);
      }
   }

   private void errorEarly(String message, Object... args) {
      super.error(message, args);
      this.displayInfo = false;
      this.toggle();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.dir == null) {
         this.onActivate();
      } else {
         if (this.suspended) {
            if (!this.inventory || !Utils.canUpdate()) {
               return;
            }

            this.updateVariables();
            this.suspended = false;
         }

         if (this.width.get() < 3 && this.dir.diagonal) {
            this.errorEarly("Diagonal highways with width less than 3 are not supported.");
         } else if (Modules.get().get(AutoEat.class).eating || Modules.get().get(AutoGap.class).isEating() || Modules.get().get(KillAura.class).attacking) {
            this.input.stop();
         } else if (this.pauseOnLag.get() && TickRate.INSTANCE.getTimeSinceLastTick() >= 1.5F) {
            this.input.stop();
         } else {
            this.count = 0;
            if (this.mc.player.getY() < this.start.y - 0.5) {
               this.setState(HighwayBuilder.State.ReLevel);
            }

            this.tickDoubleMine();
            this.state.tick(this);
            if (this.breakTimer > 0) {
               this.breakTimer--;
            }

            if (this.placeTimer > 0) {
               this.placeTimer--;
            }
         }
      }
   }

   @EventHandler
   private void onPacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundContainerSetContentPacket p) {
         if (p.containerId() == 0 && this.suspended) {
            this.inventory = true;
         } else {
            this.containerId = p.containerId();
         }
      }
   }

   @EventHandler
   private void onGameLeave(GameLeftEvent event) {
      this.suspended = true;
      this.inventory = false;
   }

   @EventHandler
   private void onRender2d(Render2DEvent event) {
      if (!this.suspended && this.renderMine.get()) {
         if (this.normalMining != null) {
            this.normalMining.renderLetter();
         }

         if (this.packetMining != null) {
            this.packetMining.renderLetter();
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (!this.suspended && this.blockPosProvider != null) {
         if (this.renderMine.get()) {
            this.render(event, this.blockPosProvider.getFront(), mBlockPos -> this.canMine(mBlockPos, true), true);
            if (this.floor.get() == HighwayBuilder.Floor.Replace) {
               this.render(event, this.blockPosProvider.getFloor(), mBlockPos -> this.canMine(mBlockPos, false), true);
            }

            if (this.railings.get()) {
               this.render(event, this.blockPosProvider.getRailings(0), mBlockPos -> this.canMine(mBlockPos, false), true);
            }

            if (this.mineAboveRailings.get()) {
               this.render(event, this.blockPosProvider.getRailings(1), mBlockPos -> this.canMine(mBlockPos, true), true);
            }

            if (this.state == HighwayBuilder.State.MineEChestBlockade) {
               this.render(event, this.blockPosProvider.getBlockade(true, this.blockadeType.get()), mBlockPos -> this.canMine(mBlockPos, true), true);
            }
         }

         if (this.renderPlace.get()) {
            this.render(event, this.blockPosProvider.getLiquids(), mBlockPos -> this.canPlace(mBlockPos, true), false);
            if (this.railings.get()) {
               this.render(event, this.blockPosProvider.getRailings(0), mBlockPos -> this.canPlace(mBlockPos, false), false);
               if (this.cornerBlock.get()) {
                  this.render(event, this.blockPosProvider.getRailings(-1), mBlockPos -> {
                     boolean valid = false;

                     for (MBlockPos pos : this.blockPosProvider.getRailings(0)) {
                        if (!this.blocksToPlace.get().contains(pos.getState().getBlock()) && pos.add(0, -1, 0).equals(mBlockPos)) {
                           valid = true;
                           break;
                        }
                     }

                     return valid && this.canPlace(mBlockPos, false);
                  }, false);
               }
            }

            this.render(event, this.blockPosProvider.getFloor(), mBlockPos -> this.canPlace(mBlockPos, false), false);
            if (this.state == HighwayBuilder.State.PlaceEChestBlockade) {
               this.render(event, this.blockPosProvider.getBlockade(false, this.blockadeType.get()), mBlockPos -> this.canPlace(mBlockPos, false), false);
            }
         }
      }
   }

   private void render(Render3DEvent event, HighwayBuilder.MBPIterator it, Predicate<MBlockPos> predicate, boolean mine) {
      Color sideColor = mine ? this.renderMineSideColor.get() : this.renderPlaceSideColor.get();
      Color lineColor = mine ? this.renderMineLineColor.get() : this.renderPlaceLineColor.get();
      ShapeMode shapeMode = mine ? this.renderMineShape.get() : this.renderPlaceShape.get();

      for (MBlockPos pos : it) {
         this.posRender2.set(pos);
         if (predicate.test(this.posRender2)) {
            int excludeDir = 0;

            for (Direction side : Direction.values()) {
               this.posRender3.set(this.posRender2).add(side.getStepX(), side.getStepY(), side.getStepZ());
               it.save();

               for (MBlockPos p : it) {
                  if (p.equals(this.posRender3) && predicate.test(p)) {
                     excludeDir |= Dir.get(side);
                  }
               }

               it.restore();
            }

            event.renderer.box(this.posRender2.getBlockPos(), sideColor, lineColor, shapeMode, excludeDir);
         }
      }
   }

   private void updateVariables() {
      this.prevInput = this.mc.player.input;
      this.mc.player.input = this.input = new CustomPlayerInput();
      this.placeTimer = this.breakTimer = this.count = this.containerId = 0;
      this.ignoreCrystals.clear();
      this.normalMining = null;
      this.packetMining = null;
   }

   private void setState(HighwayBuilder.State state) {
      this.setState(state, this.state);
   }

   private void setState(HighwayBuilder.State state, HighwayBuilder.State lastState) {
      this.lastState = lastState;
      this.state = state;
      this.input.stop();
      state.start(this);
   }

   private int getWidthLeft() {
      return switch (this.width.get()) {
         case 2, 3 -> 1;
         case 4, 5 -> 2;
         default -> 0;
      };
   }

   private int getWidthRight() {
      return switch (this.width.get()) {
         case 3, 4 -> 1;
         case 5 -> 2;
         default -> 0;
      };
   }

   private boolean canMine(MBlockPos pos, boolean mineBlocksToPlace) {
      BlockState state = pos.getState();
      return BlockUtils.canBreak(pos.getBlockPos(), state) && (mineBlocksToPlace || !this.blocksToPlace.get().contains(state.getBlock()));
   }

   private boolean canPlace(MBlockPos pos, boolean liquids) {
      if (pos.getBlockPos().distToCenterSqr(this.mc.player.getEyePosition()) > this.placeRange.get() * this.placeRange.get()) {
         return false;
      } else {
         return liquids ? !pos.getState().getFluidState().isEmpty() : BlockUtils.canPlace(pos.getBlockPos());
      }
   }

   private void disconnect(String message, Object... args) {
      MutableComponent text = Component.literal(
            String.format("%s[%s%s%s] %s", ChatFormatting.GRAY, ChatFormatting.BLUE, this.title, ChatFormatting.GRAY, ChatFormatting.RED)
               + String.format(message, args)
         )
         .append("\n");
      text.append(this.getStatsText());
      this.mc.getConnection().getConnection().disconnect(text);
   }

   public MutableComponent getStatsText() {
      MutableComponent text = Component.literal(
         String.format("%sDistance: %s%.0f\n", ChatFormatting.GRAY, ChatFormatting.WHITE, this.mc.player == null ? 0.0 : PlayerUtils.distanceTo(this.start))
      );
      text.append(String.format("%sBlocks broken: %s%d\n", ChatFormatting.GRAY, ChatFormatting.WHITE, this.blocksBroken));
      text.append(String.format("%sBlocks placed: %s%d", ChatFormatting.GRAY, ChatFormatting.WHITE, this.blocksPlaced));
      return text;
   }

   private void tickDoubleMine() {
      if (this.normalMining != null) {
         if (this.normalMining.shouldRemove()) {
            this.mc
               .getConnection()
               .send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, this.normalMining.blockPos, this.normalMining.direction));
            this.normalMining = null;
            HighwayBuilder.DoubleMineBlock.rateLimited = true;
         } else if (this.mc.level.getBlockState(this.normalMining.blockPos).getBlock() != this.normalMining.block) {
            this.normalMining = null;
            this.blocksBroken++;
            this.count++;
            HighwayBuilder.DoubleMineBlock.rateLimited = false;
         } else if (this.normalMining.isReady()) {
            this.normalMining.stopDestroying();
         }

         this.mc.player.swing(InteractionHand.MAIN_HAND);
      }

      if (this.packetMining != null) {
         if (this.packetMining.shouldRemove()) {
            this.packetMining = null;
         } else if (this.mc.level.getBlockState(this.packetMining.blockPos).getBlock() != this.packetMining.block) {
            this.packetMining = null;
            this.blocksBroken++;
            this.count++;
         }
      }
   }

   public enum BlockadeType {
      Full(6),
      Partial(4),
      Shulker(3);

      public final int columns;

      BlockadeType(int columns) {
         this.columns = columns;
      }
   }

   private class DiagonalBlockPosProvider implements HighwayBuilder.IBlockPosProvider {
      private final MBlockPos pos = new MBlockPos();
      private final MBlockPos pos2 = new MBlockPos();

      @Override
      public HighwayBuilder.MBPIterator getFront() {
         this.pos
            .coerceBlockLevel(HighwayBuilder.this.mc.player)
            .offset(HighwayBuilder.this.dir.rotateLeft())
            .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft() - 1);
         return new HighwayBuilder.MBPIterator() {
            private int i;
            private int w;
            private int y;
            private int pi;
            private int pw;
            private int py;

            @Override
            public boolean hasNext() {
               return this.i < 2 && this.w < HighwayBuilder.this.width.get() && this.y < HighwayBuilder.this.height.get();
            }

            public MBlockPos next() {
               DiagonalBlockPosProvider.this.pos2.set(DiagonalBlockPosProvider.this.pos).offset(HighwayBuilder.this.rightDir, this.w).add(0, this.y++, 0);
               if (this.y >= HighwayBuilder.this.height.get()) {
                  this.y = 0;
                  this.w++;
                  if (this.w >= (this.i == 0 ? HighwayBuilder.this.width.get() - 1 : HighwayBuilder.this.width.get())) {
                     this.w = 0;
                     this.i++;
                     DiagonalBlockPosProvider.this.pos
                        .coerceBlockLevel(HighwayBuilder.this.mc.player)
                        .offset(HighwayBuilder.this.dir)
                        .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
                  }
               }

               return DiagonalBlockPosProvider.this.pos2;
            }

            private void initPos() {
               if (this.i == 0) {
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .offset(HighwayBuilder.this.dir.rotateLeft())
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft() - 1);
               } else {
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .offset(HighwayBuilder.this.dir)
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
               }
            }

            @Override
            public void save() {
               this.pi = this.i;
               this.pw = this.w;
               this.py = this.y;
               this.i = this.w = this.y = 0;
               this.initPos();
            }

            @Override
            public void restore() {
               this.i = this.pi;
               this.w = this.pw;
               this.y = this.py;
               this.initPos();
            }
         };
      }

      @Override
      public HighwayBuilder.MBPIterator getFloor() {
         this.pos
            .coerceBlockLevel(HighwayBuilder.this.mc.player)
            .add(0, -1, 0)
            .offset(HighwayBuilder.this.dir.rotateLeft())
            .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft() - 1);
         return new HighwayBuilder.MBPIterator() {
            private int i;
            private int w;
            private int pi;
            private int pw;

            @Override
            public boolean hasNext() {
               return this.i < 2 && this.w < HighwayBuilder.this.width.get();
            }

            public MBlockPos next() {
               DiagonalBlockPosProvider.this.pos2.set(DiagonalBlockPosProvider.this.pos).offset(HighwayBuilder.this.rightDir, this.w++);
               if (this.w >= (this.i == 0 ? HighwayBuilder.this.width.get() - 1 : HighwayBuilder.this.width.get())) {
                  this.w = 0;
                  this.i++;
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .add(0, -1, 0)
                     .offset(HighwayBuilder.this.dir)
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
               }

               return DiagonalBlockPosProvider.this.pos2;
            }

            private void initPos() {
               if (this.i == 0) {
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .add(0, -1, 0)
                     .offset(HighwayBuilder.this.dir.rotateLeft())
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft() - 1);
               } else {
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .add(0, -1, 0)
                     .offset(HighwayBuilder.this.dir)
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
               }
            }

            @Override
            public void save() {
               this.pi = this.i;
               this.pw = this.w;
               this.i = this.w = 0;
               this.initPos();
            }

            @Override
            public void restore() {
               this.i = this.pi;
               this.w = this.pw;
               this.initPos();
            }
         };
      }

      @Override
      public HighwayBuilder.MBPIterator getRailings(final int state) {
         this.pos
            .coerceBlockLevel(HighwayBuilder.this.mc.player)
            .offset(HighwayBuilder.this.dir.rotateLeft())
            .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
         return new HighwayBuilder.MBPIterator() {
            private int i;
            private int y = state;
            private int pi;
            private int py;

            @Override
            public boolean hasNext() {
               return this.i < 2 && this.y < (state == 1 ? HighwayBuilder.this.height.get() : state + 1);
            }

            public MBlockPos next() {
               DiagonalBlockPosProvider.this.pos2.set(DiagonalBlockPosProvider.this.pos).add(0, this.y++, 0);
               if (this.y >= (state == 1 ? HighwayBuilder.this.height.get() : state + 1)) {
                  this.y = state;
                  this.i++;
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .offset(HighwayBuilder.this.dir.rotateRight())
                     .offset(HighwayBuilder.this.rightDir, HighwayBuilder.this.getWidthRight());
               }

               return DiagonalBlockPosProvider.this.pos2;
            }

            private void initPos() {
               if (this.i == 0) {
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .offset(HighwayBuilder.this.dir.rotateLeft())
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
               } else {
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .offset(HighwayBuilder.this.dir.rotateRight())
                     .offset(HighwayBuilder.this.rightDir, HighwayBuilder.this.getWidthRight());
               }
            }

            @Override
            public void save() {
               this.pi = this.i;
               this.py = this.y;
               this.i = 0;
               this.y = state;
               this.initPos();
            }

            @Override
            public void restore() {
               this.i = this.pi;
               this.y = this.py;
               this.initPos();
            }
         };
      }

      @Override
      public HighwayBuilder.MBPIterator getLiquids() {
         final boolean m = HighwayBuilder.this.mineAboveRailings.get();
         this.pos
            .coerceBlockLevel(HighwayBuilder.this.mc.player)
            .offset(HighwayBuilder.this.dir)
            .offset(HighwayBuilder.this.dir.rotateLeft())
            .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
         return new HighwayBuilder.MBPIterator() {
            private int i;
            private int w;
            private int y;
            private int pi;
            private int pw;
            private int py;

            private int getWidth() {
               return HighwayBuilder.this.width.get() + (this.i == 0 ? 1 : 0) + (m && this.i == 1 ? 2 : 0);
            }

            @Override
            public boolean hasNext() {
               return m && this.i == 1 && this.y == HighwayBuilder.this.height.get() && this.w == this.getWidth() - 1
                  ? false
                  : this.i < 2 && this.w < this.getWidth() && this.y < HighwayBuilder.this.height.get() + 1;
            }

            private void updateW() {
               this.w++;
               if (this.w >= this.getWidth()) {
                  this.w = 0;
                  this.i++;
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .offset(HighwayBuilder.this.dir, 2)
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft() + (m ? 1 : 0));
               }
            }

            public MBlockPos next() {
               if (this.i == (m ? 1 : 0) && this.y == HighwayBuilder.this.height.get() && (this.w == 0 || this.w == this.getWidth() - 1)) {
                  this.y = 0;
                  this.updateW();
               }

               DiagonalBlockPosProvider.this.pos2.set(DiagonalBlockPosProvider.this.pos).offset(HighwayBuilder.this.rightDir, this.w).add(0, this.y++, 0);
               if (this.y >= HighwayBuilder.this.height.get() + 1) {
                  this.y = 0;
                  this.updateW();
               }

               return DiagonalBlockPosProvider.this.pos2;
            }

            private void initPos() {
               if (this.i == 0) {
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .offset(HighwayBuilder.this.dir)
                     .offset(HighwayBuilder.this.dir.rotateLeft())
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
               } else {
                  DiagonalBlockPosProvider.this.pos
                     .coerceBlockLevel(HighwayBuilder.this.mc.player)
                     .offset(HighwayBuilder.this.dir, 2)
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft() + (m ? 1 : 0));
               }
            }

            @Override
            public void save() {
               this.pi = this.i;
               this.pw = this.w;
               this.py = this.y;
               this.i = this.w = this.y = 0;
               this.initPos();
            }

            @Override
            public void restore() {
               this.i = this.pi;
               this.w = this.pw;
               this.y = this.py;
               this.initPos();
            }
         };
      }

      @Override
      public HighwayBuilder.MBPIterator getBlockade(final boolean mine, final HighwayBuilder.BlockadeType blockadeType) {
         return new HighwayBuilder.MBPIterator() {
            private int i = mine ? -1 : 0;
            private int y;
            private int pi;
            private int py;

            private MBlockPos get(int i) {
               HorizontalDirection dir2 = HighwayBuilder.this.dir.rotateLeft().rotateLeftSkipOne();
               DiagonalBlockPosProvider.this.pos.coerceBlockLevel(HighwayBuilder.this.mc.player).offset(dir2);

               return switch (i) {
                  case -1 -> DiagonalBlockPosProvider.this.pos;
                  case 0 -> DiagonalBlockPosProvider.this.pos.offset(dir2);
                  case 1 -> DiagonalBlockPosProvider.this.pos.offset(dir2.rotateLeftSkipOne());
                  case 2 -> DiagonalBlockPosProvider.this.pos.offset(dir2.rotateLeftSkipOne().opposite());
                  case 3 -> DiagonalBlockPosProvider.this.pos.offset(dir2.opposite(), 2);
                  case 4 -> DiagonalBlockPosProvider.this.pos.offset(dir2.opposite()).offset(dir2.rotateLeftSkipOne());
                  case 5 -> DiagonalBlockPosProvider.this.pos.offset(dir2.opposite()).offset(dir2.rotateLeftSkipOne().opposite());
                  default -> throw new IllegalStateException("Unexpected value: " + i);
               };
            }

            @Override
            public boolean hasNext() {
               return this.i < blockadeType.columns && this.y < 2;
            }

            public MBlockPos next() {
               MBlockPos pos = this.get(this.i).add(0, this.y, 0);
               this.y++;
               if (this.y > 1) {
                  this.y = 0;
                  this.i++;
               }

               return pos;
            }

            @Override
            public void save() {
               this.pi = this.i;
               this.py = this.y;
               this.i = this.y = 0;
            }

            @Override
            public void restore() {
               this.i = this.pi;
               this.y = this.py;
            }

            @Override
            public int placementsPerTick(HighwayBuilder b) {
               return 1;
            }
         };
      }
   }

   public static class DoubleMineBlock {
      public static boolean rateLimited = false;
      public final BlockPos blockPos;
      public final BlockState blockState;
      private final Block block;
      private final Direction direction;
      private final HighwayBuilder b;
      private final Vector3d vec3 = new Vector3d(0.0);
      private int normalStartTime;
      private int packetStartTime;
      private boolean packet;

      public DoubleMineBlock(HighwayBuilder b, BlockPos pos) {
         this.b = b;
         this.blockPos = pos;
         this.blockState = b.mc.level.getBlockState(this.blockPos);
         this.block = this.blockState.getBlock();
         this.direction = BlockUtils.getDirection(pos);
         this.packet = false;
      }

      public HighwayBuilder.DoubleMineBlock startDestroying() {
         this.b
            .mc
            .gameMode
            .startPrediction(
               this.b.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, this.blockPos, this.direction, sequence)
            );
         this.normalStartTime = this.b.mc.player.tickCount;
         return this;
      }

      public HighwayBuilder.DoubleMineBlock stopDestroying() {
         this.b
            .mc
            .gameMode
            .startPrediction(this.b.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, this.blockPos, this.direction, sequence));
         return this;
      }

      public HighwayBuilder.DoubleMineBlock packetMine() {
         this.packetStartTime = this.b.mc.player.tickCount;
         this.packet = true;
         return this.stopDestroying();
      }

      public boolean isReady() {
         return this.progress() >= (this.b.fastBreak.get() ? 0.7 : 1.0);
      }

      public boolean shouldRemove() {
         boolean distance;
         boolean var10000;
         label30: {
            distance = !this.packet
               && Utils.distance(
                     this.b.mc.player.getEyePosition().x,
                     this.b.mc.player.getEyePosition().y,
                     this.b.mc.player.getEyePosition().z,
                     this.blockPos.getX() + this.direction.getStepX(),
                     this.blockPos.getY() + this.direction.getStepY(),
                     this.blockPos.getZ() + this.direction.getStepZ()
                  )
                  > this.b.mc.player.blockInteractionRange();
            if (this.progress() > 2.0) {
               int var10001 = this.packet ? this.packetStartTime : this.normalStartTime;
               if (this.b.mc.player.tickCount - var10001 > 60) {
                  var10000 = true;
                  break label30;
               }
            }

            var10000 = false;
         }

         boolean timeout = var10000;
         return distance || timeout;
      }

      public double progress() {
         int slot = this.b.mc.player.getInventory().getSelectedSlot();
         double var10000 = BlockUtils.getBreakDelta(slot, this.blockState);
         int var10002 = this.packet ? this.packetStartTime : this.normalStartTime;
         return var10000 * (this.b.mc.player.tickCount - var10002 + 1);
      }

      public void renderLetter() {
         this.vec3.set(this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.5, this.blockPos.getZ() + 0.5);
         if (NametagUtils.to2D(this.vec3, 2.0)) {
            NametagUtils.begin(this.vec3);
            TextRenderer.get().begin(1.0, false, true);
            String letter = this.packet ? "P" : "N";
            double w = TextRenderer.get().getWidth(letter) / 2.0;
            TextRenderer.get().render(letter, -w, 0.0, Color.WHITE, true);
            TextRenderer.get().end();
            NametagUtils.end();
         }
      }
   }

   public enum Floor {
      Replace,
      PlaceMissing;
   }

   private interface IBlockPosProvider {
      HighwayBuilder.MBPIterator getFront();

      HighwayBuilder.MBPIterator getFloor();

      HighwayBuilder.MBPIterator getRailings(int var1);

      HighwayBuilder.MBPIterator getLiquids();

      HighwayBuilder.MBPIterator getBlockade(boolean var1, HighwayBuilder.BlockadeType var2);
   }

   private interface MBPIterator extends Iterator<MBlockPos>, Iterable<MBlockPos> {
      void save();

      void restore();

      @NotNull
      @Override
      default Iterator<MBlockPos> iterator() {
         return this;
      }

      default int placementsPerTick(HighwayBuilder b) {
         return b.placementsPerTick.get();
      }
   }

   private static class MBPIteratorFilter implements HighwayBuilder.MBPIterator {
      private final HighwayBuilder.MBPIterator it;
      private final Predicate<MBlockPos> predicate;
      private MBlockPos pos;
      private boolean isOld = true;
      private boolean pisOld = true;

      public MBPIteratorFilter(HighwayBuilder.MBPIterator it, Predicate<MBlockPos> predicate) {
         this.it = it;
         this.predicate = predicate;
      }

      @Override
      public void save() {
         this.it.save();
         this.pisOld = this.isOld;
         this.isOld = true;
      }

      @Override
      public void restore() {
         this.it.restore();
         this.isOld = this.pisOld;
      }

      @Override
      public boolean hasNext() {
         if (this.isOld) {
            this.isOld = false;

            for (this.pos = null; this.it.hasNext(); this.pos = null) {
               this.pos = this.it.next();
               if (this.predicate.test(this.pos)) {
                  return true;
               }
            }
         }

         return this.pos != null && this.predicate.test(this.pos);
      }

      public MBlockPos next() {
         this.isOld = true;
         return this.pos;
      }
   }

   private class RestockTask {
      public boolean materials;
      public boolean pickaxes;
      public boolean food;
      private final HighwayBuilder b;

      public RestockTask(HighwayBuilder b) {
         this.b = b;
      }

      public void setMaterials() {
         this.setTask(0);
      }

      public void setPickaxes() {
         this.setTask(1);
      }

      public void setFood() {
         this.setTask(2);
      }

      private void setTask(@Range(from = 0L, to = 2L) int value) {
         this.complete();
         switch (value) {
            case 0:
               this.materials = true;
               break;
            case 1:
               this.pickaxes = true;
               break;
            case 2:
               this.food = true;
         }

         HighwayBuilder.this.setState(HighwayBuilder.State.Restock);
         this.b.info("Starting new restock task for " + this.item());
      }

      public void complete() {
         this.materials = false;
         this.pickaxes = false;
         this.food = false;
      }

      public boolean tasksInactive() {
         return !this.materials && !this.pickaxes && !this.food;
      }

      public String item() {
         if (this.materials) {
            return "building materials";
         } else if (this.pickaxes) {
            return "pickaxes";
         } else {
            return this.food ? "food" : "unknown";
         }
      }
   }

   public enum Rotation {
      None(false, false),
      Mine(true, false),
      Place(false, true),
      Both(true, true);

      public final boolean mine;
      public final boolean place;

      Rotation(boolean mine, boolean place) {
         this.mine = mine;
         this.place = place;
      }
   }

   private enum State {
      Center {
         @Override
         protected void start(HighwayBuilder b) {
            if (b.mc.player.position().closerThan(Vec3.atBottomCenterOf(b.mc.player.blockPosition()), 0.1)) {
               this.stop(b);
            }
         }

         @Override
         protected void tick(HighwayBuilder b) {
            double x = Math.abs(b.mc.player.getX() - (int)b.mc.player.getX()) - 0.5;
            double z = Math.abs(b.mc.player.getZ() - (int)b.mc.player.getZ()) - 0.5;
            boolean isX = Math.abs(x) <= 0.1;
            boolean isZ = Math.abs(z) <= 0.1;
            if (isX && isZ) {
               this.stop(b);
            } else {
               b.mc.player.setYRot(0.0F);
               if (!isZ) {
                  b.input.forward(z < 0.0);
                  b.input.backward(z > 0.0);
                  if (b.mc.player.getZ() < 0.0) {
                     boolean forward = b.input.keyPresses.forward();
                     b.input.forward(b.input.keyPresses.backward());
                     b.input.backward(forward);
                  }
               }

               if (!isX) {
                  b.input.right(x > 0.0);
                  b.input.left(x < 0.0);
                  if (b.mc.player.getX() < 0.0) {
                     boolean right = b.input.keyPresses.right();
                     b.input.right(b.input.keyPresses.left());
                     b.input.left(right);
                  }
               }

               b.input.sneak(true);
            }
         }

         private void stop(HighwayBuilder b) {
            b.input.stop();
            b.mc.player.setDeltaMovement(0.0, 0.0, 0.0);
            b.mc
               .player
               .setPos(
                  (int)b.mc.player.getX() + (b.mc.player.getX() < 0.0 ? -0.5 : 0.5),
                  b.mc.player.getY(),
                  (int)b.mc.player.getZ() + (b.mc.player.getZ() < 0.0 ? -0.5 : 0.5)
               );
            b.setState(b.lastState);
         }
      },
      Forward {
         @Override
         protected void start(HighwayBuilder b) {
            this.checkTasks(b);
            if (b.state == Forward) {
               b.mc.player.setYRot(b.dir.yaw);
            }
         }

         @Override
         protected void tick(HighwayBuilder b) {
            this.checkTasks(b);
            if (b.state == Forward) {
               b.input.forward(true);
            }
         }

         private void checkTasks(HighwayBuilder b) {
            if (b.destroyCrystalTraps.get() && this.isCrystalTrap(b)) {
               b.setState(DefuseCrystalTraps);
            } else if (this.needsToPlace(b, b.blockPosProvider.getLiquids(), true)) {
               b.setState(FillLiquids);
            } else if (this.needsToMine(b, b.blockPosProvider.getFront(), true)) {
               b.setState(MineFront);
            } else if (b.floor.get() == HighwayBuilder.Floor.Replace && this.needsToMine(b, b.blockPosProvider.getFloor(), false)) {
               b.setState(MineFloor);
            } else if (b.railings.get() && this.needsToMine(b, b.blockPosProvider.getRailings(0), false)) {
               b.setState(MineRailings);
            } else if (b.mineAboveRailings.get() && this.needsToMine(b, b.blockPosProvider.getRailings(1), true)) {
               b.setState(MineAboveRailings);
            } else if (b.railings.get() && this.needsToPlace(b, b.blockPosProvider.getRailings(0), false)) {
               if (b.cornerBlock.get() && this.needsToPlace(b, b.blockPosProvider.getRailings(-1), false)) {
                  b.setState(PlaceCornerBlock);
               } else {
                  b.setState(PlaceRailings);
               }
            } else if (this.needsToPlace(b, b.blockPosProvider.getFloor(), false)) {
               b.setState(PlaceFloor);
            }
         }

         private boolean needsToMine(HighwayBuilder b, HighwayBuilder.MBPIterator it, boolean mineBlocksToPlace) {
            for (MBlockPos pos : it) {
               if (b.canMine(pos, mineBlocksToPlace)) {
                  return true;
               }
            }

            return false;
         }

         private boolean needsToPlace(HighwayBuilder b, HighwayBuilder.MBPIterator it, boolean liquids) {
            for (MBlockPos pos : it) {
               if (b.canPlace(pos, liquids)) {
                  return true;
               }
            }

            return false;
         }

         private boolean isCrystalTrap(HighwayBuilder b) {
            for (Entity entity : b.mc.level.entitiesForRendering()) {
               if (entity instanceof EndCrystal endCrystal
                  && !PlayerUtils.isWithin(endCrystal, 12.0)
                  && PlayerUtils.isWithin(endCrystal, 24.0)
                  && !b.ignoreCrystals.contains(endCrystal)) {
                  Vec3 vec1 = new Vec3(0.0, 0.0, 0.0);
                  Vec3 vec2 = new Vec3(0.0, 0.0, 0.0);
                  ((IVec3)vec1).meteor$set(b.mc.player.getX(), b.mc.player.getY() + b.mc.player.getEyeHeight(), b.mc.player.getZ());
                  ((IVec3)vec2).meteor$set(entity.getX(), entity.getY() + 0.5, entity.getZ());
                  return b.mc.level.clip(new ClipContext(vec1, vec2, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, b.mc.player)).getType()
                     == Type.MISS;
               }
            }

            return false;
         }
      },
      ReLevel {
         private final MutableBlockPos pos = new MutableBlockPos();
         private BlockPos startPos;
         private int timer = 30;

         @Override
         protected void start(HighwayBuilder b) {
            this.startPos = BlockPos.containing(b.start);
         }

         @Override
         protected void tick(HighwayBuilder b) {
            Vec3 vec = b.mc.player.position().add(b.mc.player.getDeltaMovement()).add(0.0, -0.75, 0.0);
            this.pos.set(b.mc.player.getBlockX(), vec.y, b.mc.player.getBlockZ());
            if (this.pos.getY() >= b.mc.player.blockPosition().getY()) {
               this.pos.setY(b.mc.player.blockPosition().getY() - 1);
            }

            if (this.pos.getY() >= this.startPos.getY()) {
               this.pos.setY(this.startPos.getY() - 1);
            }

            if (b.mc.player.getY() > b.start.y - 0.5 && !b.mc.level.getBlockState(this.pos).canBeReplaced()) {
               b.input.jump(false);
               if (this.timer > 0) {
                  this.timer--;
               } else {
                  b.setState(Forward);
                  this.timer = 30;
               }
            } else if (b.placeTimer <= 0) {
               if (this.timer < 30) {
                  this.timer = 30;
               }

               b.input.jump(true);
               int slot = -1;
               if (this.pos.getY() == this.startPos.below().getY()) {
                  slot = this.findAndMoveToHotbar(
                     b, itemStack -> itemStack.getItem() instanceof BlockItem blockItem && b.blocksToPlace.get().contains(blockItem.getBlock())
                  );
               }

               if (slot == -1) {
                  slot = this.findAcceptablePlacementBlock(b);
                  if (slot == -1) {
                     return;
                  }
               }

               if (BlockUtils.place(this.pos.immutable(), InteractionHand.MAIN_HAND, slot, b.rotation.get().place, 100, true, true, false)) {
                  if (b.renderPlace.get()) {
                     RenderUtils.renderTickingBlock(
                        this.pos.immutable(), b.renderPlaceSideColor.get(), b.renderPlaceLineColor.get(), b.renderPlaceShape.get(), 0, 5, true, false
                     );
                  }

                  b.placeTimer = b.placeDelay.get();
               }
            }
         }

         private int findAcceptablePlacementBlock(HighwayBuilder b) {
            int slot = this.findAndMoveToHotbar(
               b, itemStack -> !(itemStack.getItem() instanceof BlockItem) ? false : b.trashItems.get().contains(itemStack.getItem())
            );
            if (slot == -1) {
               slot = this.findAndMoveToHotbar(
                  b, itemStack -> itemStack.getItem() instanceof BlockItem bi ? b.blocksToPlace.get().contains(bi.getBlock()) : false
               );
            }

            return slot != -1
               ? slot
               : this.findAndMoveToHotbar(
                  b,
                  itemStack -> {
                     if (!(itemStack.getItem() instanceof BlockItem bi)) {
                        return false;
                     } else {
                        if (Utils.isShulker(bi)) {
                           return false;
                        }

                        Block block = bi.getBlock();
                        return !Block.isShapeFullBlock(block.defaultBlockState().getCollisionShape(b.mc.level, this.pos))
                           ? false
                           : !(block instanceof FallingBlock) || !FallingBlock.isFree(b.mc.level.getBlockState(this.pos));
                     }
                  }
               );
         }
      },
      FillLiquids {
         @Override
         protected void tick(HighwayBuilder b) {
            int slot = this.findBlocksToPlacePrioritizeTrash(b);
            if (slot != -1) {
               this.place(
                  b, new HighwayBuilder.MBPIteratorFilter(b.blockPosProvider.getLiquids(), pos -> !pos.getState().getFluidState().isEmpty()), slot, Forward
               );
            }
         }
      },
      MineFront {
         @Override
         protected void start(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getFront(), true, Forward, this);
         }

         @Override
         protected void tick(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getFront(), true, Forward, this);
         }
      },
      MineFloor {
         @Override
         protected void start(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getFloor(), false, Forward, this);
         }

         @Override
         protected void tick(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getFloor(), false, Forward, this);
         }
      },
      MineRailings {
         @Override
         protected void start(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getRailings(0), false, Forward, this);
         }

         @Override
         protected void tick(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getRailings(0), false, Forward, this);
         }
      },
      MineAboveRailings {
         @Override
         protected void start(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getRailings(1), true, Forward, this);
         }

         @Override
         protected void tick(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getRailings(1), true, Forward, this);
         }
      },
      PlaceCornerBlock {
         @Override
         protected void start(HighwayBuilder b) {
            int slot = this.findBlocksToPlacePrioritizeTrash(b);
            if (slot != -1) {
               this.place(b, b.blockPosProvider.getRailings(-1), slot, Forward);
            }
         }

         @Override
         protected void tick(HighwayBuilder b) {
            int slot = this.findBlocksToPlacePrioritizeTrash(b);
            if (slot != -1) {
               this.place(b, b.blockPosProvider.getRailings(-1), slot, Forward);
            }
         }
      },
      PlaceRailings {
         @Override
         protected void start(HighwayBuilder b) {
            int slot = this.findBlocksToPlace(b);
            if (slot != -1) {
               this.place(b, b.blockPosProvider.getRailings(0), slot, Forward);
            }
         }

         @Override
         protected void tick(HighwayBuilder b) {
            int slot = this.findBlocksToPlace(b);
            if (slot != -1) {
               this.place(b, b.blockPosProvider.getRailings(0), slot, Forward);
            }
         }
      },
      PlaceFloor {
         @Override
         protected void start(HighwayBuilder b) {
            int slot = this.findBlocksToPlace(b);
            if (slot != -1) {
               this.place(b, b.blockPosProvider.getFloor(), slot, Forward);
            }
         }

         @Override
         protected void tick(HighwayBuilder b) {
            int slot = this.findBlocksToPlace(b);
            if (slot != -1) {
               this.place(b, b.blockPosProvider.getFloor(), slot, Forward);
            }
         }
      },
      ThrowOutTrash {
         private int skipSlot;
         private boolean timerEnabled;
         private boolean firstTick;
         private boolean threwItems;
         private int timer;
         private static final ItemStack[] ITEMS = new ItemStack[27];

         @Override
         protected void start(HighwayBuilder b) {
            int biggestCount = 0;

            for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
               ItemStack itemStack = b.mc.player.getInventory().getItem(i);
               if (itemStack.getItem() instanceof BlockItem && b.trashItems.get().contains(itemStack.getItem()) && itemStack.getCount() > biggestCount) {
                  biggestCount = itemStack.getCount();
                  this.skipSlot = i;
                  if (biggestCount >= 64) {
                     break;
                  }
               }
            }

            if (biggestCount == 0) {
               this.skipSlot = -1;
            }

            this.timerEnabled = false;
            this.firstTick = true;
            this.threwItems = false;
         }

         @Override
         protected void tick(HighwayBuilder b) {
            if (this.timerEnabled) {
               if (this.timer > 0) {
                  this.timer--;
               } else {
                  b.setState(b.lastState);
               }
            } else {
               b.mc.player.setYRot(b.dir.opposite().yaw);
               b.mc.player.setXRot(-25.0F);
               if (this.firstTick) {
                  this.firstTick = false;
               } else if (!b.mc.player.containerMenu.getCarried().isEmpty()) {
                  InvUtils.dropHand();
               } else {
                  for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                     if (i != this.skipSlot) {
                        ItemStack itemStack = b.mc.player.getInventory().getItem(i);
                        if (b.trashItems.get().contains(itemStack.getItem())) {
                           InvUtils.drop().slot(i);
                           this.threwItems = true;
                           return;
                        }

                        if (b.ejectUselessShulkers.get() && Utils.isShulker(itemStack.getItem())) {
                           Utils.getItemsInContainerItem(itemStack, ITEMS);
                           boolean eject = true;

                           for (ItemStack stack : ITEMS) {
                              if (stack.getItem() instanceof BlockItem bi
                                 && (
                                    b.blocksToPlace.get().contains(bi.getBlock()) || b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && bi == Items.ENDER_CHEST
                                 )) {
                                 eject = false;
                                 break;
                              }

                              if (stack.is(ItemTags.PICKAXES)) {
                                 eject = false;
                                 break;
                              }

                              if (Utils.isFood(stack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(stack.getItem())) {
                                 eject = false;
                                 break;
                              }
                           }

                           if (eject) {
                              InvUtils.drop().slot(i);
                              this.threwItems = true;
                              return;
                           }
                        }
                     }
                  }

                  this.timerEnabled = true;
                  this.timer = this.threwItems ? 10 : 1;
               }
            }
         }
      },
      PlaceEChestBlockade {
         @Override
         protected void tick(HighwayBuilder b) {
            int slot = this.findBlocksToPlacePrioritizeTrash(b);
            if (slot != -1) {
               this.place(b, b.blockPosProvider.getBlockade(false, b.blockadeType.get()), slot, MineEnderChests);
            }
         }
      },
      MineEChestBlockade {
         @Override
         protected void tick(HighwayBuilder b) {
            this.mine(b, b.blockPosProvider.getBlockade(true, b.blockadeType.get()), true, Center, Forward);
         }
      },
      MineEnderChests {
         private static final MBlockPos pos = new MBlockPos();
         private int minimumObsidian;
         private boolean first;
         private boolean primed;
         private boolean stopTimerEnabled;
         private int stopTimer;
         private int moveTimer;
         private int rebreakTimer;
         private int timeout;

         @Override
         protected void start(HighwayBuilder b) {
            if (b.lastState != Center && b.lastState != ThrowOutTrash && b.lastState != PlaceEChestBlockade) {
               b.setState(Center);
            } else if (b.lastState == Center) {
               b.setState(ThrowOutTrash);
            } else if (b.lastState == ThrowOutTrash) {
               b.setState(PlaceEChestBlockade);
            } else {
               int emptySlots = 0;

               for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                  if (b.mc.player.getInventory().getItem(i).isEmpty()) {
                     emptySlots++;
                  }
               }

               if (emptySlots == 0) {
                  b.error("No empty slots.");
               } else {
                  int minimumSlots = Math.max(emptySlots - b.minEmpty.get(), 1);
                  this.minimumObsidian = minimumSlots * 64;
                  this.first = true;
                  this.moveTimer = this.timeout = 0;
                  this.stopTimerEnabled = false;
                  this.primed = false;
               }
            }
         }

         @Override
         protected void tick(HighwayBuilder b) {
            if (this.stopTimerEnabled) {
               if (this.stopTimer > 0) {
                  this.stopTimer--;
               } else {
                  b.setState(MineEChestBlockade);
               }
            } else {
               HorizontalDirection dir = b.dir.diagonal ? b.dir.rotateLeft().rotateLeftSkipOne() : b.dir.opposite();
               pos.set(b.mc.player).offset(dir);
               if (this.moveTimer > 0) {
                  b.mc.player.setYRot(dir.yaw);
                  b.input.forward(this.moveTimer > 2);
                  this.moveTimer--;
               } else {
                  int obsidianCount = 0;
                  double var10004 = pos.x;
                  double var10005 = pos.y;
                  double var10006 = pos.z;

                  for (Entity entity : b.mc.level.getEntities(b.mc.player, new AABB(var10004, var10005, var10006, pos.x + 1, pos.y + 2, pos.z + 1))) {
                     if (entity instanceof ItemEntity itemEntity && itemEntity.getItem().getItem() == Items.OBSIDIAN) {
                        obsidianCount += itemEntity.getItem().getCount();
                     }
                  }

                  for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                     ItemStack itemStack = b.mc.player.getInventory().getItem(i);
                     if (itemStack.getItem() == Items.OBSIDIAN) {
                        obsidianCount += itemStack.getCount();
                     }
                  }

                  if (obsidianCount >= this.minimumObsidian) {
                     this.stopTimerEnabled = true;
                     this.stopTimer = 12;
                  } else {
                     BlockPos bp = pos.getBlockPos();
                     BlockState blockState = b.mc.level.getBlockState(bp);
                     if (blockState.getBlock() == Blocks.ENDER_CHEST) {
                        if (b.mc.screen instanceof ContainerScreen screen) {
                           if (screen.getMenu().containerId != b.containerId) {
                              return;
                           }

                           b.mc.screen.onClose();
                        }

                        if (!EChestMemory.isKnown()) {
                           if (b.rotation.get().place) {
                              Rotations.rotate(
                                 Rotations.getYaw(bp),
                                 Rotations.getPitch(bp),
                                 () -> b.mc
                                    .gameMode
                                    .useItemOn(b.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bp), Direction.UP, bp, false))
                              );
                           } else {
                              b.mc.gameMode.useItemOn(b.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bp), Direction.UP, bp, false));
                           }

                           return;
                        }

                        if (this.first) {
                           this.moveTimer = 8;
                           this.first = false;
                           return;
                        }

                        int slot = this.findAndMoveBestToolToHotbar(b, blockState, true);
                        if (slot == -1) {
                           b.error("Cannot find pickaxe without silk touch to mine ender chests.");
                           return;
                        }

                        InvUtils.swap(slot, false);
                        if (b.rebreakEchests.get() && this.primed) {
                           this.timeout++;
                           if (this.timeout > 60) {
                              this.primed = false;
                              this.timeout = 0;
                              return;
                           }

                           if (this.rebreakTimer > 0) {
                              this.rebreakTimer--;
                              return;
                           }

                           this.rebreakTimer = b.rebreakTimer.get();
                           if (b.rotation.get().mine) {
                              Rotations.rotate(
                                 Rotations.getYaw(bp),
                                 Rotations.getPitch(bp),
                                 () -> b.mc
                                    .gameMode
                                    .startPrediction(
                                       b.mc.level,
                                       sequence -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, bp, BlockUtils.getDirection(bp), sequence)
                                    )
                              );
                           } else {
                              b.mc
                                 .gameMode
                                 .startPrediction(
                                    b.mc.level,
                                    sequence -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, bp, BlockUtils.getDirection(bp), sequence)
                                 );
                           }
                        } else if (b.rotation.get().mine) {
                           Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () -> BlockUtils.breakBlock(bp, true));
                        } else {
                           BlockUtils.breakBlock(bp, true);
                        }
                     } else {
                        int slot = this.findAndMoveToHotbar(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST);
                        if (slot == -1 || this.countItem(b, stack -> stack.getItem().equals(Items.ENDER_CHEST)) <= b.saveEchests.get()) {
                           this.stopTimerEnabled = true;
                           this.stopTimer = 12;
                           return;
                        }

                        if (this.countItem(b, stack -> stack.is(ItemTags.PICKAXES)) <= b.savePickaxes.get()
                           && (b.searchEnderChest.get() || b.searchShulkers.get())) {
                           b.restockTask.setPickaxes();
                        }

                        if (!this.first) {
                           this.primed = true;
                        }

                        BlockUtils.place(bp, InteractionHand.MAIN_HAND, slot, b.rotation.get().place, 0, true, true, false);
                        this.timeout = 0;
                     }
                  }
               }
            }
         }
      },
      Restock {
         private static final MBlockPos pos = new MBlockPos();
         private static final ItemStack[] ITEMS = new ItemStack[27];
         private int minimumSlots;
         private int stopTimer;
         private int delayTimer;
         private boolean breakContainer;
         private boolean indicateStopping;
         private Predicate<ItemStack> shulkerPredicate;
         private int slot = -1;

         @Override
         protected void start(HighwayBuilder b) {
            this.slot = -1;
            if (this.shulkerPredicate == null) {
               this.setShulkerPredicate(b);
            }

            if (b.restockTask.tasksInactive()) {
               b.setState(Forward);
            } else if (b.lastState != Center && b.lastState != ThrowOutTrash && b.lastState != PlaceShulkerBlockade && b.lastState != this) {
               b.setState(Center);
            } else if (b.lastState == Center) {
               b.setState(ThrowOutTrash);
            } else {
               if (this.slot == -1 && b.searchShulkers.get()) {
                  this.slot = this.findAndMoveToHotbar(b, this.shulkerPredicate);
                  if (this.slot != -1 && b.lastState != PlaceShulkerBlockade) {
                     b.setState(PlaceShulkerBlockade);
                  }
               }

               if (this.slot == -1 && b.searchEnderChest.get() && this.countItem(b, stackx -> stackx.getItem().equals(Items.ENDER_CHEST)) > 0) {
                  boolean stop = EChestMemory.isKnown();
                  if (EChestMemory.isKnown()) {
                     for (ItemStack stack : EChestMemory.ITEMS) {
                        if (b.restockTask.materials
                           && stack.getItem() instanceof BlockItem bi
                           && (b.blocksToPlace.get().contains(bi.getBlock()) || b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && bi == Items.ENDER_CHEST)) {
                           stop = false;
                           break;
                        }

                        if (b.restockTask.pickaxes && stack.is(ItemTags.PICKAXES)) {
                           stop = false;
                           break;
                        }

                        if (b.restockTask.food && Utils.isFood(stack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(stack.getItem())) {
                           stop = false;
                           break;
                        }

                        if (b.searchShulkers.get() && this.shulkerPredicate.test(stack)) {
                           stop = false;
                           break;
                        }
                     }
                  }

                  if (!stop) {
                     this.slot = this.findAndMoveToHotbar(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST);
                  }
               }

               if (this.slot != -1) {
                  int restockSlots = -b.minEmpty.get();

                  for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                     if (b.mc.player.getInventory().getItem(i).isEmpty()) {
                        restockSlots++;
                     }
                  }

                  if (restockSlots <= 0) {
                     b.error("No empty slots for restocking items.");
                  } else {
                     this.minimumSlots = b.restockTask.materials ? restockSlots : 1;
                     HorizontalDirection dir = b.dir.diagonal ? b.dir.rotateLeft().rotateLeftSkipOne() : b.dir.opposite();
                     pos.set(b.mc.player).offset(dir);
                     this.breakContainer = b.mc.level.getBlockState(pos.getBlockPos()).getBlock() == Blocks.ENDER_CHEST;
                     this.indicateStopping = false;
                     this.delayTimer = b.inventoryDelay.get();
                  }
               } else {
                  boolean restockOccurred = b.restockTask.materials
                        && (
                           this.hasItem(b, stackx -> stackx.getItem() instanceof BlockItem bix && b.blocksToPlace.get().contains(bix.getBlock()))
                              || b.blocksToPlace.get().contains(Blocks.OBSIDIAN)
                                 && this.countItem(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST) > b.saveEchests.get()
                        )
                     || b.restockTask.pickaxes && this.countItem(b, itemStack -> itemStack.is(ItemTags.PICKAXES)) > b.savePickaxes.get()
                     || b.restockTask.food
                        && this.hasItem(
                           b, itemStack -> Utils.isFood(itemStack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem())
                        );
                  if (restockOccurred) {
                     b.setState(ThrowOutTrash, Forward);
                  } else {
                     b.error("Unable to perform restock for '" + b.restockTask.item() + "'.");
                  }
               }
            }
         }

         @Override
         protected void tick(HighwayBuilder b) {
            if (this.slot == -1) {
               b.error("Invalid restocking action.");
            } else if (this.indicateStopping && !this.breakContainer) {
               if (this.stopTimer > 0) {
                  this.stopTimer--;
               } else if (b.lastState == PlaceShulkerBlockade) {
                  b.setState(MineShulkerBlockade);
               } else {
                  b.setState(ThrowOutTrash, Forward);
               }
            } else if (b.restockTask.tasksInactive()) {
               b.setState(Forward);
            } else if (this.delayTimer > 0) {
               this.delayTimer--;
            } else {
               int slotsPulled = 0;
               if (b.restockTask.materials) {
                  slotsPulled += this.countSlots(b, itemStack -> itemStack.getItem() instanceof BlockItem bi && b.blocksToPlace.get().contains(bi.getBlock()));
                  if (b.blocksToPlace.get().contains(Blocks.OBSIDIAN)) {
                     slotsPulled += (this.countItem(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST) - b.saveEchests.get()) * 8 / 64;
                  }
               }

               if (b.restockTask.pickaxes) {
                  slotsPulled += this.countSlots(b, itemStack -> itemStack.is(ItemTags.PICKAXES)) - b.savePickaxes.get();
               }

               if (b.restockTask.food) {
                  slotsPulled += this.countSlots(
                     b, itemStack -> Utils.isFood(itemStack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem())
                  );
               }

               if (slotsPulled >= this.minimumSlots && !this.indicateStopping) {
                  this.indicateStopping = true;
                  this.breakContainer = true;
                  this.stopTimer = 12;
                  if (b.mc.screen != null) {
                     b.mc.screen.onClose();
                  }
               } else {
                  BlockPos blockPos = pos.getBlockPos();
                  BlockState blockState = b.mc.level.getBlockState(blockPos);
                  switch (blockState.getBlock()) {
                     case ShulkerBoxBlock var15:
                        if (b.mc.screen instanceof ShulkerBoxScreen screen) {
                           if (screen.getMenu().containerId != b.containerId) {
                              return;
                           }

                           Container inv = ((ShulkerBoxMenuAccessor)screen.getMenu()).meteor$getContainer();
                           if (this.restockItems(b, inv)) {
                              this.delayTimer = b.inventoryDelay.get();
                              return;
                           }

                           b.mc.screen.onClose();
                           this.breakContainer = true;
                        } else {
                           if (!b.searchShulkers.get()) {
                              this.breakContainer = true;
                           }

                           this.handleContainerBlock(b, blockPos);
                        }
                        break;
                     case EnderChestBlock var16:
                        if (b.mc.screen instanceof ContainerScreen screen) {
                           if (screen.getMenu().containerId != b.containerId) {
                              return;
                           }

                           Container inv = screen.getMenu().getContainer();
                           if (this.restockItems(b, inv)) {
                              this.delayTimer = b.inventoryDelay.get();
                              return;
                           }

                           if (b.searchShulkers.get()) {
                              int moveTo = InvUtils.findEmpty().slot();
                              if (moveTo != -1) {
                                 for (int i = 0; i < inv.getContainerSize(); i++) {
                                    if (this.shulkerPredicate.test(inv.getItem(i))) {
                                       InvUtils.move().fromId(i).to(moveTo);
                                       this.delayTimer = b.inventoryDelay.get();
                                       break;
                                    }
                                 }
                              }
                           }

                           b.mc.screen.onClose();
                           this.breakContainer = true;
                        } else {
                           if (!b.searchEnderChest.get()) {
                              this.breakContainer = true;
                           }

                           this.handleContainerBlock(b, blockPos);
                        }
                        break;
                     case AirBlock var17:
                        if (this.breakContainer) {
                           this.breakContainer = false;
                           if (this.indicateStopping) {
                              b.restockTask.complete();
                           } else {
                              this.start(b);
                           }

                           return;
                        }

                        BlockUtils.place(blockPos, InteractionHand.MAIN_HAND, this.slot, b.rotation.get().place, 0, true, true, false);
                        break;
                     default:
                        b.error("Invalid block at container restocking position?");
                  }
               }
            }
         }

         private boolean restockItems(HighwayBuilder b, Container inv) {
            if (b.restockTask.materials) {
               if (this.grabFromInventory(inv, itemStack -> itemStack.getItem() instanceof BlockItem bi && b.blocksToPlace.get().contains(bi.getBlock()))) {
                  return true;
               }

               if (b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && this.grabFromInventory(inv, itemStack -> itemStack.getItem() == Items.ENDER_CHEST)) {
                  return true;
               }
            }

            if (b.restockTask.pickaxes && this.grabFromInventory(inv, itemStack -> itemStack.is(ItemTags.PICKAXES))) {
               return true;
            } else {
               return b.restockTask.food
                  ? this.grabFromInventory(
                     inv, itemStack -> Utils.isFood(itemStack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem())
                  )
                  : false;
            }
         }

         private boolean grabFromInventory(Container inv, Predicate<ItemStack> filterItem) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
               if (filterItem.test(inv.getItem(i))) {
                  InvUtils.shiftClick().slotId(i);
                  return true;
               }
            }

            return false;
         }

         private void setShulkerPredicate(HighwayBuilder b) {
            this.shulkerPredicate = itemStack -> {
               if (!Utils.isShulker(itemStack.getItem())) {
                  return false;
               }

               Utils.getItemsInContainerItem(itemStack, ITEMS);

               for (ItemStack stack : ITEMS) {
                  if (b.restockTask.materials
                     && stack.getItem() instanceof BlockItem bi
                     && (b.blocksToPlace.get().contains(bi.getBlock()) || b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && bi == Items.ENDER_CHEST)) {
                     return true;
                  }

                  if (b.restockTask.pickaxes && stack.is(ItemTags.PICKAXES)) {
                     return true;
                  }

                  if (b.restockTask.food && Utils.isFood(stack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(stack.getItem())) {
                     return true;
                  }
               }

               return false;
            };
         }

         private void handleContainerBlock(HighwayBuilder b, BlockPos bp) {
            if (this.breakContainer) {
               BlockState state = b.mc.level.getBlockState(bp);
               int toolSlot = this.findAndMoveBestToolToHotbar(b, state, false);
               InvUtils.swap(toolSlot, false);
               if (b.rotation.get().mine) {
                  Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () -> BlockUtils.breakBlock(bp, true));
               } else {
                  BlockUtils.breakBlock(bp, true);
               }
            } else {
               if (b.rotation.get().place) {
                  Rotations.rotate(
                     Rotations.getYaw(bp),
                     Rotations.getPitch(bp),
                     () -> b.mc.gameMode.useItemOn(b.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bp), Direction.UP, bp, false))
                  );
               } else {
                  b.mc.gameMode.useItemOn(b.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bp), Direction.UP, bp, false));
               }

               this.delayTimer = b.inventoryDelay.get();
            }
         }

         private int countSlots(HighwayBuilder b, Predicate<ItemStack> predicate) {
            int count = 0;

            for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
               ItemStack stack = b.mc.player.getInventory().getItem(i);
               if (predicate.test(stack)) {
                  count++;
               }
            }

            return count;
         }
      },
      PlaceShulkerBlockade {
         @Override
         protected void tick(HighwayBuilder b) {
            int slot = this.findBlocksToPlacePrioritizeTrash(b);
            if (slot != -1) {
               this.place(b, b.blockPosProvider.getBlockade(false, HighwayBuilder.BlockadeType.Shulker), slot, Restock);
            }
         }
      },
      MineShulkerBlockade {
         private boolean stopTimerEnabled;
         private int stopTimer;

         @Override
         protected void start(HighwayBuilder b) {
            this.stopTimerEnabled = false;
            if (b.lastState == this) {
               this.stopTimerEnabled = true;
               this.stopTimer = 12;
            }
         }

         @Override
         protected void tick(HighwayBuilder b) {
            if (!this.stopTimerEnabled) {
               this.mine(b, b.blockPosProvider.getBlockade(true, b.blockadeType.get()), true, this, this);
            } else {
               this.stopTimer--;
               if (this.stopTimer <= 0) {
                  b.setState(ThrowOutTrash, Forward);
               }
            }
         }
      },
      DefuseCrystalTraps {
         private int cooldown;
         private int shots;
         private EndCrystal target;

         @Override
         protected void start(HighwayBuilder b) {
            if (!InvUtils.find(Items.BOW).found()
               || !InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem).found() && !b.mc.player.getAbilities().instabuild) {
               b.destroyCrystalTraps.set(false);
               b.warning("No bow found to destroy crystal traps with. Toggling the setting off.");
               b.setState(Forward);
            }

            this.shots = this.cooldown = 0;
            this.target = null;
         }

         @Override
         protected void tick(HighwayBuilder b) {
            if (this.cooldown > 0) {
               this.cooldown--;
            } else {
               if (!InvUtils.testInMainHand(Items.BOW)) {
                  int slot = this.findAndMoveToHotbar(b, itemStack -> itemStack.getItem() instanceof BowItem);
                  if (slot == -1) {
                     b.destroyCrystalTraps.set(false);
                     b.warning("No bow found to destroy crystal traps with. Toggling the setting off.");
                     b.setState(Forward);
                     b.mc.gameMode.releaseUsingItem(b.mc.player);
                     b.drawingBow = false;
                     return;
                  }

                  InvUtils.swap(slot, false);
               }

               EndCrystal potentialTarget = (EndCrystal)TargetUtils.get(
                  entity -> {
                     if (entity instanceof EndCrystal endCrystal) {
                        if (PlayerUtils.isWithin(endCrystal, 12.0) || !PlayerUtils.isWithin(endCrystal, 24.0)) {
                           return false;
                        }

                        if (b.ignoreCrystals.contains(endCrystal)) {
                           return false;
                        }

                        Vec3 vec1 = new Vec3(0.0, 0.0, 0.0);
                        Vec3 vec2 = new Vec3(0.0, 0.0, 0.0);
                        ((IVec3)vec1).meteor$set(b.mc.player.getX(), b.mc.player.getY() + b.mc.player.getEyeHeight(), b.mc.player.getZ());
                        ((IVec3)vec2).meteor$set(entity.getX(), entity.getY() + 0.5, entity.getZ());
                        return b.mc
                              .level
                              .clip(new ClipContext(vec1, vec2, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, b.mc.player))
                              .getType()
                           == Type.MISS;
                     } else {
                        return false;
                     }
                  },
                  SortPriority.LowestDistance
               );
               if (this.target == null || this.target.isRemoved()) {
                  if (potentialTarget == null) {
                     b.setState(Forward);
                     b.mc.gameMode.releaseUsingItem(b.mc.player);
                     b.drawingBow = false;
                     return;
                  }

                  this.target = potentialTarget;
                  this.shots = 0;
               }

               if (this.shots >= 3) {
                  b.ignoreCrystals.add(this.target);
                  b.warning("Detected potential hangup on a crystal. Adding it to ignore list and continuing forward.");
                  b.setState(Forward);
                  b.mc.gameMode.releaseUsingItem(b.mc.player);
                  b.drawingBow = false;
               } else {
                  b.mc.player.setYRot((float)Rotations.getYaw(this.target));
                  float pitch = this.aim(b, this.target);
                  if (Float.isNaN(pitch)) {
                     b.mc.player.setXRot((float)Rotations.getPitch(this.target));
                  } else {
                     b.mc.player.setXRot(pitch);
                  }

                  if (BowItem.getPowerForTime(b.mc.player.getTicksUsingItem() - 3) >= 1.0F) {
                     b.mc.gameMode.releaseUsingItem(b.mc.player);
                     b.drawingBow = false;
                     this.cooldown = 20;
                     this.shots++;
                  } else {
                     b.drawingBow = true;
                     b.mc.gameMode.useItem(b.mc.player, InteractionHand.MAIN_HAND);
                  }
               }
            }
         }

         private float aim(HighwayBuilder b, Entity target) {
            float velocity = BowItem.getPowerForTime(b.mc.player.getTicksUsingItem());
            Vec3 pos = target.position();
            double relativeX = pos.x - b.mc.player.getX();
            double relativeY = pos.y + 0.5 - b.mc.player.getEyeY();
            double relativeZ = pos.z - b.mc.player.getZ();
            double hDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
            double hDistanceSq = hDistance * hDistance;
            float g = 0.006F;
            float velocitySq = velocity * velocity;
            return (float)(
               -Math.toDegrees(
                  Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - g * (g * hDistanceSq + 2.0 * relativeY * velocitySq))) / (g * hDistance))
               )
            );
         }
      };

      protected void start(HighwayBuilder b) {
      }

      protected abstract void tick(HighwayBuilder var1);

      protected void mine(
         HighwayBuilder b, HighwayBuilder.MBPIterator it, boolean mineBlocksToPlace, HighwayBuilder.State nextState, HighwayBuilder.State lastState
      ) {
         label104: {
            boolean breaking = false;
            boolean finishedBreaking = false;
            if (b.doubleMine.get()) {
               ArrayDeque<BlockPos> toDoubleMine = new ArrayDeque<>();
               it.save();
               it.forEach(
                  posx -> {
                     if (BlockUtils.canBreak(posx.getBlockPos(), posx.getState())
                        && (mineBlocksToPlace || !b.blocksToPlace.get().contains(posx.getState().getBlock()))
                        && !BlockUtils.canInstaBreak(posx.getBlockPos())
                        && (
                           !Modules.get().get(SpeedMine.class).instamine()
                              || posx.getState().getDestroyProgress(b.mc.player, b.mc.level, posx.getBlockPos()) <= 0.5
                        )
                        && (b.normalMining == null || !posx.getBlockPos().equals(b.normalMining.blockPos))
                        && (b.packetMining == null || !posx.getBlockPos().equals(b.packetMining.blockPos))) {
                        toDoubleMine.add(posx.getBlockPos().mutable());
                     }
                  }
               );
               it.restore();
               if (!toDoubleMine.isEmpty()) {
                  int slot = this.findAndMoveBestToolToHotbar(b, b.mc.level.getBlockState(toDoubleMine.peek()), false);
                  if (slot == -1) {
                     return;
                  }

                  InvUtils.swap(slot, false);
                  this.doubleMine(b, toDoubleMine);
               }

               if (b.normalMining != null || b.packetMining != null) {
                  break label104;
               }
            }

            for (MBlockPos pos : it) {
               if (b.count >= b.blocksPerTick.get()) {
                  return;
               }

               if (b.breakTimer > 0) {
                  return;
               }

               BlockState state = pos.getState();
               if (!state.isAir() && (mineBlocksToPlace || !b.blocksToPlace.get().contains(state.getBlock()))) {
                  int slot = this.findAndMoveBestToolToHotbar(b, state, false);
                  if (slot == -1) {
                     return;
                  }

                  InvUtils.swap(slot, false);
                  BlockPos mcPos = pos.getBlockPos();
                  boolean multiBreak = b.blocksPerTick.get() > 1 && BlockUtils.canInstaBreak(mcPos) && !b.rotation.get().mine;
                  if (BlockUtils.canBreak(mcPos)) {
                     if (b.rotation.get().mine) {
                        Rotations.rotate(Rotations.getYaw(mcPos), Rotations.getPitch(mcPos), () -> BlockUtils.breakBlock(mcPos, true));
                     } else {
                        BlockUtils.breakBlock(mcPos, true);
                     }

                     breaking = true;
                     b.breakTimer = b.breakDelay.get();
                     if (!b.lastBreakingPos.equals(pos)) {
                        b.lastBreakingPos.set(pos);
                        b.blocksBroken++;
                     }

                     b.count++;
                     if (!multiBreak) {
                        if (finishedBreaking || !breaking) {
                           b.setState(nextState, lastState);
                        }

                        return;
                     }
                  }

                  if (!it.hasNext() && BlockUtils.canInstaBreak(mcPos)) {
                     finishedBreaking = true;
                  }
               }
            }
         }

         int slot = this.findAndMoveBestToolToHotbar(b, b.normalMining != null ? b.normalMining.blockState : b.packetMining.blockState, false);
         if (slot != -1) {
            InvUtils.swap(slot, false);
         }
      }

      private void doubleMine(HighwayBuilder b, ArrayDeque<BlockPos> blocks) {
         if (b.breakTimer <= 0) {
            if (b.normalMining == null) {
               HighwayBuilder.DoubleMineBlock block = new HighwayBuilder.DoubleMineBlock(b, blocks.pop());
               b.normalMining = block.startDestroying();
               b.breakTimer = b.breakDelay.get();
               if (b.breakTimer > 0) {
                  return;
               }
            }

            if (!HighwayBuilder.DoubleMineBlock.rateLimited) {
               if (b.packetMining == null && !blocks.isEmpty()) {
                  HighwayBuilder.DoubleMineBlock block = new HighwayBuilder.DoubleMineBlock(b, blocks.pop());
                  if (block != null) {
                     b.packetMining = b.normalMining.packetMine();
                     b.normalMining = block.startDestroying();
                     b.breakTimer = b.breakDelay.get();
                  }
               }
            }
         }
      }

      protected void place(HighwayBuilder b, HighwayBuilder.MBPIterator it, int slot, HighwayBuilder.State nextState) {
         boolean placed = false;
         boolean finishedPlacing = false;

         for (MBlockPos pos : it) {
            if (b.count >= it.placementsPerTick(b)) {
               return;
            }

            if (b.placeTimer > 0) {
               return;
            }

            if (!(pos.getBlockPos().distToCenterSqr(b.mc.player.getEyePosition()) > b.placeRange.get() * b.placeRange.get())) {
               if (BlockUtils.place(pos.getBlockPos(), InteractionHand.MAIN_HAND, slot, b.rotation.get().place, 0, true, true, true)) {
                  placed = true;
                  b.blocksPlaced++;
                  b.placeTimer = b.placeDelay.get();
                  b.count++;
                  if (b.placementsPerTick.get() == 1) {
                     break;
                  }
               }

               if (!it.hasNext()) {
                  finishedPlacing = true;
               }
            }
         }

         if (finishedPlacing || !placed) {
            b.setState(nextState);
         }
      }

      private int findSlot(HighwayBuilder b, Predicate<ItemStack> predicate, boolean hotbar) {
         for (int i = hotbar ? 0 : 9; i < (hotbar ? 9 : b.mc.player.getInventory().getNonEquipmentItems().size()); i++) {
            if (predicate.test(b.mc.player.getInventory().getItem(i))) {
               return i;
            }
         }

         return -1;
      }

      protected int findHotbarSlot(HighwayBuilder b, boolean replaceTools) {
         int thrashSlot = -1;
         int slotsWithBlocks = 0;
         int slotWithLeastBlocks = -1;
         int slotWithLeastBlocksCount = Integer.MAX_VALUE;

         for (int i = 0; i < 9; i++) {
            ItemStack itemStack = b.mc.player.getInventory().getItem(i);
            if (itemStack.isEmpty()) {
               return i;
            }

            if (replaceTools && AutoTool.isTool(itemStack)) {
               return i;
            }

            if (b.trashItems.get().contains(itemStack.getItem())) {
               thrashSlot = i;
            }

            if (itemStack.getItem() instanceof BlockItem blockItem
               && (b.blocksToPlace.get().contains(blockItem.getBlock()) || b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && blockItem == Items.ENDER_CHEST)) {
               slotsWithBlocks++;
               if (itemStack.getCount() < slotWithLeastBlocksCount) {
                  slotWithLeastBlocksCount = itemStack.getCount();
                  slotWithLeastBlocks = i;
               }
            }
         }

         if (thrashSlot != -1) {
            return thrashSlot;
         }

         if (slotsWithBlocks > 0) {
            return slotWithLeastBlocks;
         }

         b.error("No empty space in hotbar.");
         return -1;
      }

      protected boolean hasItem(HighwayBuilder b, Predicate<ItemStack> predicate) {
         for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
            if (predicate.test(b.mc.player.getInventory().getItem(i))) {
               return true;
            }
         }

         return false;
      }

      protected int countItem(HighwayBuilder b, Predicate<ItemStack> predicate) {
         int count = 0;

         for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
            ItemStack stack = b.mc.player.getInventory().getItem(i);
            if (predicate.test(stack)) {
               count += stack.getCount();
            }
         }

         return count;
      }

      protected int findAndMoveToHotbar(HighwayBuilder b, Predicate<ItemStack> predicate) {
         int slot = this.findSlot(b, predicate, true);
         if (slot != -1) {
            return slot;
         }

         int hotbarSlot = this.findHotbarSlot(b, false);
         if (hotbarSlot == -1) {
            return -1;
         }

         slot = this.findSlot(b, predicate, false);
         if (slot == -1) {
            return -1;
         }

         InvUtils.move().from(slot).toHotbar(hotbarSlot);
         InvUtils.dropHand();
         return hotbarSlot;
      }

      protected int findAndMoveBestToolToHotbar(HighwayBuilder b, BlockState blockState, boolean noSilkTouch) {
         if (b.mc.player.isCreative()) {
            return b.mc.player.getInventory().getSelectedSlot();
         }

         double bestScore = -1.0;
         int bestSlot = -1;

         for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
            double score = AutoTool.getScore(
               b.mc.player.getInventory().getItem(i),
               blockState,
               false,
               false,
               AutoTool.EnchantPreference.None,
               itemStack -> noSilkTouch && Utils.hasEnchantment(itemStack, Enchantments.SILK_TOUCH)
                  ? false
                  : !b.dontBreakTools.get()
                     || itemStack.getMaxDamage() - itemStack.getDamageValue() > itemStack.getMaxDamage() * (b.breakDurability.get() / 100)
            );
            if (score > bestScore) {
               bestScore = score;
               bestSlot = i;
            }
         }

         if (bestSlot == -1) {
            return b.mc.player.getInventory().getSelectedSlot();
         }

         ItemStack bestStack = b.mc.player.getInventory().getItem(bestSlot);
         if (bestStack.is(ItemTags.PICKAXES)) {
            int count = this.countItem(b, stack -> stack.is(ItemTags.PICKAXES));
            if (count <= b.savePickaxes.get()
               && (
                  !b.restockTask.pickaxes
                     || bestStack.getMaxDamage() - bestStack.getDamageValue() <= bestStack.getMaxDamage() * (b.breakDurability.get() / 100)
               )) {
               if (b.restockTask.pickaxes || !b.searchEnderChest.get() && !b.searchShulkers.get()) {
                  b.error("Found less than the minimum amount of pickaxes required: " + count + "/" + (b.savePickaxes.get() + 1));
               } else {
                  b.restockTask.setPickaxes();
               }

               return -1;
            }
         }

         if (bestSlot < 9) {
            return bestSlot;
         }

         int hotbarSlot = this.findHotbarSlot(b, true);
         if (hotbarSlot == -1) {
            return -1;
         }

         InvUtils.move().from(bestSlot).toHotbar(hotbarSlot);
         InvUtils.dropHand();
         return hotbarSlot;
      }

      protected int findBlocksToPlace(HighwayBuilder b) {
         int slot = this.findAndMoveToHotbar(
            b, itemStack -> itemStack.getItem() instanceof BlockItem blockItem && b.blocksToPlace.get().contains(blockItem.getBlock())
         );
         if (slot != -1) {
            return slot;
         }

         if (b.mineEnderChests.get()
            && b.blocksToPlace.get().contains(Blocks.OBSIDIAN)
            && this.countItem(b, stack -> stack.getItem().equals(Items.ENDER_CHEST)) > b.saveEchests.get()) {
            b.setState(MineEnderChests);
         } else if (!b.searchEnderChest.get() && !b.searchShulkers.get()) {
            b.error("Out of blocks to place.");
         } else {
            b.restockTask.setMaterials();
         }

         return -1;
      }

      protected int findBlocksToPlacePrioritizeTrash(HighwayBuilder b) {
         int slot = this.findAndMoveToHotbar(
            b, itemStack -> !(itemStack.getItem() instanceof BlockItem) ? false : b.trashItems.get().contains(itemStack.getItem())
         );
         return slot != -1 ? slot : this.findBlocksToPlace(b);
      }
   }

   private class StraightBlockPosProvider implements HighwayBuilder.IBlockPosProvider {
      private final MBlockPos pos = new MBlockPos();
      private final MBlockPos pos2 = new MBlockPos();

      @Override
      public HighwayBuilder.MBPIterator getFront() {
         this.pos
            .coerceBlockLevel(HighwayBuilder.this.mc.player)
            .offset(HighwayBuilder.this.dir)
            .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft());
         return new HighwayBuilder.MBPIterator() {
            private int w;
            private int y;
            private int pw;
            private int py;

            @Override
            public boolean hasNext() {
               return this.w < HighwayBuilder.this.width.get() && this.y < HighwayBuilder.this.height.get();
            }

            public MBlockPos next() {
               StraightBlockPosProvider.this.pos2.set(StraightBlockPosProvider.this.pos).offset(HighwayBuilder.this.rightDir, this.w).add(0, this.y, 0);
               this.w++;
               if (this.w >= HighwayBuilder.this.width.get()) {
                  this.w = 0;
                  this.y++;
               }

               return StraightBlockPosProvider.this.pos2;
            }

            @Override
            public void save() {
               this.pw = this.w;
               this.py = this.y;
               this.w = this.y = 0;
            }

            @Override
            public void restore() {
               this.w = this.pw;
               this.y = this.py;
            }
         };
      }

      @Override
      public HighwayBuilder.MBPIterator getFloor() {
         this.pos
            .coerceBlockLevel(HighwayBuilder.this.mc.player)
            .offset(HighwayBuilder.this.dir)
            .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft())
            .add(0, -1, 0);
         return new HighwayBuilder.MBPIterator() {
            private int w;
            private int pw;

            @Override
            public boolean hasNext() {
               return this.w < HighwayBuilder.this.width.get();
            }

            public MBlockPos next() {
               return StraightBlockPosProvider.this.pos2.set(StraightBlockPosProvider.this.pos).offset(HighwayBuilder.this.rightDir, this.w++);
            }

            @Override
            public void save() {
               this.pw = this.w;
               this.w = 0;
            }

            @Override
            public void restore() {
               this.w = this.pw;
            }
         };
      }

      @Override
      public HighwayBuilder.MBPIterator getRailings(final int state) {
         this.pos.coerceBlockLevel(HighwayBuilder.this.mc.player).offset(HighwayBuilder.this.dir);
         return new HighwayBuilder.MBPIterator() {
            private int i;
            private int y = state;
            private int pi;
            private int py;

            @Override
            public boolean hasNext() {
               return this.i < 2 && this.y < (state == 1 ? HighwayBuilder.this.height.get() : state + 1);
            }

            public MBlockPos next() {
               if (this.i == 0) {
                  StraightBlockPosProvider.this.pos2
                     .set(StraightBlockPosProvider.this.pos)
                     .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft() + 1)
                     .add(0, this.y, 0);
               } else {
                  StraightBlockPosProvider.this.pos2
                     .set(StraightBlockPosProvider.this.pos)
                     .offset(HighwayBuilder.this.rightDir, HighwayBuilder.this.getWidthRight() + 1)
                     .add(0, this.y, 0);
               }

               this.y++;
               if (this.y >= (state == 1 ? HighwayBuilder.this.height.get() : state + 1)) {
                  this.y = state;
                  this.i++;
               }

               return StraightBlockPosProvider.this.pos2;
            }

            @Override
            public void save() {
               this.pi = this.i;
               this.py = this.y;
               this.i = 0;
               this.y = state;
            }

            @Override
            public void restore() {
               this.i = this.pi;
               this.y = this.py;
            }
         };
      }

      @Override
      public HighwayBuilder.MBPIterator getLiquids() {
         this.pos
            .coerceBlockLevel(HighwayBuilder.this.mc.player)
            .offset(HighwayBuilder.this.dir, 2)
            .offset(HighwayBuilder.this.leftDir, HighwayBuilder.this.getWidthLeft() + (HighwayBuilder.this.mineAboveRailings.get() ? 2 : 1));
         return new HighwayBuilder.MBPIterator() {
            private int w;
            private int y;
            private int pw;
            private int py;

            private int getWidth() {
               return HighwayBuilder.this.width.get() + (HighwayBuilder.this.mineAboveRailings.get() ? 2 : 0);
            }

            @Override
            public boolean hasNext() {
               return this.w < this.getWidth() + 2 && this.y < HighwayBuilder.this.height.get() + 1;
            }

            public MBlockPos next() {
               StraightBlockPosProvider.this.pos2.set(StraightBlockPosProvider.this.pos).offset(HighwayBuilder.this.rightDir, this.w).add(0, this.y, 0);
               this.w++;
               if (this.w >= this.getWidth() + 2) {
                  this.w = 0;
                  this.y++;
               }

               return StraightBlockPosProvider.this.pos2;
            }

            @Override
            public void save() {
               this.pw = this.w;
               this.py = this.y;
               this.w = this.y = 0;
            }

            @Override
            public void restore() {
               this.w = this.pw;
               this.y = this.py;
            }
         };
      }

      @Override
      public HighwayBuilder.MBPIterator getBlockade(final boolean mine, final HighwayBuilder.BlockadeType blockadeType) {
         return new HighwayBuilder.MBPIterator() {
            private int i = mine ? -1 : 0;
            private int y;
            private int pi;
            private int py;

            private MBlockPos get(int i) {
               StraightBlockPosProvider.this.pos.coerceBlockLevel(HighwayBuilder.this.mc.player).offset(HighwayBuilder.this.dir.opposite());

               return switch (i) {
                  case -1 -> StraightBlockPosProvider.this.pos;
                  case 0 -> StraightBlockPosProvider.this.pos.offset(HighwayBuilder.this.dir.opposite());
                  case 1 -> StraightBlockPosProvider.this.pos.offset(HighwayBuilder.this.leftDir);
                  case 2 -> StraightBlockPosProvider.this.pos.offset(HighwayBuilder.this.rightDir);
                  case 3 -> StraightBlockPosProvider.this.pos.offset(HighwayBuilder.this.dir, 2);
                  case 4 -> StraightBlockPosProvider.this.pos.offset(HighwayBuilder.this.dir).offset(HighwayBuilder.this.leftDir);
                  case 5 -> StraightBlockPosProvider.this.pos.offset(HighwayBuilder.this.dir).offset(HighwayBuilder.this.rightDir);
                  default -> throw new IllegalStateException("Unexpected value: " + i);
               };
            }

            @Override
            public boolean hasNext() {
               return this.i < blockadeType.columns && this.y < 2;
            }

            public MBlockPos next() {
               if (HighwayBuilder.this.width.get() == 1 && HighwayBuilder.this.railings.get() && this.i > 0 && this.y == 0) {
                  this.y++;
               }

               MBlockPos pos = this.get(this.i).add(0, this.y, 0);
               this.y++;
               if (this.y > 1) {
                  this.y = 0;
                  this.i++;
               }

               return pos;
            }

            @Override
            public void save() {
               this.pi = this.i;
               this.py = this.y;
               this.i = this.y = 0;
            }

            @Override
            public void restore() {
               this.i = this.pi;
               this.y = this.py;
            }

            @Override
            public int placementsPerTick(HighwayBuilder b) {
               return 1;
            }
         };
      }
   }
}
