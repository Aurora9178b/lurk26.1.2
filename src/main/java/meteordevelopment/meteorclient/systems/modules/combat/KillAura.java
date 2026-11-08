package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.BypassSetting;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.esp.CaptureMarkESP;
import meteordevelopment.meteorclient.utils.render.esp.CircleESP;
import meteordevelopment.meteorclient.utils.render.esp.FireflyESP;
import meteordevelopment.meteorclient.utils.render.esp.ThunderHackESP;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.SimplexNoise;

public class KillAura extends Module {
   private static final long MACE_SHIELD_SWAP_DELAY_MS = 50L;
   private static final double NOISE_SCALE = 0.1;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgNoise = this.settings.createGroup("NoiseSystem");
   private final SettingGroup sgTargets = this.settings.createGroup("Targets");
   private final SettingGroup sgEsp = this.settings.createGroup("ESP");
   private final Setting<KillAura.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Attack timing mode."))
               .defaultValue(KillAura.Mode.OnePointNinePlus))
            .build()
      );
   private final Setting<Boolean> autoMace = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-mace").description("Automatically uses mace critical attacks when possible.").defaultValue(false).build());
   private final Setting<Boolean> onlyCritical = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("only-critical")
            .description("Only attacks when an AutoMace critical can be performed.")
            .defaultValue(false)
            .visible(this.autoMace::get)
            .build()
      );
   private final Setting<KillAura.TargetSort> sort = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("sort")).description("How targets are sorted."))
               .defaultValue(KillAura.TargetSort.LowestDistance))
            .build()
      );
   private final Setting<Boolean> lockTarget = this.sgGeneral
      .add(new BoolSetting.Builder().name("lock-target").description("Keeps the current target until it leaves range.").defaultValue(true).build());
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Attack range.")
            .defaultValue(3.0)
            .range(1.0, 6.0)
            .sliderRange(1.0, 6.0)
            .decimalPlaces(2)
            .build()
      );
   private final Setting<Double> aimRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("aim-range")
            .description("Additional target search range.")
            .defaultValue(3.0)
            .range(0.0, 6.0)
            .sliderRange(0.0, 6.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Integer> fov = this.sgGeneral
      .add(new IntSetting.Builder().name("fov").description("Target FOV.").defaultValue(360).range(10, 360).sliderRange(10, 360).build());
   private final Setting<Double> rotationSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rotation-speed")
            .description("Rotation speed.")
            .defaultValue(10.0)
            .range(1.0, 10.0)
            .sliderRange(1.0, 10.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Double> rotationBackSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rotation-back-speed")
            .description("Rotation return speed.")
            .defaultValue(3.0)
            .range(1.0, 10.0)
            .sliderRange(1.0, 10.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<KillAura.RotationMode> rotationMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("rotation-mode"))
                  .description("How rotations are applied."))
               .defaultValue(KillAura.RotationMode.Static))
            .build()
      );
   private final Setting<Integer> interactTicks = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("interact-ticks")
            .description("Ticks to keep rotating in Interact mode.")
            .defaultValue(3)
            .range(1, 10)
            .sliderRange(1, 10)
            .visible(() -> this.rotationMode.get() == KillAura.RotationMode.Interact)
            .build()
      );
   private final Setting<Integer> minCPS = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("min-cps")
            .description("Minimum CPS in 1.8 mode.")
            .defaultValue(10)
            .range(1, 20)
            .sliderRange(1, 20)
            .visible(() -> this.mode.get() == KillAura.Mode.OnePointEight)
            .build()
      );
   private final Setting<Integer> maxCPS = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-cps")
            .description("Maximum CPS in 1.8 mode.")
            .defaultValue(12)
            .range(1, 20)
            .sliderRange(1, 20)
            .visible(() -> this.mode.get() == KillAura.Mode.OnePointEight)
            .build()
      );
   private final Setting<KillAura.Noise> noise = this.sgNoise
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("Noise")).description("Acid rotation noise mode."))
               .defaultValue(KillAura.Noise.Noise1))
            .build()
      );
   private final Setting<Double> adjustmentFactor = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("AdjustmentFactor")
            .description("Adjustment factor for Noise2 and Noise3.")
            .defaultValue(0.1)
            .range(0.0, 5.0)
            .sliderRange(0.0, 5.0)
            .decimalPlaces(3)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise2 || this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> minMotionXZ = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("minMotionXZ")
            .description("Minimum XZ motion for Noise3.")
            .defaultValue(0.003)
            .range(0.0, 5.0)
            .sliderRange(0.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> maxMotionXZ = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("maxMotionXZ")
            .description("Maximum XZ motion for Noise3.")
            .defaultValue(0.03)
            .range(0.0, 5.0)
            .sliderRange(0.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> minMotionY = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("minMotionY")
            .description("Minimum Y motion for Noise3.")
            .defaultValue(0.001)
            .range(0.0, 5.0)
            .sliderRange(0.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> maxMotionY = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("maxMotionY")
            .description("Maximum Y motion for Noise3.")
            .defaultValue(0.03)
            .range(0.0, 5.0)
            .sliderRange(0.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionXMin = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionXMin")
            .description("Initial motion X min for Noise3.")
            .defaultValue(-0.05)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionXMax = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionXMax")
            .description("Initial motion X max for Noise3.")
            .defaultValue(0.05)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionYMin = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionYMin")
            .description("Initial motion Y min for Noise3.")
            .defaultValue(0.05)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionYMax = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionYMax")
            .description("Initial motion Y max for Noise3.")
            .defaultValue(0.05)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionZMin = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionZMin")
            .description("Initial motion Z min for Noise3.")
            .defaultValue(0.05)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionZMax = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionZMax")
            .description("Initial motion Z max for Noise3.")
            .defaultValue(0.05)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationPointX = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationPointX")
            .description("X boundary factor for Noise3.")
            .defaultValue(2.0)
            .range(0.0, 5.0)
            .sliderRange(0.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationPointZ = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationPointZ")
            .description("Z boundary factor for Noise3.")
            .defaultValue(2.0)
            .range(0.0, 5.0)
            .sliderRange(0.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionFactor = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionFactor")
            .description("Negative boundary motion factor for Noise3.")
            .defaultValue(0.03)
            .range(0.0, 5.0)
            .sliderRange(0.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionFMin = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionFMin")
            .description("Jitter X min for Noise3.")
            .defaultValue(-0.03)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionFMax = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionFMax")
            .description("Jitter X max for Noise3.")
            .defaultValue(0.03)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionGMin = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionGMin")
            .description("Jitter Z min for Noise3.")
            .defaultValue(-0.15)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionGMax = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionGMax")
            .description("Jitter Z max for Noise3.")
            .defaultValue(0.15)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionHMin = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionHMin")
            .description("Center X min for Noise3.")
            .defaultValue(-0.1)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionHMax = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionHMax")
            .description("Center X max for Noise3.")
            .defaultValue(0.1)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionIMin = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionIMin")
            .description("Wall aim X min for Noise3.")
            .defaultValue(-0.15)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Double> rotationMotionIMax = this.sgNoise
      .add(
         new DoubleSetting.Builder()
            .name("rotationMotionIMax")
            .description("Wall aim X max for Noise3.")
            .defaultValue(0.15)
            .range(-5.0, 5.0)
            .sliderRange(-5.0, 5.0)
            .decimalPlaces(5)
            .visible(() -> this.noise.get() == KillAura.Noise.Noise3)
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgTargets
      .add(new EntityTypeListSetting.Builder().name("entities").description("Entities to attack.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<Boolean> invisible = this.sgTargets
      .add(new BoolSetting.Builder().name("invisible").description("Targets invisible entities.").defaultValue(true).build());
   private final Setting<Boolean> teams = this.sgTargets
      .add(
         new BoolSetting.Builder()
            .name("teams")
            .description("Does not attack players wearing the same dyed leather chestplate color.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> ignoreCreative = this.sgTargets
      .add(new BoolSetting.Builder().name("ignore-creative").description("Ignores players in creative mode.").defaultValue(true).build());
   private final Setting<Boolean> esp = this.sgEsp.add(new BoolSetting.Builder().name("esp").description("Renders target ESP.").defaultValue(true).build());
   private final Setting<KillAura.ESPMode> espMode = this.sgEsp
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("esp-mode"))
                     .description("Target ESP mode."))
                  .defaultValue(KillAura.ESPMode.Circle))
               .visible(this.esp::get))
            .build()
      );
   private final Setting<CaptureMarkESP.Style> captureStyle = this.sgEsp
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("capture-style"))
                     .description("CaptureMark texture style."))
                  .defaultValue(CaptureMarkESP.Style.One))
               .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.CaptureMark))
            .build()
      );
   private final Setting<SettingColor> espColor1 = this.sgEsp
      .add(
         new ColorSetting.Builder()
            .name("esp-main")
            .description("CaptureMark main color.")
            .defaultValue(new SettingColor(255, 183, 197, 255))
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.CaptureMark)
            .build()
      );
   private final Setting<SettingColor> espColor2 = this.sgEsp
      .add(
         new ColorSetting.Builder()
            .name("esp-second")
            .description("CaptureMark secondary color.")
            .defaultValue(new SettingColor(255, 133, 161, 255))
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.CaptureMark)
            .build()
      );
   private final Setting<Double> espSize = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("esp-size")
            .description("CaptureMark size.")
            .defaultValue(1.2)
            .range(0.5, 3.0)
            .sliderRange(0.5, 3.0)
            .decimalPlaces(1)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.CaptureMark)
            .build()
      );
   private final Setting<Double> espRotSpeed = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("rot-speed")
            .description("CaptureMark rotation speed.")
            .defaultValue(2.0)
            .range(0.5, 10.0)
            .sliderRange(0.5, 10.0)
            .decimalPlaces(1)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.CaptureMark)
            .build()
      );
   private final Setting<Double> waveSpeed = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("wave-speed")
            .description("CaptureMark color wave speed.")
            .defaultValue(3.0)
            .range(0.5, 10.0)
            .sliderRange(0.5, 10.0)
            .decimalPlaces(1)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.CaptureMark)
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgEsp
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("Circle side color.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Circle)
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgEsp
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("Circle line color.")
            .defaultValue(new SettingColor(255, 255, 255, 233))
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Circle)
            .build()
      );
   private final Setting<Double> circleRadius = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("circle-radius")
            .description("Circle radius.")
            .defaultValue(0.75)
            .range(0.1, 2.0)
            .sliderRange(0.1, 2.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Circle)
            .build()
      );
   private final Setting<Double> circleAlphaFactor = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("circle-alpha-factor")
            .description("Circle alpha factor.")
            .defaultValue(1.0)
            .range(0.0, 2.0)
            .sliderRange(0.0, 2.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Circle)
            .build()
      );
   private final Setting<SettingColor> fireflyColor = this.sgEsp
      .add(
         new ColorSetting.Builder()
            .name("firefly-color")
            .description("Firefly primary color.")
            .defaultValue(new SettingColor(149, 149, 149, 255))
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly)
            .build()
      );
   private final Setting<FireflyESP.ColorMode> fireflyColorMode = this.sgEsp
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("firefly-color-mode"))
                     .description("Firefly color mode."))
                  .defaultValue(FireflyESP.ColorMode.Blend))
               .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly))
            .build()
      );
   private final Setting<SettingColor> fireflyColor2 = this.sgEsp
      .add(
         new ColorSetting.Builder()
            .name("firefly-color-2")
            .description("Firefly secondary color.")
            .defaultValue(new SettingColor(255, 133, 161, 255))
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly && this.fireflyColorMode.get() == FireflyESP.ColorMode.Blend)
            .build()
      );
   private final Setting<Double> fireflyColorMix = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("firefly-color-mix")
            .description("Firefly blend mix.")
            .defaultValue(0.65)
            .range(0.0, 1.0)
            .sliderRange(0.0, 1.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly && this.fireflyColorMode.get() == FireflyESP.ColorMode.Blend)
            .build()
      );
   private final Setting<Double> fireflyColorSpeed = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("firefly-color-speed")
            .description("Firefly blend animation speed.")
            .defaultValue(1.2)
            .range(0.1, 6.0)
            .sliderRange(0.1, 6.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly && this.fireflyColorMode.get() == FireflyESP.ColorMode.Blend)
            .build()
      );
   private final Setting<Double> fireflyRainbowSpeed = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("firefly-rainbow-speed")
            .description("Firefly rainbow speed.")
            .defaultValue(1.0)
            .range(0.1, 6.0)
            .sliderRange(0.1, 6.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly && this.fireflyColorMode.get() == FireflyESP.ColorMode.Rainbow)
            .build()
      );
   private final Setting<Double> fireflyRainbowSaturation = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("firefly-rainbow-saturation")
            .description("Firefly rainbow saturation.")
            .defaultValue(0.85)
            .range(0.1, 1.0)
            .sliderRange(0.1, 1.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly && this.fireflyColorMode.get() == FireflyESP.ColorMode.Rainbow)
            .build()
      );
   private final Setting<Double> fireflyRainbowBrightness = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("firefly-rainbow-brightness")
            .description("Firefly rainbow brightness.")
            .defaultValue(1.0)
            .range(0.1, 1.0)
            .sliderRange(0.1, 1.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly && this.fireflyColorMode.get() == FireflyESP.ColorMode.Rainbow)
            .build()
      );
   private final Setting<Integer> fireflyLength = this.sgEsp
      .add(
         new IntSetting.Builder()
            .name("firefly-length")
            .description("Firefly trail length.")
            .defaultValue(14)
            .range(8, 128)
            .sliderRange(8, 128)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly)
            .build()
      );
   private final Setting<Integer> fireflyFactor = this.sgEsp
      .add(
         new IntSetting.Builder()
            .name("firefly-factor")
            .description("Firefly density.")
            .defaultValue(8)
            .range(1, 10)
            .sliderRange(1, 10)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly)
            .build()
      );
   private final Setting<Double> fireflyShaking = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("firefly-shaking")
            .description("Firefly shake intensity.")
            .defaultValue(1.8)
            .range(0.25, 10.0)
            .sliderRange(0.25, 10.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly)
            .build()
      );
   private final Setting<Double> fireflyAmplitude = this.sgEsp
      .add(
         new DoubleSetting.Builder()
            .name("firefly-amplitude")
            .description("Firefly wave amplitude.")
            .defaultValue(3.0)
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .decimalPlaces(2)
            .visible(() -> this.esp.get() && this.espMode.get() == KillAura.ESPMode.Firefly)
            .build()
      );
   public LivingEntity target;
   public boolean attacking;
   private final List<LivingEntity> targets = new ArrayList<>();
   private Vec3 rotationPoint = Vec3.ZERO;
   private Vec3 rotationMotion = Vec3.ZERO;
   private double noiseOffsetX = Math.random() * 1000.0;
   private double noiseOffsetY = Math.random() * 1000.0;
   private double noiseOffsetZ = Math.random() * 1000.0;
   private LivingEntity lastAimTarget;
   private Rotation attackRotation;
   private int interactRotateTicks;
   private double attacks;
   private boolean shieldBreakSwapped;
   private long shieldBreakSwapBackTime;
   private int shieldBreakReturnTicks;
   private boolean maceSwapped;
   private long maceSwapBackTime;
   private int maceReturnTicks;
   private KillAura.PendingMaceAttack pendingMaceAttack;
   private LivingEntity pendingMaceTarget;
   private long pendingMaceAttackTime;

   public KillAura() {
      super(Categories.Combat, "Aura", "Aura");
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      this.attacking = false;
      this.targets.clear();
      this.rotationPoint = Vec3.ZERO;
      this.rotationMotion = Vec3.ZERO;
      this.lastAimTarget = null;
      this.attackRotation = null;
      this.interactRotateTicks = 0;
      this.attacks = 0.0;
      this.shieldBreakReturnTicks = 0;
      this.maceReturnTicks = 0;
      this.pendingMaceAttack = null;
      this.pendingMaceTarget = null;
      this.returnShieldBreakSlot();
      this.returnMaceSlot();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.attacking = false;
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null && this.mc.getConnection() != null) {
         this.tickPendingMaceAttack();
         if (!this.tryReturnMaceSlot() && !this.maceSwapped && !this.isWaitingForMaceReturn() && this.pendingMaceAttack == null) {
            if (!this.tryReturnShieldBreakSlot() && !this.shieldBreakSwapped && !this.isWaitingForShieldBreakReturn()) {
               if (!this.mc.player.isUsingItem() && !this.mc.player.isBlocking()) {
                  this.targets.clear();
                  this.acquireTargets(this.targets);
                  if (this.targets.isEmpty()) {
                     this.target = null;
                     this.lastAimTarget = null;
                     this.rotationPoint = Vec3.ZERO;
                     this.rotationMotion = Vec3.ZERO;
                  } else {
                     this.updateTarget();
                     int min = Math.min(this.minCPS.get(), this.maxCPS.get());
                     int max = Math.max(this.minCPS.get(), this.maxCPS.get());
                     this.attacks = this.attacks + this.random(min, max) / 20.0;
                     if (this.target != null) {
                        this.attacking = true;
                        if (this.lastAimTarget != this.target) {
                           this.rotationPoint = Vec3.ZERO;
                           this.rotationMotion = Vec3.ZERO;
                           this.lastAimTarget = this.target;
                        }

                        this.handleRotation(RotationUtil.rotationFromVec(this.getNoiseLookPoint(this.target)));
                        if (this.autoMace.get()) {
                           boolean maceAttacked = this.clickMaceTargets();
                           if (maceAttacked) {
                              this.attacks = 0.0;
                              return;
                           }

                           if (this.onlyCritical.get()) {
                              return;
                           }
                        }

                        if (this.mode.get() == KillAura.Mode.OnePointEight) {
                           while (this.attacks >= 1.0) {
                              this.clickTargets();
                              this.attacks--;
                              if (this.shieldBreakSwapped) {
                                 break;
                              }
                           }
                        } else {
                           if (this.tryShieldBreakIgnoringCooldown()) {
                              return;
                           }

                           if (this.mc.player.getAttackStrengthScale(0.5F) >= 1.0F) {
                              this.clickTargets();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryShieldBreakIgnoringCooldown() {
      return this.target != null && this.canAttack(this.target) && this.doShieldBreakAttack(this.target);
   }

   private void clickTargets() {
      if (this.target != null && this.canAttack(this.target)) {
         this.doAttack(this.target);
      }
   }

   private boolean clickMaceTargets() {
      return !this.canMaceAttack() ? false : this.target != null && this.canAttack(this.target) && this.doMaceAttack(this.target);
   }

   private boolean canAttack(LivingEntity entity) {
      if (!this.isInRange(entity, this.range.get(), this.wallRange())) {
         return false;
      } else {
         return this.rotationMode.get() != KillAura.RotationMode.None && this.rotationMode.get() != KillAura.RotationMode.Grim
            ? this.isLookingAtTarget(RotationManager.INSTANCE.getRotation(), entity, this.range.get(), this.wallRange())
            : true;
      }
   }

   private void handleRotation(Rotation rotation) {
      this.attackRotation = rotation;
      boolean preparingAttack = this.isPreparingAttack();
      switch ((KillAura.RotationMode)this.rotationMode.get()) {
         case Interact:
            if (preparingAttack) {
               this.interactRotateTicks = Math.max(this.interactRotateTicks, this.interactTicks.get());
            }

            if (this.interactRotateTicks > 0) {
               RotationManager.INSTANCE.setRotations(rotation, this.rotationSpeed.get(), this.rotationBackSpeed.get(), Priority.Medium);
               this.interactRotateTicks--;
            }
            break;
         case Static:
            RotationManager.INSTANCE.setRotations(rotation, this.rotationSpeed.get(), this.rotationBackSpeed.get(), Priority.Medium);
            break;
         case Grim:
            if (preparingAttack) {
               RotationManager.INSTANCE.setRotations(rotation, this.rotationSpeed.get(), this.rotationBackSpeed.get(), Priority.Medium);
            }
         case None:
      }
   }

   private boolean isPreparingAttack() {
      if (this.target == null) {
         return false;
      } else if (this.autoMace.get() && this.canMaceAttack()) {
         return true;
      } else if (this.shouldShieldBreak(this.target)) {
         return true;
      } else {
         return this.mode.get() == KillAura.Mode.OnePointEight ? this.attacks >= 1.0 : this.mc.player.getAttackStrengthScale(0.5F) >= 1.0F;
      }
   }

   private void doAttack(LivingEntity attackedTarget) {
      if (!this.doShieldBreakAttack(attackedTarget)) {
         this.attack(attackedTarget);
      }
   }

   private boolean doMaceAttack(LivingEntity attackedTarget) {
      if (!this.hasMace()) {
         return false;
      }

      this.protectElytra();
      if (this.shouldMaceShieldBreak(attackedTarget)) {
         return this.startMaceShieldBreak(attackedTarget);
      }

      if (!this.swapToMace()) {
         return false;
      }

      this.attack(attackedTarget);
      this.scheduleMaceSwapBack(true);
      return true;
   }

   private boolean canMaceAttack() {
      return this.mc.player.fallDistance >= 3.0;
   }

   private boolean startMaceShieldBreak(LivingEntity attackedTarget) {
      if (!this.swapToAxe()) {
         return false;
      }

      this.attack(attackedTarget);
      this.maceSwapped = true;
      this.pendingMaceTarget = attackedTarget;
      this.pendingMaceAttack = KillAura.PendingMaceAttack.Mace;
      this.pendingMaceAttackTime = System.currentTimeMillis() + 50L;
      return true;
   }

   private void tickPendingMaceAttack() {
      if (this.pendingMaceAttack != null && System.currentTimeMillis() >= this.pendingMaceAttackTime) {
         LivingEntity attackedTarget = this.pendingMaceTarget;
         this.pendingMaceAttack = null;
         this.pendingMaceTarget = null;
         if (attackedTarget != null && attackedTarget.isAlive() && !attackedTarget.isDeadOrDying() && this.swapToMace()) {
            this.attack(attackedTarget);
            this.scheduleMaceSwapBack(true);
         } else {
            this.scheduleMaceSwapBack(this.maceSwapped);
         }
      }
   }

   private boolean doShieldBreakAttack(LivingEntity attackedTarget) {
      if (this.shouldShieldBreak(attackedTarget) && this.swapToAxe()) {
         this.scheduleShieldBreakSwapBack(true);
         this.attack(attackedTarget);
         return true;
      } else {
         return false;
      }
   }

   private void attack(LivingEntity attackedTarget) {
      this.sendGrimRotation(true);
      this.mc.gameMode.attack(this.mc.player, attackedTarget);
      this.mc.player.swing(InteractionHand.MAIN_HAND);
      this.sendGrimRotation(false);
   }

   private void sendGrimRotation(boolean targetRotation) {
      if (this.rotationMode.get() == KillAura.RotationMode.Grim && this.mc.player != null && this.mc.getConnection() != null) {
         Rotation rotation = targetRotation && this.attackRotation != null
            ? this.attackRotation
            : new Rotation(this.mc.player.getYRot(), this.mc.player.getXRot());
         this.mc.getConnection().send(new Rot(rotation.getYaw(), rotation.getPitch(), this.mc.player.onGround(), this.mc.player.horizontalCollision));
      }
   }

   private boolean shouldShieldBreak(LivingEntity entity) {
      return !(entity instanceof Player player)
         ? false
         : player.isBlocking() && (player.getMainHandItem().is(Items.SHIELD) || player.getOffhandItem().is(Items.SHIELD));
   }

   private boolean shouldMaceShieldBreak(LivingEntity entity) {
      return !(entity instanceof Player player)
         ? false
         : player.isBlocking() && (player.getMainHandItem().is(Items.SHIELD) || player.getOffhandItem().is(Items.SHIELD));
   }

   private boolean swapToAxe() {
      FindItemResult axe = InvUtils.findInHotbar(stack -> stack.getItem() instanceof AxeItem);
      if (!axe.found()) {
         return false;
      }

      SlotHelper.setCurrentItem(axe.slot(), true);
      return true;
   }

   private boolean swapToMace() {
      FindItemResult mace = InvUtils.findInHotbar(Items.MACE);
      if (!mace.found()) {
         return false;
      }

      SlotHelper.setCurrentItem(mace.slot(), true);
      return true;
   }

   private boolean hasMace() {
      return InvUtils.findInHotbar(Items.MACE).found();
   }

   private void scheduleShieldBreakSwapBack(boolean didSwap) {
      if (didSwap) {
         this.shieldBreakSwapped = true;
         this.shieldBreakSwapBackTime = System.currentTimeMillis() + this.shieldBreakSwapBackDelay();
      }
   }

   private long shieldBreakSwapBackDelay() {
      return BypassSetting.getSwitchDelay();
   }

   private boolean tryReturnShieldBreakSlot() {
      if (this.shieldBreakSwapped && System.currentTimeMillis() >= this.shieldBreakSwapBackTime) {
         this.returnShieldBreakSlot();
         return true;
      } else {
         return false;
      }
   }

   private boolean isWaitingForShieldBreakReturn() {
      if (this.shieldBreakReturnTicks <= 0) {
         return false;
      }

      this.shieldBreakReturnTicks--;
      return true;
   }

   private void returnShieldBreakSlot() {
      if (this.shieldBreakSwapped) {
         SlotHelper.INSTANCE.stopNextTick();
         this.shieldBreakSwapped = false;
         this.shieldBreakReturnTicks = 1;
      }
   }

   private void scheduleMaceSwapBack(boolean didSwap) {
      if (didSwap) {
         this.maceSwapped = true;
         this.maceSwapBackTime = System.currentTimeMillis() + BypassSetting.getSwitchDelay();
      }
   }

   private boolean tryReturnMaceSlot() {
      if (this.maceSwapped && System.currentTimeMillis() >= this.maceSwapBackTime) {
         this.returnMaceSlot();
         return true;
      } else {
         return false;
      }
   }

   private boolean isWaitingForMaceReturn() {
      if (this.maceReturnTicks <= 0) {
         return false;
      }

      this.maceReturnTicks--;
      return true;
   }

   private void returnMaceSlot() {
      if (this.maceSwapped) {
         SlotHelper.INSTANCE.stopNextTick();
         this.maceSwapped = false;
         this.maceReturnTicks = 1;
      }
   }

   private void protectElytra() {
      ItemStack chest = this.mc.player.getItemBySlot(EquipmentSlot.CHEST);
      if (this.isGlider(chest)) {
         int chestplateSlot = this.findBestChestplate();
         if (chestplateSlot != -1) {
            InvUtils.move().from(chestplateSlot).toArmor(EquipmentSlot.CHEST.getIndex());
         } else {
            int emptySlot = this.findEmptyNonEquipmentSlot();
            if (emptySlot != -1) {
               InvUtils.move().fromArmor(EquipmentSlot.CHEST.getIndex()).to(emptySlot);
            }
         }
      }
   }

   private int findBestChestplate() {
      int bestSlot = -1;
      int bestScore = Integer.MIN_VALUE;

      for (int i = 0; i < this.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (this.isChestplate(stack)) {
            int score = this.getArmorScore(stack);
            if (score > bestScore) {
               bestScore = score;
               bestSlot = i;
            }
         }
      }

      return bestSlot;
   }

   private int findEmptyNonEquipmentSlot() {
      for (int i = 0; i < this.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
         if (this.mc.player.getInventory().getItem(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private boolean isGlider(ItemStack stack) {
      return stack != null && (stack.has(DataComponents.GLIDER) || stack.is(Items.ELYTRA));
   }

   private boolean isChestplate(ItemStack stack) {
      return stack != null && stack.is(ItemTags.CHEST_ARMOR) && !this.isGlider(stack);
   }

   private int getArmorScore(ItemStack stack) {
      int score = 0;
      if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
         ItemAttributeModifiers component = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);

         for (Entry modifier : component.modifiers()) {
            if (modifier.attribute() == Attributes.ARMOR || modifier.attribute() == Attributes.ARMOR_TOUGHNESS) {
               score += (int)modifier.modifier().amount();
            }
         }
      }

      if (stack.isDamageableItem()) {
         score += Math.max(0, stack.getMaxDamage() - stack.getDamageValue()) / 100;
      }

      return score;
   }

   private void acquireTargets(List<LivingEntity> out) {
      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof LivingEntity living && this.isValidTarget(living)) {
            out.add(living);
         }
      }

      FakePlayerManager.forEach(fakePlayer -> {
         if (fakePlayer != null && !out.contains(fakePlayer) && this.isValidTarget(fakePlayer)) {
            out.add(fakePlayer);
         }
      });
      out.sort(this.targetComparator());
      if (out.size() > 64) {
         out.subList(64, out.size()).clear();
      }
   }

   private void updateTarget() {
      LivingEntity candidate = this.targets.getFirst();
      if (this.target != null && this.targets.contains(this.target)) {
         if (this.sort.get() == KillAura.TargetSort.FOV || !this.lockTarget.get()) {
            this.target = candidate;
         }
      } else {
         this.target = candidate;
      }
   }

   private boolean isValidTarget(LivingEntity entity) {
      if (entity != this.mc.player && entity != this.mc.getCameraEntity()) {
         if (entity.isDeadOrDying() || !entity.isAlive() || entity.getHealth() <= 0.0F) {
            return false;
         }

         if (!this.invisible.get() && entity.isInvisible()) {
            return false;
         }

         if (!this.entities.get().contains(entity.getType())) {
            return false;
         }

         if (!RotationUtil.isInFov(entity, this.fov.get().intValue())) {
            return false;
         }

         if (entity instanceof Player player) {
            if (player instanceof FakePlayerEntity fakePlayer) {
               return !fakePlayer.noHit && this.isInRange(entity, this.rotateDistance(), this.wallRange());
            }

            if (this.ignoreCreative.get() && player.isCreative()) {
               return false;
            }

            if (!Friends.get().shouldAttack(player)) {
               return false;
            }

            if (this.teams.get() && this.sameLeatherChestColor(player, this.mc.player)) {
               return false;
            }
         }

         return this.isInRange(entity, this.rotateDistance(), this.wallRange());
      } else {
         return false;
      }
   }

   private Comparator<LivingEntity> targetComparator() {
      return switch ((KillAura.TargetSort)this.sort.get()) {
         case LowestDistance -> Comparator.comparingDouble(entity -> this.mc.player.distanceToSqr(entity));
         case HighestDistance -> Comparator.<LivingEntity>comparingDouble(entity -> this.mc.player.distanceToSqr(entity)).reversed();
         case FOV -> Comparator.comparingDouble(this::getFovAngle);
         case LowestHealth -> Comparator.comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount());
         case HighestHealth -> Comparator.<LivingEntity>comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount()).reversed();
         case LowestDurability -> Comparator.comparingDouble(entity -> this.armorDurability(entity));
         case HighestDurability -> Comparator.<LivingEntity>comparingDouble(entity -> this.armorDurability(entity)).reversed();
      };
   }

   private double getFovAngle(LivingEntity entity) {
      double dx = entity.getX() - this.mc.player.getX();
      double dz = entity.getZ() - this.mc.player.getZ();
      float yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      return Math.abs(Mth.wrapDegrees(yaw - this.mc.player.getYRot()));
   }

   private double armorDurability(LivingEntity entity) {
      double value = 0.0;
      value += this.armorDurability(entity.getItemBySlot(EquipmentSlot.HEAD));
      value += this.armorDurability(entity.getItemBySlot(EquipmentSlot.CHEST));
      value += this.armorDurability(entity.getItemBySlot(EquipmentSlot.LEGS));
      return value + this.armorDurability(entity.getItemBySlot(EquipmentSlot.FEET));
   }

   private double armorDurability(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.isDamageableItem() && stack.getMaxDamage() > 0
         ? (double)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()
         : 0.0;
   }

   private double rotateDistance() {
      return this.range.get() + this.aimRange.get();
   }

   private double wallRange() {
      return this.range.get();
   }

   private boolean isInRange(LivingEntity entity, double distance, double wallDistance) {
      if (this.mc.player != null && this.mc.level != null && entity != null) {
         double distanceSq = distance * distance;
         Vec3 eyePoint = entity.position().add(0.0, entity.getEyeHeight(entity.getPose()), 0.0);
         if (this.squaredDistanceFromEyes(eyePoint) > distanceSq + 4.0) {
            return false;
         }

         AABB box = entity.getBoundingBox();
         double halfX = box.getXsize() / 2.0;
         double halfZ = box.getZsize() / 2.0;

         for (double x = -halfX; x <= halfX; x += 0.15) {
            for (double z = -halfZ; z <= halfZ; z += 0.15) {
               for (double y = 0.05; y <= box.getYsize(); y += 0.25) {
                  Vec3 point = new Vec3(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                  if (!(this.squaredDistanceFromEyes(point) > distanceSq)) {
                     Rotation rotation = RotationUtil.rotationFromVec(point);
                     if (this.isLookingAtTarget(rotation, entity, distance, wallDistance)) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean sameLeatherChestColor(Player player, Player localPlayer) {
      Integer color = this.leatherChestColor(player);
      Integer localColor = this.leatherChestColor(localPlayer);
      return color != null && color.equals(localColor);
   }

   private Integer leatherChestColor(Player player) {
      if (player == null) {
         return null;
      } else {
         ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
         if (!chest.isEmpty() && chest.is(Items.LEATHER_CHESTPLATE)) {
            DyedItemColor color = chest.get(DataComponents.DYED_COLOR);
            return color == null ? null : color.rgb();
         } else {
            return null;
         }
      }
   }

   private Vec3 getNoiseLookPoint(LivingEntity target) {
      return switch ((KillAura.Noise)this.noise.get()) {
         case Noise1 -> this.getAcidLookPoint(target);
         case Noise2 -> this.getAcidLookPoint2(target, this.adjustmentFactor.get());
         case Noise3 -> this.getAcidLookPoint(
            target,
            this.minMotionXZ.get(),
            this.maxMotionXZ.get(),
            this.minMotionY.get(),
            this.maxMotionY.get(),
            this.rotationMotionXMin.get(),
            this.rotationMotionXMax.get(),
            this.rotationMotionYMin.get(),
            this.rotationMotionYMax.get(),
            this.rotationMotionZMin.get(),
            this.rotationMotionZMax.get(),
            this.rotationPointX.get(),
            this.rotationPointZ.get(),
            this.rotationMotionFactor.get(),
            this.rotationMotionFMin.get(),
            this.rotationMotionFMax.get(),
            this.rotationMotionGMin.get(),
            this.rotationMotionGMax.get(),
            this.rotationMotionHMin.get(),
            this.rotationMotionHMax.get(),
            this.rotationMotionIMin.get(),
            this.rotationMotionIMax.get()
         );
      };
   }

   private Vec3 getAcidLookPoint2(LivingEntity target, double adjustmentFactor) {
      int methodIndex = (int)(Math.abs(this.perlinNoise(this.noiseOffsetX, this.noiseOffsetY, this.noiseOffsetZ)) * 8.0) % 8;

      return switch (methodIndex) {
         case 1 -> this.randomOffsetMethod(target, 0.2, 0.2, 0.2);
         case 2 -> this.smoothTrackingMethod(target, 0.5);
         case 3 -> this.predictMovementMethod(target, 0.5);
         case 4 -> this.edgeFocusMethod(target, 2.0, 2.0);
         case 5 -> this.centerFocusMethod(target, 2.0);
         case 6 -> this.heightOffsetMethod(target);
         case 7 -> this.dynamicAdjustmentMethod(target, adjustmentFactor);
         default -> this.getAcidLookPoint(target);
      };
   }

   private Vec3 randomOffsetMethod(LivingEntity target, double xNoiseFactor, double yNoiseFactor, double zNoiseFactor) {
      double offsetX = this.perlinNoise(this.noiseOffsetX, 0.0, 0.0) * xNoiseFactor - 0.1;
      double offsetY = this.perlinNoise(0.0, this.noiseOffsetY, 0.0) * yNoiseFactor - 0.1;
      double offsetZ = this.perlinNoise(0.0, 0.0, this.noiseOffsetZ) * zNoiseFactor - 0.1;
      this.noiseOffsetX += 0.1;
      this.noiseOffsetY += 0.1;
      this.noiseOffsetZ += 0.1;
      return target.position().add(offsetX, offsetY, offsetZ);
   }

   private Vec3 smoothTrackingMethod(LivingEntity target, double predict) {
      return target.position().add(target.getDeltaMovement().scale(predict));
   }

   private Vec3 predictMovementMethod(LivingEntity target, double predictionTime) {
      return target.position().add(target.getDeltaMovement().scale(predictionTime));
   }

   private Vec3 edgeFocusMethod(LivingEntity target, double xFactor, double zFactor) {
      double lengthX = target.getBoundingBox().getXsize();
      double lengthZ = target.getBoundingBox().getZsize();
      double edgeX = (this.perlinNoise(this.noiseOffsetX, 0.0, 0.0) > 0.0 ? 1 : -1) * lengthX / xFactor;
      double edgeZ = (this.perlinNoise(0.0, 0.0, this.noiseOffsetZ) > 0.0 ? 1 : -1) * lengthZ / zFactor;
      this.noiseOffsetX += 0.1;
      this.noiseOffsetZ += 0.1;
      return target.position().add(edgeX, 0.0, edgeZ);
   }

   private Vec3 centerFocusMethod(LivingEntity target, double targetFactor) {
      return target.position().add(0.0, target.getBoundingBox().getYsize() / targetFactor, 0.0);
   }

   private Vec3 heightOffsetMethod(LivingEntity target) {
      double height = target.getBoundingBox().getYsize();
      double offsetY = this.perlinNoise(0.0, this.noiseOffsetY, 0.0) * height;
      this.noiseOffsetY += 0.1;
      return target.position().add(0.0, offsetY, 0.0);
   }

   private Vec3 dynamicAdjustmentMethod(LivingEntity target, double adjustmentFactor) {
      return target.position().add(target.getDeltaMovement().scale(adjustmentFactor));
   }

   private double perlinNoise(double x, double y, double z) {
      return SimplexNoise.noise((float)x, (float)y, (float)z);
   }

   private Vec3 getAcidLookPoint(LivingEntity target) {
      double lengthX = target.getBoundingBox().getXsize();
      double lengthY = target.getBoundingBox().getYsize();
      double lengthZ = target.getBoundingBox().getZsize();
      if (this.rotationMotion.equals(Vec3.ZERO)) {
         this.rotationMotion = new Vec3(this.random(-0.05, 0.05), this.random(-0.05, 0.05), this.random(-0.05, 0.05));
      }

      this.rotationPoint = this.rotationPoint.add(this.rotationMotion);
      if (this.rotationPoint.x >= (lengthX - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(-this.random(0.003, 0.03), this.rotationMotion.y, this.rotationMotion.z);
      }

      if (this.rotationPoint.y >= lengthY) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, -this.random(0.001, 0.03), this.rotationMotion.z);
      }

      if (this.rotationPoint.z >= (lengthZ - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, this.rotationMotion.y, -this.random(0.003, 0.03));
      }

      if (this.rotationPoint.x <= -(lengthX - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(this.random(0.003, 0.03), this.rotationMotion.y, this.rotationMotion.z);
      }

      if (this.rotationPoint.y <= 0.05) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, this.random(0.001, 0.03), this.rotationMotion.z);
      }

      if (this.rotationPoint.z <= -(lengthZ - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, this.rotationMotion.y, this.random(0.003, 0.03));
      }

      this.rotationPoint = this.rotationPoint.add(this.random(-0.03, 0.03), 0.0, this.random(-0.03, 0.03));
      if (!this.mc.player.hasLineOfSight(target) && this.wallRange() > 0.0) {
         return target.position().add(this.random(-0.15, 0.15), lengthY, this.random(-0.15, 0.15));
      }

      Rotation currentRotation = RotationManager.INSTANCE.getRotation();
      if (!this.isLookingAtTarget(currentRotation, target, this.range.get(), this.wallRange())) {
         Vec3 center = target.position().add(0.0, target.getEyeHeight(target.getPose()) / 2.0F, 0.0);
         Rotation centerRotation = RotationUtil.rotationFromVec(center);
         if (this.squaredDistanceFromEyes(center) <= this.range.get() * this.range.get()
            && this.isLookingAtTarget(centerRotation, target, this.range.get(), 0.0)) {
            this.rotationPoint = new Vec3(this.random(-0.1, 0.1), target.getEyeHeight(target.getPose()) / this.random(1.8, 2.5), this.random(-0.1, 0.1));
         } else {
            this.scanAcidHitboxPoint(target, lengthX);
         }
      }

      return target.position().add(this.rotationPoint);
   }

   private Vec3 getAcidLookPoint(
      LivingEntity target,
      double minMotionXZ,
      double maxMotionXZ,
      double minMotionY,
      double maxMotionY,
      double rotationMotionXMin,
      double rotationMotionXMax,
      double rotationMotionYMin,
      double rotationMotionYMax,
      double rotationMotionZMin,
      double rotationMotionZMax,
      double rotationPointX,
      double rotationPointZ,
      double rotationMotionFactor,
      double rotationMotionFMin,
      double rotationMotionFMax,
      double rotationMotionGMin,
      double rotationMotionGMax,
      double rotationMotionHMin,
      double rotationMotionHMax,
      double rotationMotionIMin,
      double rotationMotionIMax
   ) {
      double lengthX = target.getBoundingBox().getXsize();
      double lengthY = target.getBoundingBox().getYsize();
      double lengthZ = target.getBoundingBox().getZsize();
      if (this.rotationMotion.equals(Vec3.ZERO)) {
         this.rotationMotion = new Vec3(
            this.random(rotationMotionXMin, rotationMotionXMax),
            this.random(rotationMotionYMin, rotationMotionYMax),
            this.random(rotationMotionZMin, rotationMotionZMax)
         );
      }

      this.rotationPoint = this.rotationPoint.add(this.rotationMotion);
      if (this.rotationPoint.x >= (lengthX - rotationPointX) / 2.0) {
         this.rotationMotion = new Vec3(-this.random(minMotionXZ, maxMotionXZ), this.rotationMotion.y, this.rotationMotion.z);
      }

      if (this.rotationPoint.y >= lengthY) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, -this.random(minMotionY, maxMotionY), this.rotationMotion.z);
      }

      if (this.rotationPoint.z >= (lengthZ - rotationPointZ) / 2.0) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, this.rotationMotion.y, -this.random(minMotionXZ, maxMotionXZ));
      }

      if (this.rotationPoint.x <= -(lengthX - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(this.random(minMotionXZ, rotationMotionFactor), this.rotationMotion.y, this.rotationMotion.z);
      }

      if (this.rotationPoint.y <= 0.05) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, this.random(minMotionY, maxMotionY), this.rotationMotion.z);
      }

      if (this.rotationPoint.z <= -(lengthZ - 0.05) / 2.0) {
         this.rotationMotion = new Vec3(this.rotationMotion.x, this.rotationMotion.y, this.random(minMotionXZ, maxMotionXZ));
      }

      this.rotationPoint = this.rotationPoint
         .add(this.random(rotationMotionFMin, rotationMotionFMax), 0.0, this.random(rotationMotionGMin, rotationMotionGMax));
      if (!this.mc.player.hasLineOfSight(target) && this.wallRange() > 0.0) {
         return target.position().add(this.random(rotationMotionIMin, rotationMotionIMax), lengthY, this.random(-0.15, 0.15));
      }

      Rotation currentRotation = RotationManager.INSTANCE.getRotation();
      if (!this.isLookingAtTarget(currentRotation, target, this.range.get(), this.wallRange())) {
         Vec3 center = target.position().add(0.0, target.getEyeHeight(target.getPose()) / 2.0F, 0.0);
         Rotation centerRotation = RotationUtil.rotationFromVec(center);
         if (this.squaredDistanceFromEyes(center) <= this.range.get() * this.range.get()
            && this.isLookingAtTarget(centerRotation, target, this.range.get(), 0.0)) {
            this.rotationPoint = new Vec3(
               this.random(rotationMotionHMin, rotationMotionHMax), target.getEyeHeight(target.getPose()) / this.random(1.8, 2.5), this.random(-0.1, 0.1)
            );
         } else {
            this.scanAcidHitboxPoint(target, lengthX);
         }
      }

      return target.position().add(this.rotationPoint);
   }

   private void scanAcidHitboxPoint(LivingEntity target, double lengthX) {
      double halfBox = lengthX / 2.0;

      for (double x = -halfBox; x <= halfBox; x += 0.05) {
         for (double z = -halfBox; z <= halfBox; z += 0.05) {
            for (double y = 0.05; y <= target.getBoundingBox().getYsize(); y += 0.15) {
               Vec3 point = new Vec3(target.getX() + x, target.getY() + y, target.getZ() + z);
               if (!(this.squaredDistanceFromEyes(point) > this.range.get() * this.range.get())) {
                  Rotation rotation = RotationUtil.rotationFromVec(point);
                  if (this.isLookingAtTarget(rotation, target, this.range.get(), 0.0)) {
                     this.rotationPoint = new Vec3(x, y, z);
                     return;
                  }
               }
            }
         }
      }
   }

   private boolean isLookingAtTarget(Rotation rotation, LivingEntity target, double distance, double wallDistance) {
      if (this.mc.player != null && this.mc.level != null && rotation != null && target != null) {
         Vec3 start = this.mc.player.getEyePosition(1.0F);
         Vec3 direction = Vec3.directionFromRotation(rotation.getPitch(), rotation.getYaw());
         Vec3 end = start.add(direction.scale(distance));
         HitResult blockResult = this.mc.level.clip(new ClipContext(start, end, Block.OUTLINE, Fluid.NONE, this.mc.player));
         double blockDistanceSq = distance * distance;
         if (blockResult != null && blockResult.getType() != Type.MISS) {
            blockDistanceSq = start.distanceToSqr(blockResult.getLocation());
         }

         AABB searchBox = this.mc.player.getBoundingBox().expandTowards(direction.scale(distance)).inflate(1.0);
         double maxDistanceSq = Math.max(blockDistanceSq, wallDistance * wallDistance);
         EntityHitResult best = null;
         double bestDistanceSq = maxDistanceSq;

         for (Entity entity : this.mc.level.getEntities(this.mc.player, searchBox, entityx -> !entityx.isSpectator() && entityx.isPickable())) {
            if (entity == target) {
               AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
               Optional<Vec3> hit = entityBox.clip(start, end);
               if (!hit.isEmpty()) {
                  double hitDistanceSq = start.distanceToSqr(hit.get());
                  if (hitDistanceSq <= bestDistanceSq) {
                     best = new EntityHitResult(entity, hit.get());
                     bestDistanceSq = hitDistanceSq;
                  }
               }
            }
         }

         if (best == null) {
            return false;
         }

         double entityDistanceSq = start.distanceToSqr(best.getLocation());
         boolean allowedWallDistance = entityDistanceSq <= wallDistance * wallDistance;
         boolean wallMissing = blockResult == null || blockResult.getType() == Type.MISS;
         boolean wallBehindEntity = entityDistanceSq < blockDistanceSq;
         return (wallMissing || allowedWallDistance || wallBehindEntity) && entityDistanceSq <= distance * distance;
      } else {
         return false;
      }
   }

   private double squaredDistanceFromEyes(Vec3 point) {
      return this.mc.player == null ? Double.MAX_VALUE : this.mc.player.getEyePosition(1.0F).distanceToSqr(point);
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.esp.get() && this.target != null) {
         switch ((KillAura.ESPMode)this.espMode.get()) {
            case CaptureMark:
               CaptureMarkESP.render(
                  event.matrices,
                  this.target,
                  this.espSize.get(),
                  this.espRotSpeed.get(),
                  this.waveSpeed.get(),
                  this.espColor1.get(),
                  this.espColor2.get(),
                  this.captureStyle.get()
               );
               break;
            case Circle:
               CircleESP.render(
                  event.matrices,
                  this.target,
                  this.circleRadius.get().floatValue(),
                  this.sideColor.get(),
                  this.lineColor.get(),
                  this.circleAlphaFactor.get().floatValue()
               );
               break;
            case Firefly:
               FireflyESP.render(
                  event.matrices,
                  this.target,
                  this.fireflyLength.get(),
                  this.fireflyFactor.get(),
                  this.fireflyShaking.get(),
                  this.fireflyAmplitude.get(),
                  this.fireflyColor.get(),
                  this.fireflyColorMode.get(),
                  this.fireflyColor2.get(),
                  this.fireflyColorMix.get(),
                  this.fireflyColorSpeed.get(),
                  this.fireflyRainbowSpeed.get(),
                  this.fireflyRainbowSaturation.get(),
                  this.fireflyRainbowBrightness.get()
               );
               break;
            case ThunderHack:
               ThunderHackESP.render(event.matrices, this.target);
         }
      }
   }

   private double random(double min, double max) {
      return min + Math.random() * (max - min);
   }

   @Override
   public String getInfoString() {
      return this.rotationMode.get().toString();
   }

   private enum ESPMode {
      CaptureMark,
      Circle,
      Firefly,
      ThunderHack;
   }

   private enum Mode {
      OnePointEight("1.8"),
      OnePointNinePlus("1.9+");

      private final String title;

      Mode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }

   private enum Noise {
      Noise1,
      Noise2,
      Noise3;
   }

   private enum PendingMaceAttack {
      Mace;
   }

   private enum RotationMode {
      Interact,
      Static,
      Grim,
      None;
   }

   private enum TargetSort {
      LowestDistance,
      HighestDistance,
      FOV,
      LowestHealth,
      HighestHealth,
      LowestDurability,
      HighestDurability;
   }
}
