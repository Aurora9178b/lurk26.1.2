package meteordevelopment.meteorclient.systems.modules.combat;

import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IAABB;
import meteordevelopment.meteorclient.mixininterface.IClipContext;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3d;

public class CrystalAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSwitch = this.settings.createGroup("Switch");
   private final SettingGroup sgPlace = this.settings.createGroup("Place");
   private final SettingGroup sgFacePlace = this.settings.createGroup("Face Place");
   private final SettingGroup sgBreak = this.settings.createGroup("Break");
   private final SettingGroup sgPause = this.settings.createGroup("Pause");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder().name("target-range").description("Range in which to target players.").defaultValue(10.0).min(0.0).sliderMax(16.0).build()
      );
   private final Setting<Boolean> predictMovement = this.sgGeneral
      .add(new BoolSetting.Builder().name("predict-movement").description("Predicts target movement.").defaultValue(false).build());
   private final Setting<Double> minDamage = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("min-damage")
            .description("Minimum damage the crystal needs to deal to your target.")
            .defaultValue(6.0)
            .min(0.0)
            .build()
      );
   private final Setting<Double> maxDamage = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("max-damage")
            .description("Maximum damage crystals can deal to yourself.")
            .defaultValue(6.0)
            .range(0.0, 36.0)
            .sliderMax(36.0)
            .build()
      );
   private final Setting<Boolean> antiSuicide = this.sgGeneral
      .add(new BoolSetting.Builder().name("anti-suicide").description("Will not place and break crystals if they will kill you.").defaultValue(true).build());
   private final Setting<Boolean> ignoreNakeds = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-nakeds").description("Ignore players with no items.").defaultValue(false).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates server-side towards the crystals being hit/placed.").defaultValue(true).build());
   private final Setting<CrystalAura.YawStepMode> yawStepMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("yaw-steps-mode"))
                     .description("When to run the yaw steps check."))
                  .defaultValue(CrystalAura.YawStepMode.Break))
               .visible(this.rotate::get))
            .build()
      );
   private final Setting<Double> yawSteps = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("yaw-steps")
            .description("Maximum number of degrees its allowed to rotate in one tick.")
            .defaultValue(180.0)
            .range(1.0, 180.0)
            .visible(this.rotate::get)
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(
         new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER, EntityType.WARDEN, EntityType.WITHER)
            .build()
      );
   private final Setting<CrystalAura.AutoSwitchMode> autoSwitch = this.sgSwitch
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("auto-switch"))
                  .description("Switches to crystals in your hotbar once a target is found."))
               .defaultValue(CrystalAura.AutoSwitchMode.Normal))
            .build()
      );
   private final Setting<Integer> switchDelay = this.sgSwitch
      .add(
         new IntSetting.Builder()
            .name("switch-delay")
            .description("The delay in ticks to wait to break a crystal after switching hotbar slot.")
            .defaultValue(0)
            .min(0)
            .build()
      );
   private final Setting<Boolean> noGapSwitch = this.sgSwitch
      .add(
         new BoolSetting.Builder()
            .name("no-gap-switch")
            .description("Won't auto switch if you're holding a gapple.")
            .defaultValue(true)
            .visible(() -> this.autoSwitch.get() == CrystalAura.AutoSwitchMode.Normal)
            .build()
      );
   private final Setting<Boolean> noBowSwitch = this.sgSwitch
      .add(new BoolSetting.Builder().name("no-bow-switch").description("Won't auto switch if you're holding a bow.").defaultValue(true).build());
   private final Setting<Boolean> antiWeakness = this.sgSwitch
      .add(
         new BoolSetting.Builder()
            .name("anti-weakness")
            .description("Switches to tools with so you can break crystals with the weakness effect.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> doPlace = this.sgPlace
      .add(new BoolSetting.Builder().name("place").description("If the CA should place crystals.").defaultValue(true).build());
   public final Setting<Integer> placeDelay = this.sgPlace
      .add(
         new IntSetting.Builder()
            .name("place-delay")
            .description("The delay in ticks to wait to place a crystal after it's exploded.")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .build()
      );
   private final Setting<Double> placeRange = this.sgPlace
      .add(new DoubleSetting.Builder().name("place-range").description("Range in which to place crystals.").defaultValue(4.5).min(0.0).sliderMax(6.0).build());
   private final Setting<Double> placeWallsRange = this.sgPlace
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("Range in which to place crystals when behind blocks.")
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Boolean> placement112 = this.sgPlace
      .add(new BoolSetting.Builder().name("1.12-placement").description("Uses 1.12 crystal placement.").defaultValue(false).build());
   private final Setting<CrystalAura.SupportMode> support = this.sgPlace
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("support"))
                  .description("Places a support block in air if no other position have been found."))
               .defaultValue(CrystalAura.SupportMode.Disabled))
            .build()
      );
   private final Setting<Integer> supportDelay = this.sgPlace
      .add(
         new IntSetting.Builder()
            .name("support-delay")
            .description("Delay in ticks after placing support block.")
            .defaultValue(1)
            .min(0)
            .visible(() -> this.support.get() != CrystalAura.SupportMode.Disabled)
            .build()
      );
   private final Setting<Boolean> facePlace = this.sgFacePlace
      .add(
         new BoolSetting.Builder()
            .name("face-place")
            .description("Will face-place when target is below a certain health or armor durability threshold.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Double> facePlaceHealth = this.sgFacePlace
      .add(
         new DoubleSetting.Builder()
            .name("face-place-health")
            .description("The health the target has to be at to start face placing.")
            .defaultValue(8.0)
            .min(1.0)
            .sliderMin(1.0)
            .sliderMax(36.0)
            .visible(this.facePlace::get)
            .build()
      );
   private final Setting<Double> facePlaceDurability = this.sgFacePlace
      .add(
         new DoubleSetting.Builder()
            .name("face-place-durability")
            .description("The durability threshold percentage to be able to face-place.")
            .defaultValue(2.0)
            .min(1.0)
            .sliderMin(1.0)
            .sliderMax(100.0)
            .visible(this.facePlace::get)
            .build()
      );
   private final Setting<Boolean> facePlaceArmor = this.sgFacePlace
      .add(
         new BoolSetting.Builder()
            .name("face-place-missing-armor")
            .description("Automatically starts face placing when a target misses a piece of armor.")
            .defaultValue(false)
            .visible(this.facePlace::get)
            .build()
      );
   private final Setting<Keybind> forceFacePlace = this.sgFacePlace
      .add(
         new KeybindSetting.Builder()
            .name("force-face-place")
            .description("Starts face place when this button is pressed.")
            .defaultValue(Keybind.none())
            .build()
      );
   private final Setting<Boolean> doBreak = this.sgBreak
      .add(new BoolSetting.Builder().name("break").description("If the CA should break crystals.").defaultValue(true).build());
   private final Setting<Integer> breakDelay = this.sgBreak
      .add(
         new IntSetting.Builder()
            .name("break-delay")
            .description("The delay in ticks to wait to break a crystal after it's placed.")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .build()
      );
   private final Setting<Boolean> smartDelay = this.sgBreak
      .add(new BoolSetting.Builder().name("smart-delay").description("Only breaks crystals when the target can receive damage.").defaultValue(false).build());
   private final Setting<Double> breakRange = this.sgBreak
      .add(new DoubleSetting.Builder().name("break-range").description("Range in which to break crystals.").defaultValue(4.5).min(0.0).sliderMax(6.0).build());
   private final Setting<Double> breakWallsRange = this.sgBreak
      .add(
         new DoubleSetting.Builder()
            .name("walls-range")
            .description("Range in which to break crystals when behind blocks.")
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Boolean> onlyBreakOwn = this.sgBreak
      .add(new BoolSetting.Builder().name("only-own").description("Only breaks own crystals.").defaultValue(false).build());
   private final Setting<Integer> breakAttempts = this.sgBreak
      .add(
         new IntSetting.Builder()
            .name("break-attempts")
            .description("How many times to hit a crystal before stopping to target it.")
            .defaultValue(2)
            .sliderMin(1)
            .sliderMax(5)
            .build()
      );
   private final Setting<Integer> ticksExisted = this.sgBreak
      .add(
         new IntSetting.Builder()
            .name("ticks-existed")
            .description("Amount of ticks a crystal needs to have lived for it to be attacked by CrystalAura.")
            .defaultValue(0)
            .min(0)
            .build()
      );
   private final Setting<Integer> attackFrequency = this.sgBreak
      .add(new IntSetting.Builder().name("attack-frequency").description("Maximum hits to do per second.").defaultValue(25).min(1).sliderRange(1, 30).build());
   private final Setting<Boolean> fastBreak = this.sgBreak
      .add(
         new BoolSetting.Builder()
            .name("fast-break")
            .description("Ignores break delay and tries to break the crystal as soon as it's spawned in the world.")
            .defaultValue(true)
            .build()
      );
   public final Setting<CrystalAura.PauseMode> pauseOnUse = this.sgPause
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("pause-on-use"))
                  .description("Which processes should be paused while using an item."))
               .defaultValue(CrystalAura.PauseMode.Place))
            .build()
      );
   public final Setting<CrystalAura.PauseMode> pauseOnMine = this.sgPause
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("pause-on-mine"))
                  .description("Which processes should be paused while mining a block."))
               .defaultValue(CrystalAura.PauseMode.None))
            .build()
      );
   private final Setting<Boolean> pauseOnLag = this.sgPause
      .add(new BoolSetting.Builder().name("pause-on-lag").description("Whether to pause if the server is not responding.").defaultValue(true).build());
   public final Setting<List<Module>> pauseModules = this.sgPause
      .add(
         new ModuleListSetting.Builder()
            .name("pause-modules")
            .description("Pauses while any of the selected modules are active.")
            .defaultValue(BedAura.class)
            .build()
      );
   public final Setting<Double> pauseHealth = this.sgPause
      .add(
         new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5.0)
            .range(0.0, 36.0)
            .sliderRange(0.0, 36.0)
            .build()
      );
   public final Setting<CrystalAura.SwingMode> swingMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("swing-mode"))
                  .description("How to swing when placing."))
               .defaultValue(CrystalAura.SwingMode.Both))
            .build()
      );
   private final Setting<CrystalAura.RenderMode> renderMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("render-mode")).description("The mode to render in."))
               .defaultValue(CrystalAura.RenderMode.Normal))
            .build()
      );
   private final Setting<Boolean> renderPlace = this.sgRender
      .add(
         new BoolSetting.Builder()
            .name("render-place")
            .description("Renders a block overlay over the block the crystals are being placed on.")
            .defaultValue(true)
            .visible(() -> this.renderMode.get() == CrystalAura.RenderMode.Normal)
            .build()
      );
   private final Setting<Integer> placeRenderTime = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("place-time")
            .description("How long to render placements.")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .visible(() -> this.renderMode.get() == CrystalAura.RenderMode.Normal && this.renderPlace.get())
            .build()
      );
   private final Setting<Boolean> renderBreak = this.sgRender
      .add(
         new BoolSetting.Builder()
            .name("render-break")
            .description("Renders a block overlay over the block the crystals are broken on.")
            .defaultValue(false)
            .visible(() -> this.renderMode.get() == CrystalAura.RenderMode.Normal)
            .build()
      );
   private final Setting<Integer> breakRenderTime = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("break-time")
            .description("How long to render breaking for.")
            .defaultValue(13)
            .min(0)
            .sliderMax(20)
            .visible(() -> this.renderMode.get() == CrystalAura.RenderMode.Normal && this.renderBreak.get())
            .build()
      );
   private final Setting<Integer> smoothness = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("smoothness")
            .description("How smoothly the render should move around.")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .visible(() -> this.renderMode.get() == CrystalAura.RenderMode.Smooth)
            .build()
      );
   private final Setting<Double> height = this.sgRender
      .add(
         new DoubleSetting.Builder()
            .name("height")
            .description("How tall the gradient should be.")
            .defaultValue(0.7)
            .min(0.0)
            .sliderMax(1.0)
            .visible(() -> this.renderMode.get() == CrystalAura.RenderMode.Gradient)
            .build()
      );
   private final Setting<Integer> renderTime = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("render-time")
            .description("How long to render placements.")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .visible(() -> this.renderMode.get() == CrystalAura.RenderMode.Smooth || this.renderMode.get() == CrystalAura.RenderMode.Fading)
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                     .description("How the shapes are rendered."))
                  .defaultValue(ShapeMode.Both))
               .visible(() -> this.renderMode.get() != CrystalAura.RenderMode.None))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 45))
            .visible(() -> this.shapeMode.get().sides() && this.renderMode.get() != CrystalAura.RenderMode.None)
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255))
            .visible(() -> this.shapeMode.get().lines() && this.renderMode.get() != CrystalAura.RenderMode.None)
            .build()
      );
   private final Setting<Boolean> renderDamageText = this.sgRender
      .add(
         new BoolSetting.Builder()
            .name("damage")
            .description("Renders crystal damage text in the block overlay.")
            .defaultValue(true)
            .visible(() -> this.renderMode.get() != CrystalAura.RenderMode.None)
            .build()
      );
   private final Setting<SettingColor> damageColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("damage-color")
            .description("The color of the damage text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .visible(() -> this.renderMode.get() != CrystalAura.RenderMode.None && this.renderDamageText.get())
            .build()
      );
   private final Setting<Double> damageTextScale = this.sgRender
      .add(
         new DoubleSetting.Builder()
            .name("damage-scale")
            .description("How big the damage text should be.")
            .defaultValue(1.25)
            .min(1.0)
            .sliderMax(4.0)
            .visible(() -> this.renderMode.get() != CrystalAura.RenderMode.None && this.renderDamageText.get())
            .build()
      );
   private Item mainItem;
   private Item offItem;
   private int breakTimer;
   private int placeTimer;
   private int switchTimer;
   private int ticksPassed;
   private final List<LivingEntity> targets = new ArrayList<>();
   private final Vec3 vec3d = new Vec3(0.0, 0.0, 0.0);
   private final Vec3 playerEyePos = new Vec3(0.0, 0.0, 0.0);
   private final Vector3d vec3 = new Vector3d();
   private final MutableBlockPos blockPos = new MutableBlockPos();
   private final AABB box = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   private final Vec3 vec3dRayTraceEnd = new Vec3(0.0, 0.0, 0.0);
   private ClipContext clipContext;
   private final IntSet placedCrystals = new IntOpenHashSet();
   private boolean placing;
   private int placingTimer;
   public int kaTimer;
   private final MutableBlockPos placingCrystalBlockPos = new MutableBlockPos();
   private final IntSet removed = new IntOpenHashSet();
   private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();
   private final Int2IntMap waitingToExplode = new Int2IntOpenHashMap();
   private int attacks;
   private double serverYaw;
   private LivingEntity bestTarget;
   private double bestTargetDamage;
   private int bestTargetTimer;
   private boolean didRotateThisTick;
   private boolean isLastRotationPos;
   private final Vec3 lastRotationPos = new Vec3(0.0, 0.0, 0.0);
   private double lastYaw;
   private double lastPitch;
   private int lastRotationTimer;
   private int placeRenderTimer;
   private int breakRenderTimer;
   private final MutableBlockPos placeRenderPos = new MutableBlockPos();
   private final MutableBlockPos breakRenderPos = new MutableBlockPos();
   private AABB renderBoxOne;
   private AABB renderBoxTwo;
   private double renderDamage;

   public CrystalAura() {
      super(Categories.Combat, "crystal-aura", "Automatically places and attacks crystals.");
   }

   @Override
   public void onActivate() {
      this.breakTimer = 0;
      this.placeTimer = 0;
      this.ticksPassed = 0;
      this.clipContext = new ClipContext(new Vec3(0.0, 0.0, 0.0), new Vec3(0.0, 0.0, 0.0), Block.COLLIDER, Fluid.NONE, this.mc.player);
      this.placing = false;
      this.placingTimer = 0;
      this.kaTimer = 0;
      this.attacks = 0;
      this.serverYaw = this.mc.player.getYRot();
      this.bestTargetDamage = 0.0;
      this.bestTargetTimer = 0;
      this.lastRotationTimer = this.getLastRotationStopDelay();
      this.placeRenderTimer = 0;
      this.breakRenderTimer = 0;
   }

   @Override
   public void onDeactivate() {
      this.targets.clear();
      this.placedCrystals.clear();
      this.attemptedBreaks.clear();
      this.waitingToExplode.clear();
      this.removed.clear();
      this.bestTarget = null;
   }

   private int getLastRotationStopDelay() {
      return Math.max(10, this.placeDelay.get() / 2 + this.breakDelay.get() / 2 + 10);
   }

   @EventHandler(priority = 100)
   private void onPreTick(TickEvent.Pre event) {
      this.didRotateThisTick = false;
      this.lastRotationTimer++;
      if (this.placing) {
         if (this.placingTimer > 0) {
            this.placingTimer--;
         } else {
            this.placing = false;
         }
      }

      if (this.kaTimer > 0) {
         this.kaTimer--;
      }

      if (this.ticksPassed < 20) {
         this.ticksPassed++;
      } else {
         this.ticksPassed = 0;
         this.attacks = 0;
      }

      if (this.bestTargetTimer > 0) {
         this.bestTargetTimer--;
      }

      this.bestTargetDamage = 0.0;
      if (this.breakTimer > 0) {
         this.breakTimer--;
      }

      if (this.placeTimer > 0) {
         this.placeTimer--;
      }

      if (this.switchTimer > 0) {
         this.switchTimer--;
      }

      if (this.placeRenderTimer > 0) {
         this.placeRenderTimer--;
      }

      if (this.breakRenderTimer > 0) {
         this.breakRenderTimer--;
      }

      this.mainItem = this.mc.player.getMainHandItem().getItem();
      this.offItem = this.mc.player.getOffhandItem().getItem();
      IntIterator it = this.waitingToExplode.keySet().iterator();

      while (it.hasNext()) {
         int id = it.nextInt();
         int ticks = this.waitingToExplode.get(id);
         if (ticks > 3) {
            it.remove();
            this.removed.remove(id);
         } else {
            this.waitingToExplode.put(id, ticks + 1);
         }
      }

      ((IVec3)this.playerEyePos)
         .meteor$set(
            this.mc.player.position().x, this.mc.player.position().y + this.mc.player.getEyeHeight(this.mc.player.getPose()), this.mc.player.position().z
         );
      this.findTargets();
      if (!this.targets.isEmpty()) {
         if (!this.didRotateThisTick) {
            this.doBreak();
         }

         if (!this.didRotateThisTick) {
            this.doPlace();
         }
      }
   }

   @EventHandler(priority = -866)
   private void onPreTickLast(TickEvent.Pre event) {
      if (this.rotate.get() && this.lastRotationTimer < this.getLastRotationStopDelay() && !this.didRotateThisTick) {
         Rotations.rotate(
            this.isLastRotationPos ? Rotations.getYaw(this.lastRotationPos) : this.lastYaw,
            this.isLastRotationPos ? Rotations.getPitch(this.lastRotationPos) : this.lastPitch,
            -100,
            null
         );
      }
   }

   @EventHandler
   private void onEntityAdded(EntityAddedEvent event) {
      if (event.entity instanceof EndCrystal) {
         if (this.placing && event.entity.blockPosition().equals(this.placingCrystalBlockPos)) {
            this.placing = false;
            this.placingTimer = 0;
            this.placedCrystals.add(event.entity.getId());
         }

         if (this.fastBreak.get() && !this.didRotateThisTick && this.attacks < this.attackFrequency.get()) {
            float damage = this.getBreakDamage(event.entity, true);
            if (damage > this.minDamage.get()) {
               this.doBreak(event.entity);
            }
         }
      }
   }

   @EventHandler
   private void onEntityRemoved(EntityRemovedEvent event) {
      if (event.entity instanceof EndCrystal) {
         this.placedCrystals.remove(event.entity.getId());
         this.removed.remove(event.entity.getId());
         this.waitingToExplode.remove(event.entity.getId());
      }
   }

   private void setRotation(boolean isPos, Vec3 pos, double yaw, double pitch) {
      this.didRotateThisTick = true;
      this.isLastRotationPos = isPos;
      if (isPos) {
         ((IVec3)this.lastRotationPos).meteor$set(pos.x, pos.y, pos.z);
      } else {
         this.lastYaw = yaw;
         this.lastPitch = pitch;
      }

      this.lastRotationTimer = 0;
   }

   private void doBreak() {
      if (this.doBreak.get() && this.breakTimer <= 0 && this.switchTimer <= 0 && this.attacks < this.attackFrequency.get()) {
         if (!this.shouldPause(CrystalAura.PauseMode.Break)) {
            float bestDamage = 0.0F;
            Entity crystal = null;

            for (Entity entity : this.mc.level.entitiesForRendering()) {
               float damage = this.getBreakDamage(entity, true);
               if (damage > bestDamage) {
                  bestDamage = damage;
                  crystal = entity;
               }
            }

            if (crystal != null) {
               this.doBreak(crystal);
            }
         }
      }
   }

   private float getBreakDamage(Entity entity, boolean checkCrystalAge) {
      if (!(entity instanceof EndCrystal)) {
         return 0.0F;
      } else if (this.onlyBreakOwn.get() && !this.placedCrystals.contains(entity.getId())) {
         return 0.0F;
      } else if (this.removed.contains(entity.getId())) {
         return 0.0F;
      } else if (this.attemptedBreaks.get(entity.getId()) > this.breakAttempts.get()) {
         return 0.0F;
      } else if (checkCrystalAge && entity.tickCount < this.ticksExisted.get()) {
         return 0.0F;
      } else if (this.isOutOfRange(entity.position(), entity.blockPosition(), false)) {
         return 0.0F;
      } else {
         this.blockPos.set(entity.blockPosition()).move(0, -1, 0);
         float selfDamage = DamageUtils.crystalDamage(this.mc.player, entity.position(), this.predictMovement.get(), this.blockPos);
         if (!(selfDamage > this.maxDamage.get()) && (!this.antiSuicide.get() || !(selfDamage >= EntityUtils.getTotalHealth(this.mc.player)))) {
            float damage = this.getDamageToTargets(entity.position(), this.blockPos, true, false);
            boolean shouldFacePlace = this.shouldFacePlace();
            double minimumDamage = shouldFacePlace ? Math.min(this.minDamage.get(), 1.5) : this.minDamage.get();
            return damage < minimumDamage ? 0.0F : damage;
         } else {
            return 0.0F;
         }
      }
   }

   private void doBreak(Entity crystal) {
      if (this.antiWeakness.get()) {
         MobEffectInstance weakness = this.mc.player.getEffect(MobEffects.WEAKNESS);
         MobEffectInstance strength = this.mc.player.getEffect(MobEffects.STRENGTH);
         if (weakness != null
            && (strength == null || strength.getAmplifier() <= weakness.getAmplifier())
            && !this.isValidWeaknessItem(this.mc.player.getMainHandItem(), crystal)) {
            if (!InvUtils.swap(InvUtils.findInHotbar(stack -> this.isValidWeaknessItem(stack, crystal)).slot(), false)) {
               return;
            }

            this.switchTimer = 1;
            return;
         }
      }

      boolean attacked = true;
      if (this.rotate.get()) {
         double yaw = Rotations.getYaw(crystal);
         double pitch = Rotations.getPitch(crystal, Target.Feet);
         if (this.doYawSteps(yaw, pitch)) {
            this.setRotation(true, crystal.position(), 0.0, 0.0);
            Rotations.rotate(yaw, pitch, 50, () -> this.attackCrystal(crystal));
            this.breakTimer = this.breakDelay.get();
         } else {
            attacked = false;
         }
      } else {
         this.attackCrystal(crystal);
         this.breakTimer = this.breakDelay.get();
      }

      if (attacked) {
         this.removed.add(crystal.getId());
         this.attemptedBreaks.put(crystal.getId(), this.attemptedBreaks.get(crystal.getId()) + 1);
         this.waitingToExplode.put(crystal.getId(), 0);
         this.breakRenderPos.set(crystal.blockPosition().below());
         this.breakRenderTimer = this.breakRenderTime.get();
      }
   }

   private boolean isValidWeaknessItem(ItemStack itemStack, Entity crystal) {
      return DamageUtils.getAttackDamage(this.mc.player, crystal, itemStack) > 0.0F;
   }

   private void attackCrystal(Entity entity) {
      this.mc.player.connection.send(new ServerboundAttackPacket(entity.getId()));
      InteractionHand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
      if (hand == null) {
         hand = InteractionHand.MAIN_HAND;
      }

      if (this.swingMode.get().client()) {
         this.mc.player.swing(hand);
      }

      if (this.swingMode.get().packet()) {
         this.mc.getConnection().send(new ServerboundSwingPacket(hand));
      }

      this.attacks++;
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundSetCarriedItemPacket) {
         this.switchTimer = this.switchDelay.get();
      }
   }

   private void doPlace() {
      if (this.doPlace.get() && this.placeTimer <= 0) {
         if (!this.shouldPause(CrystalAura.PauseMode.Place)) {
            if (InvUtils.testInHotbar(Items.END_CRYSTAL)) {
               if (this.autoSwitch.get() != CrystalAura.AutoSwitchMode.None) {
                  if (this.noGapSwitch.get()
                     && this.autoSwitch.get() == CrystalAura.AutoSwitchMode.Normal
                     && this.offItem != Items.END_CRYSTAL
                     && (
                        this.mainItem == Items.ENCHANTED_GOLDEN_APPLE
                           || this.offItem == Items.ENCHANTED_GOLDEN_APPLE
                           || this.mainItem == Items.GOLDEN_APPLE
                           || this.offItem == Items.GOLDEN_APPLE
                     )) {
                     return;
                  }

                  if (this.noBowSwitch.get() && (this.mainItem == Items.BOW || this.offItem == Items.BOW)) {
                     return;
                  }
               } else if (this.mainItem != Items.END_CRYSTAL && this.offItem != Items.END_CRYSTAL) {
                  return;
               }

               for (Entity entity : this.mc.level.entitiesForRendering()) {
                  if (this.getBreakDamage(entity, false) > 0.0F) {
                     return;
                  }
               }

               AtomicDouble bestDamage = new AtomicDouble(0.0);
               AtomicReference<MutableBlockPos> bestBlockPos = new AtomicReference<>(new MutableBlockPos());
               AtomicBoolean isSupport = new AtomicBoolean(this.support.get() != CrystalAura.SupportMode.Disabled);
               BlockIterator.register(
                  (int)Math.ceil(this.placeRange.get()),
                  (int)Math.ceil(this.placeRange.get()),
                  (bp, blockState) -> {
                     boolean hasBlock = blockState.is(Blocks.BEDROCK) || blockState.is(Blocks.OBSIDIAN);
                     if (hasBlock || isSupport.get() && blockState.canBeReplaced()) {
                        this.blockPos.set(bp.getX(), bp.getY() + 1, bp.getZ());
                        if (this.mc.level.getBlockState(this.blockPos).isAir()) {
                           if (this.placement112.get()) {
                              this.blockPos.move(0, 1, 0);
                              if (!this.mc.level.getBlockState(this.blockPos).isAir()) {
                                 return;
                              }
                           }

                           ((IVec3)this.vec3d).meteor$set(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
                           this.blockPos.set(bp).move(0, 1, 0);
                           if (!this.isOutOfRange(this.vec3d, this.blockPos, true)) {
                              float selfDamage = DamageUtils.crystalDamage(this.mc.player, this.vec3d, this.predictMovement.get(), bp);
                              if (!(selfDamage > this.maxDamage.get())
                                 && (!this.antiSuicide.get() || !(selfDamage >= EntityUtils.getTotalHealth(this.mc.player)))) {
                                 float damage = this.getDamageToTargets(this.vec3d, bp, false, !hasBlock && this.support.get() == CrystalAura.SupportMode.Fast);
                                 boolean shouldFacePlace = this.shouldFacePlace();
                                 double minimumDamage = Math.min(this.minDamage.get(), shouldFacePlace ? 1.5 : this.minDamage.get());
                                 if (!(damage < minimumDamage)) {
                                    double x = bp.getX();
                                    double y = bp.getY() + 1;
                                    double z = bp.getZ();
                                    ((IAABB)this.box).meteor$set(x, y, z, x + 1.0, y + (this.placement112.get() ? 1 : 2), z + 1.0);
                                    if (!this.intersectsWithEntities(this.box)) {
                                       if (damage > bestDamage.get() || isSupport.get() && hasBlock) {
                                          bestDamage.set(damage);
                                          bestBlockPos.get().set(bp);
                                       }

                                       if (hasBlock) {
                                          isSupport.set(false);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               );
               BlockIterator.after(
                  () -> {
                     if (bestDamage.get() != 0.0) {
                        BlockHitResult result = this.getPlaceInfo(bestBlockPos.get());
                        ((IVec3)this.vec3d)
                           .meteor$set(
                              result.getBlockPos().getX() + 0.5 + result.getDirection().getUnitVec3i().getX() * 1.0 / 2.0,
                              result.getBlockPos().getY() + 0.5 + result.getDirection().getUnitVec3i().getY() * 1.0 / 2.0,
                              result.getBlockPos().getZ() + 0.5 + result.getDirection().getUnitVec3i().getZ() * 1.0 / 2.0
                           );
                        if (this.rotate.get()) {
                           double yaw = Rotations.getYaw(this.vec3d);
                           double pitch = Rotations.getPitch(this.vec3d);
                           if (this.yawStepMode.get() == CrystalAura.YawStepMode.Break || this.doYawSteps(yaw, pitch)) {
                              this.setRotation(true, this.vec3d, 0.0, 0.0);
                              Rotations.rotate(yaw, pitch, 50, () -> this.placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null));
                              this.placeTimer = this.placeTimer + this.placeDelay.get();
                           }
                        } else {
                           this.placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null);
                           this.placeTimer = this.placeTimer + this.placeDelay.get();
                        }
                     }
                  }
               );
            }
         }
      }
   }

   private BlockHitResult getPlaceInfo(BlockPos blockPos) {
      ((IVec3)this.vec3d)
         .meteor$set(this.mc.player.getX(), this.mc.player.getY() + this.mc.player.getEyeHeight(this.mc.player.getPose()), this.mc.player.getZ());

      for (Direction side : Direction.values()) {
         ((IVec3)this.vec3dRayTraceEnd)
            .meteor$set(
               blockPos.getX() + 0.5 + side.getUnitVec3i().getX() * 0.5,
               blockPos.getY() + 0.5 + side.getUnitVec3i().getY() * 0.5,
               blockPos.getZ() + 0.5 + side.getUnitVec3i().getZ() * 0.5
            );
         ((IClipContext)this.clipContext).meteor$set(this.vec3d, this.vec3dRayTraceEnd, Block.COLLIDER, Fluid.NONE, this.mc.player);
         BlockHitResult result = this.mc.level.clip(this.clipContext);
         if (result != null && result.getType() == Type.BLOCK && result.getBlockPos().equals(blockPos)) {
            return result;
         }
      }

      Direction side = blockPos.getY() > this.vec3d.y ? Direction.DOWN : Direction.UP;
      return new BlockHitResult(this.vec3d, side, blockPos, false);
   }

   private void placeCrystal(BlockHitResult result, double damage, BlockPos supportBlock) {
      Item targetItem = supportBlock == null ? Items.END_CRYSTAL : Items.OBSIDIAN;
      FindItemResult item = InvUtils.findInHotbar(targetItem);
      if (item.found()) {
         int prevSlot = this.mc.player.getInventory().getSelectedSlot();
         if (this.autoSwitch.get() != CrystalAura.AutoSwitchMode.None && !item.isOffhand()) {
            InvUtils.swap(item.slot(), false);
         }

         InteractionHand hand = item.getHand();
         if (hand != null) {
            if (supportBlock == null) {
               this.mc.gameMode.startPrediction(this.mc.level, sequence -> new ServerboundUseItemOnPacket(hand, result, sequence));
               if (this.swingMode.get().client()) {
                  this.mc.player.swing(hand);
               }

               if (this.swingMode.get().packet()) {
                  this.mc.getConnection().send(new ServerboundSwingPacket(hand));
               }

               this.placing = true;
               this.placingTimer = 4;
               this.kaTimer = 8;
               this.placingCrystalBlockPos.set(result.getBlockPos()).move(0, 1, 0);
               this.placeRenderPos.set(result.getBlockPos());
               this.renderDamage = damage;
               if (this.renderMode.get() == CrystalAura.RenderMode.Normal) {
                  this.placeRenderTimer = this.placeRenderTime.get();
               } else {
                  this.placeRenderTimer = this.renderTime.get();
                  if (this.renderMode.get() == CrystalAura.RenderMode.Fading) {
                     RenderUtils.renderTickingBlock(
                        this.placeRenderPos, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0, this.renderTime.get(), true, false
                     );
                  }
               }
            } else {
               BlockUtils.place(supportBlock, item, false, 0, this.swingMode.get().client(), true, false);
               this.placeTimer = this.placeTimer + this.supportDelay.get();
               if (this.supportDelay.get() == 0) {
                  this.placeCrystal(result, damage, null);
               }
            }

            if (this.autoSwitch.get() == CrystalAura.AutoSwitchMode.Silent) {
               InvUtils.swap(prevSlot, false);
            }
         }
      }
   }

   @EventHandler
   private void onPacketSent(PacketEvent.Sent event) {
      if (event.packet instanceof ServerboundMovePlayerPacket packet) {
         this.serverYaw = packet.getYRot((float)this.serverYaw);
      }
   }

   public boolean doYawSteps(double targetYaw, double targetPitch) {
      targetYaw = Mth.wrapDegrees(targetYaw) + 180.0;
      double serverYaw = Mth.wrapDegrees(this.serverYaw) + 180.0;
      if (distanceBetweenAngles(serverYaw, targetYaw) <= this.yawSteps.get()) {
         return true;
      }

      double delta = Math.abs(targetYaw - serverYaw);
      double yaw = this.serverYaw;
      if (serverYaw < targetYaw) {
         if (delta < 180.0) {
            yaw += this.yawSteps.get();
         } else {
            yaw -= this.yawSteps.get();
         }
      } else if (delta < 180.0) {
         yaw -= this.yawSteps.get();
      } else {
         yaw += this.yawSteps.get();
      }

      this.setRotation(false, null, yaw, targetPitch);
      Rotations.rotate(yaw, targetPitch, -100, null);
      return false;
   }

   private static double distanceBetweenAngles(double alpha, double beta) {
      double phi = Math.abs(beta - alpha) % 360.0;
      return phi > 180.0 ? 360.0 - phi : phi;
   }

   private boolean shouldFacePlace() {
      if (!this.facePlace.get()) {
         return false;
      }

      if (this.forceFacePlace.get().isPressed()) {
         return true;
      }

      for (LivingEntity target : this.targets) {
         if (EntityUtils.getTotalHealth(target) <= this.facePlaceHealth.get()) {
            return true;
         }

         for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            ItemStack itemStack = target.getItemBySlot(slot);
            if (itemStack != null && !itemStack.isEmpty()) {
               if ((double)(itemStack.getMaxDamage() - itemStack.getDamageValue()) / itemStack.getMaxDamage() * 100.0 <= this.facePlaceDurability.get()) {
                  return true;
               }
            } else if (this.facePlaceArmor.get()) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean shouldPause(CrystalAura.PauseMode process) {
      if ((this.mc.player.isUsingItem() || this.mc.options.keyUse.isDown()) && this.pauseOnUse.get().matches(process)) {
         return true;
      }

      if (this.pauseOnLag.get() && TickRate.INSTANCE.getTimeSinceLastTick() >= 1.0F) {
         return true;
      }

      for (Module module : this.pauseModules.get()) {
         if (module.isActive()) {
            return true;
         }
      }

      return this.pauseOnMine.get().matches(process) && this.mc.gameMode.isDestroying()
         ? true
         : EntityUtils.getTotalHealth(this.mc.player) <= this.pauseHealth.get();
   }

   private boolean isOutOfRange(Vec3 vec3d, BlockPos blockPos, boolean place) {
      ((IClipContext)this.clipContext).meteor$set(this.playerEyePos, vec3d, Block.COLLIDER, Fluid.NONE, this.mc.player);
      BlockHitResult result = this.mc.level.clip(this.clipContext);
      return result != null && result.getBlockPos().equals(blockPos)
         ? !PlayerUtils.isWithin(vec3d, (place ? this.placeRange : this.breakRange).get())
         : !PlayerUtils.isWithin(vec3d, (place ? this.placeWallsRange : this.breakWallsRange).get());
   }

   private LivingEntity getNearestTarget() {
      LivingEntity nearestTarget = null;
      double nearestDistance = Double.MAX_VALUE;

      for (LivingEntity target : this.targets) {
         double distance = PlayerUtils.squaredDistanceTo(target);
         if (distance < nearestDistance) {
            nearestTarget = target;
            nearestDistance = distance;
         }
      }

      return nearestTarget;
   }

   private float getDamageToTargets(Vec3 vec3d, BlockPos obsidianPos, boolean breaking, boolean fast) {
      float damage = 0.0F;
      if (fast) {
         LivingEntity target = this.getNearestTarget();
         if (!this.smartDelay.get() || !breaking || target.hurtTime <= 0) {
            damage = DamageUtils.crystalDamage(target, vec3d, this.predictMovement.get(), obsidianPos);
         }
      } else {
         for (LivingEntity target : this.targets) {
            if (!this.smartDelay.get() || !breaking || target.hurtTime <= 0) {
               float dmg = DamageUtils.crystalDamage(target, vec3d, this.predictMovement.get(), obsidianPos);
               if (dmg > this.bestTargetDamage) {
                  this.bestTarget = target;
                  this.bestTargetDamage = dmg;
                  this.bestTargetTimer = 10;
               }

               damage += dmg;
            }
         }
      }

      return damage;
   }

   @Override
   public String getInfoString() {
      return this.bestTarget != null && this.bestTargetTimer > 0 ? EntityUtils.getName(this.bestTarget) : null;
   }

   private void findTargets() {
      this.targets.clear();

      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof LivingEntity livingEntity
            && !(
               livingEntity instanceof Player player
                  && (
                     player.getAbilities().instabuild
                        || livingEntity == this.mc.player
                        || !player.isAlive()
                        || !Friends.get().shouldAttack(player)
                        || this.ignoreNakeds.get()
                           && player.getOffhandItem().isEmpty()
                           && player.getMainHandItem().isEmpty()
                           && player.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                           && player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                           && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                           && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                  )
            )
            && this.entities.get().contains(livingEntity.getType())
            && !(livingEntity.distanceToSqr(this.mc.player) > this.targetRange.get() * this.targetRange.get())) {
            this.targets.add(livingEntity);
         }
      }
   }

   private boolean intersectsWithEntities(AABB box) {
      return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !this.removed.contains(entity.getId()));
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.renderMode.get() != CrystalAura.RenderMode.None) {
         switch ((CrystalAura.RenderMode)this.renderMode.get()) {
            case Normal:
               if (this.renderPlace.get() && this.placeRenderTimer > 0) {
                  event.renderer.box(this.placeRenderPos, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
               }

               if (this.renderBreak.get() && this.breakRenderTimer > 0) {
                  event.renderer.box(this.breakRenderPos, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
               }
               break;
            case Smooth:
               if (this.placeRenderTimer <= 0) {
                  return;
               }

               if (this.renderBoxOne == null) {
                  this.renderBoxOne = new AABB(this.placeRenderPos);
               }

               if (this.renderBoxTwo == null) {
                  this.renderBoxTwo = new AABB(this.placeRenderPos);
               } else {
                  ((IAABB)this.renderBoxTwo).meteor$set(this.placeRenderPos);
               }

               double offsetX = (this.renderBoxTwo.minX - this.renderBoxOne.minX) / this.smoothness.get().intValue();
               double offsetY = (this.renderBoxTwo.minY - this.renderBoxOne.minY) / this.smoothness.get().intValue();
               double offsetZ = (this.renderBoxTwo.minZ - this.renderBoxOne.minZ) / this.smoothness.get().intValue();
               ((IAABB)this.renderBoxOne)
                  .meteor$set(
                     this.renderBoxOne.minX + offsetX,
                     this.renderBoxOne.minY + offsetY,
                     this.renderBoxOne.minZ + offsetZ,
                     this.renderBoxOne.maxX + offsetX,
                     this.renderBoxOne.maxY + offsetY,
                     this.renderBoxOne.maxZ + offsetZ
                  );
               event.renderer.box(this.renderBoxOne, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
            case Fading:
            default:
               break;
            case Gradient:
               if (this.placeRenderTimer <= 0) {
                  return;
               }

               Color bottom = new Color(0, 0, 0, 0);
               int x = this.placeRenderPos.getX();
               int y = this.placeRenderPos.getY() + 1;
               int z = this.placeRenderPos.getZ();
               if (this.shapeMode.get().sides()) {
                  event.renderer.quadHorizontal(x, y, z, x + 1, z + 1, this.sideColor.get());
                  event.renderer.gradientQuadVertical(x, y, z, x + 1, y - this.height.get(), z, bottom, this.sideColor.get());
                  event.renderer.gradientQuadVertical(x, y, z, x, y - this.height.get(), z + 1, bottom, this.sideColor.get());
                  event.renderer.gradientQuadVertical(x + 1, y, z, x + 1, y - this.height.get(), z + 1, bottom, this.sideColor.get());
                  event.renderer.gradientQuadVertical(x, y, z + 1, x + 1, y - this.height.get(), z + 1, bottom, this.sideColor.get());
               }

               if (this.shapeMode.get().lines()) {
                  event.renderer.line(x, y, z, x + 1, y, z, this.lineColor.get());
                  event.renderer.line(x, y, z, x, y, z + 1, this.lineColor.get());
                  event.renderer.line(x + 1, y, z, x + 1, y, z + 1, this.lineColor.get());
                  event.renderer.line(x, y, z + 1, x + 1, y, z + 1, this.lineColor.get());
                  event.renderer.line(x, y, z, x, y - this.height.get(), z, this.lineColor.get(), bottom);
                  event.renderer.line(x + 1, y, z, x + 1, y - this.height.get(), z, this.lineColor.get(), bottom);
                  event.renderer.line(x, y, z + 1, x, y - this.height.get(), z + 1, this.lineColor.get(), bottom);
                  event.renderer.line(x + 1, y, z + 1, x + 1, y - this.height.get(), z + 1, this.lineColor.get(), bottom);
               }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.renderMode.get() != CrystalAura.RenderMode.None && this.renderDamageText.get()) {
         if (this.placeRenderTimer > 0 || this.breakRenderTimer > 0) {
            if (this.renderMode.get() == CrystalAura.RenderMode.Smooth) {
               if (this.renderBoxOne == null) {
                  return;
               }

               this.vec3.set(this.renderBoxOne.minX + 0.5, this.renderBoxOne.minY + 0.5, this.renderBoxOne.minZ + 0.5);
            } else {
               this.vec3.set(this.placeRenderPos.getX() + 0.5, this.placeRenderPos.getY() + 0.5, this.placeRenderPos.getZ() + 0.5);
            }

            if (NametagUtils.to2D(this.vec3, this.damageTextScale.get())) {
               NametagUtils.begin(this.vec3);
               TextRenderer.get().begin(1.0, false, true);
               String text = String.format("%.1f", this.renderDamage);
               double w = TextRenderer.get().getWidth(text) / 2.0;
               TextRenderer.get().render(text, -w, 0.0, this.damageColor.get(), true);
               TextRenderer.get().end();
               NametagUtils.end();
            }
         }
      }
   }

   public enum AutoSwitchMode {
      Normal,
      Silent,
      None;
   }

   public enum PauseMode {
      Both,
      Place,
      Break,
      None;

      public boolean matches(CrystalAura.PauseMode process) {
         return this == process || this == Both;
      }
   }

   public enum RenderMode {
      Normal,
      Smooth,
      Fading,
      Gradient,
      None;
   }

   public enum SupportMode {
      Disabled,
      Accurate,
      Fast;
   }

   public enum SwingMode {
      Both,
      Packet,
      Client,
      None;

      public boolean packet() {
         return this == Packet || this == Both;
      }

      public boolean client() {
         return this == Client || this == Both;
      }
   }

   public enum YawStepMode {
      Break,
      All;
   }
}
