package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TargetHUD extends HudElement {
   public static final HudElementInfo<TargetHUD> INFO = new HudElementInfo<>(
      Hud.GROUP, "target-hud", "TargetHud", "Displays acid-style target cards.", TargetHUD::new
   );
   private static final double BASE_WIDTH = 380.0;
   private static final double BASE_HEIGHT = 100.0;
   private static final double BASE_SPACING = 20.0;
   private static final float HEALTH_ANIMATION_SPEED = 5.0F;
   private static final Color TEXT = new Color(255, 255, 255, 255);
   private static final Color TEXT_DIM = new Color(210, 210, 210, 230);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgColors = this.settings.createGroup("Colors");
   private final Setting<TargetHUD.RenderMode> renderMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("render-mode"))
                  .description("The render mode used for the target cards."))
               .defaultValue(TargetHUD.RenderMode.Rounded))
            .build()
      );
   private final Setting<Boolean> drawBackground = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("draw-background")
            .description("Draws a colored background behind shader modes.")
            .defaultValue(true)
            .visible(() -> this.renderMode.get() == TargetHUD.RenderMode.Shader || this.renderMode.get() == TargetHUD.RenderMode.BlurredShader)
            .build()
      );
   private final Setting<Boolean> enableGlow = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("enable-glow")
            .description("Renders glow around shader target cards.")
            .defaultValue(true)
            .visible(() -> this.renderMode.get() == TargetHUD.RenderMode.Shader || this.renderMode.get() == TargetHUD.RenderMode.BlurredShader)
            .build()
      );
   private final Setting<Integer> maxTargets = this.sgGeneral
      .add(
         new IntSetting.Builder().name("max-targets").description("Maximum amount of targets to render.").defaultValue(3).range(1, 6).sliderRange(1, 6).build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("Target search range.").defaultValue(100.0).min(1.0).sliderRange(1.0, 200.0).build());
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("Target HUD scale.").defaultValue(1.0).min(0.5).sliderRange(0.5, 2.0).build());
   private final Setting<Double> radius = this.sgGeneral
      .add(new DoubleSetting.Builder().name("radius").description("Corner radius.").defaultValue(8.0).min(0.0).sliderRange(0.0, 25.0).build());
   private final Setting<Double> animationSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("animation-speed")
            .description("Animation smoothing speed.")
            .defaultValue(12.0)
            .min(1.0)
            .sliderRange(1.0, 30.0)
            .build()
      );
   private final Setting<SettingColor> backgroundColor = this.sgColors
      .add(
         new ColorSetting.Builder().name("background-color").description("Target card background color.").defaultValue(new SettingColor(0, 0, 0, 135)).build()
      );
   private final Setting<SettingColor> accentColor = this.sgColors
      .add(new ColorSetting.Builder().name("accent-color").description("Health bar color.").defaultValue(new SettingColor(255, 35, 35, 235)).build());
   private final Setting<SettingColor> lowHealthColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("low-health-color")
            .description("Health bar color when the target is low.")
            .defaultValue(new SettingColor(255, 95, 95, 245))
            .build()
      );
   private final Setting<SettingColor> distanceColor = this.sgColors
      .add(new ColorSetting.Builder().name("distance-color").description("Distance text color.").defaultValue(new SettingColor(220, 220, 220, 235)).build());
   private final Map<UUID, Float> animatedHealth = new HashMap<>();
   private final Map<UUID, TargetHUD.AnimatedPosition> animatedPositions = new HashMap<>();

   private TargetHUD() {
      super(INFO);
      this.setSize(380.0, 100.0);
   }

   @Override
   public void render(HudRenderer renderer) {
      List<Player> targets = this.collectTargets();
      double s = this.scale.get();
      int targetCount = Math.max(1, targets.size());
      double width = 380.0 * s;
      double height = (100.0 * targetCount + 20.0 * Math.max(0, targetCount - 1)) * s;
      this.setSize(width, height);
      this.cleanupAnimations(targets);
      if (targets.isEmpty()) {
         if (this.isInEditor()) {
            this.renderEmptyEditor(renderer, width, 100.0 * s, s);
         }
      } else {
         for (int i = 0; i < targets.size(); i++) {
            Player target = targets.get(i);
            UUID uuid = target.getUUID();
            float targetX = this.x;
            float targetY = (float)(this.y + i * 120.0 * s);
            TargetHUD.AnimatedPosition position = this.animatedPositions.computeIfAbsent(uuid, ignored -> new TargetHUD.AnimatedPosition(targetX, targetY));
            float speed = this.animationSpeed.get().floatValue();
            position.x = AnimationUtility.ease(position.x, targetX, speed);
            position.y = AnimationUtility.ease(position.y, targetY, speed);
            float health = this.getAnimatedHealth(target);
            this.renderTarget(renderer, target, position.x, position.y, width, 100.0 * s, s, health);
         }
      }
   }

   private List<Player> collectTargets() {
      List<Player> targets = new ArrayList<>();
      if (MeteorClient.mc.player == null || MeteorClient.mc.level == null) {
         return targets;
      }

      if (this.isInEditor()) {
         targets.add(MeteorClient.mc.player);
         return targets;
      }

      if (this.currentAuraTarget() instanceof Player player && this.isValidTarget(player)) {
         targets.add(player);
      }

      MeteorClient.mc
         .level
         .players()
         .stream()
         .filter(this::isValidTarget)
         .filter(playerx -> !targets.contains(playerx))
         .sorted(Comparator.comparingDouble(playerx -> MeteorClient.mc.player.distanceToSqr(playerx)))
         .limit(Math.max(0, this.maxTargets.get() - targets.size()))
         .forEach(targets::add);
      return targets;
   }

   private LivingEntity currentAuraTarget() {
      KillAura aura = Modules.get().get(KillAura.class);
      return aura != null && aura.isActive() ? aura.target : null;
   }

   private boolean isValidTarget(Player player) {
      return player != null
         && player != MeteorClient.mc.player
         && !player.isRemoved()
         && player.isAlive()
         && MeteorClient.mc.player.distanceToSqr(player) <= this.range.get() * this.range.get();
   }

   private void cleanupAnimations(List<Player> targets) {
      List<UUID> active = targets.stream().map(Entity::getUUID).toList();
      Iterator<UUID> healthIterator = this.animatedHealth.keySet().iterator();

      while (healthIterator.hasNext()) {
         if (!active.contains(healthIterator.next())) {
            healthIterator.remove();
         }
      }

      Iterator<UUID> positionIterator = this.animatedPositions.keySet().iterator();

      while (positionIterator.hasNext()) {
         if (!active.contains(positionIterator.next())) {
            positionIterator.remove();
         }
      }
   }

   private void renderEmptyEditor(HudRenderer renderer, double width, double height, double scale) {
      this.drawCardBackground(renderer, this.x, this.y, width, height, scale);
      double textScale = textScale(1.0, scale);
      String text = "TargetHud";
      renderer.text(
         text,
         this.x + (width - renderer.textWidth(text, false, textScale)) * 0.5,
         this.y + (height - renderer.textHeight(false, textScale)) * 0.5,
         TEXT_DIM,
         false,
         textScale
      );
   }

   private void renderTarget(HudRenderer renderer, Player target, double cardX, double cardY, double width, double height, double scale, float health) {
      this.drawCardBackground(renderer, cardX, cardY, width, height, scale);
      double headSize = 70.0 * scale;
      double headRadius = Math.max(0.001, this.radius.get() * scale);
      double headX = cardX + 5.0 * scale;
      double headY = cardY + 5.0 * scale;
      double textX = cardX + 88.0 * scale;
      double nameY = cardY + 5.0 * scale;
      double infoY = cardY + 25.0 * scale;
      double armorX = cardX + 85.0 * scale;
      double armorY = cardY + 45.0 * scale;
      this.drawPlayerHead(renderer, target, headX, headY, headSize, headRadius);
      this.drawTexts(renderer, target, textX, nameY, infoY, width - (textX - cardX) - 5.0 * scale, scale, health);
      this.drawArmor(renderer, target, armorX, armorY, scale);
      this.drawHealthBar(renderer, target, cardX, cardY, width, height, scale, health);
   }

   private void drawCardBackground(HudRenderer renderer, double x, double y, double width, double height, double scale) {
      double r = Math.max(0.001, this.radius.get() * scale);
      boolean blurred = this.renderMode.get() == TargetHUD.RenderMode.Blurred || this.renderMode.get() == TargetHUD.RenderMode.BlurredShader;
      boolean shader = this.renderMode.get() == TargetHUD.RenderMode.Shader || this.renderMode.get() == TargetHUD.RenderMode.BlurredShader;
      if (blurred) {
         BlurUtil.render((int)Math.round(x), (int)Math.round(y), (int)Math.ceil(width), (int)Math.ceil(height), (float)r);
      }

      if (!shader || this.drawBackground.get()) {
         Render2DUtil.drawRoundedRect(renderer, x, y, width, height, r, this.backgroundColor.get());
      }

      if (shader && this.enableGlow.get()) {
         Render2DUtil.drawRoundedGlow(x, y, width, height, r);
      }
   }

   private void drawPlayerHead(HudRenderer renderer, Player player, double x, double y, double size, double radius) {
      if (player instanceof AbstractClientPlayer clientPlayer) {
         Identifier skin = clientPlayer.getSkin().body().texturePath();
         Render2DUtil.drawRoundedTexture(renderer, skin, x, y, size, size, radius, 8.0F, 8.0F, 8.0F, 8.0F, 64.0F, 64.0F);
         Render2DUtil.drawRoundedTexture(renderer, skin, x, y, size, size, radius, 40.0F, 8.0F, 8.0F, 8.0F, 64.0F, 64.0F);
      } else {
         Render2DUtil.drawRoundedRect(renderer, x, y, size, size, radius, new Color(35, 35, 35, 180));
      }
   }

   private void drawTexts(HudRenderer renderer, Player target, double x, double nameY, double infoY, double maxWidth, double scale, float health) {
      double nameScale = textScale(1.12, scale);
      double infoScale = textScale(0.72, scale);
      String name = this.trimToWidth(renderer, target.getName().getString(), maxWidth, nameScale);
      double distance = MeteorClient.mc.player == null ? 0.0 : MeteorClient.mc.player.distanceTo(target);
      String info = this.renderMode.get() == TargetHUD.RenderMode.Blurred
         ? String.format("%.1f m%s", distance, this.getInfoString(target).isEmpty() ? "" : " " + this.getInfoString(target))
         : String.format("Distance: %.1f m Health: %.1f", distance, health);
      String state = this.getInfoString(target);
      Color nameColor = Friends.get().isFriend(target) ? Config.get().friendColor.get() : TEXT;
      renderer.text(name, x, nameY, nameColor, false, nameScale);
      renderer.text(
         this.trimToWidth(renderer, info, maxWidth, infoScale), x, infoY, state.isEmpty() ? this.distanceColor.get() : this.accentColor.get(), false, infoScale
      );
   }

   private String getInfoString(Player target) {
      if (Friends.get().isFriend(target)) {
         return "[Friend]";
      }

      LivingEntity auraTarget = this.currentAuraTarget();
      return auraTarget == target ? "[Targeting]" : "";
   }

   private void drawArmor(HudRenderer renderer, Player target, double x, double y, double scale) {
      ItemStack[] items = new ItemStack[]{
         target.getMainHandItem(),
         target.getItemBySlot(EquipmentSlot.HEAD),
         target.getItemBySlot(EquipmentSlot.CHEST),
         target.getItemBySlot(EquipmentSlot.LEGS),
         target.getItemBySlot(EquipmentSlot.FEET),
         target.getOffhandItem()
      };
      float itemScale = (float)(2.0 * scale);
      int itemStep = (int)Math.round(40.0 * scale);
      int drawY = (int)Math.round(y);

      for (int i = 0; i < items.length; i++) {
         ItemStack item = items[i];
         if (!item.isEmpty()) {
            int drawX = (int)Math.round(x + i * itemStep);
            renderer.post(() -> renderer.item(item, drawX, drawY, itemScale, false));
         }
      }
   }

   private float getAnimatedHealth(Player target) {
      float targetHealth = Mth.clamp(target.getHealth(), 0.0F, Math.max(1.0F, target.getMaxHealth()));
      UUID uuid = target.getUUID();
      float previousHealth = this.animatedHealth.getOrDefault(uuid, targetHealth);
      float health = AnimationUtility.ease(previousHealth, targetHealth, 5.0F);
      this.animatedHealth.put(uuid, health);
      return health;
   }

   private void drawHealthBar(HudRenderer renderer, Player target, double x, double y, double width, double height, double scale, float health) {
      Color fillColor = target.getHealth() <= 8.0F ? this.lowHealthColor.get() : this.accentColor.get();
      if (this.renderMode.get() == TargetHUD.RenderMode.Blurred) {
         double healthWidth = h2b(width, health, target.getMaxHealth());
         Render2DUtil.drawRect(renderer, x, y + height, healthWidth, 3.0 * scale, fillColor);
      } else {
         double barX = x + 5.0 * scale;
         double barY = y + height - 10.0 * scale;
         double barWidth = width - 10.0 * scale;
         double healthWidth = h2b(barWidth, health, target.getMaxHealth());
         if (healthWidth > 0.5) {
            Render2DUtil.drawRoundedRect(renderer, barX, barY, healthWidth, 5.0 * scale, 3.0 * scale, fillColor);
         }
      }
   }

   private static double h2b(double width, float health, float maxHealth) {
      health = Math.max(0.0F, Math.min(health, maxHealth));
      return health / Math.max(1.0F, maxHealth) * width;
   }

   private String trimToWidth(HudRenderer renderer, String text, double width, double scale) {
      if (renderer.textWidth(text, false, scale) <= width) {
         return text;
      }

      String ellipsis = "...";
      int end = text.length();

      while (end > 0 && renderer.textWidth(text.substring(0, end) + ellipsis, false, scale) > width) {
         end--;
      }

      return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
   }

   private static double textScale(double baseScale, double layoutScale) {
      return baseScale * Math.sqrt(layoutScale);
   }

   private static class AnimatedPosition {
      private float x;
      private float y;

      private AnimatedPosition(float x, float y) {
         this.x = x;
         this.y = y;
      }
   }

   public enum RenderMode {
      Rounded,
      Blurred,
      Shader,
      BlurredShader;
   }
}
