package meteordevelopment.meteorclient.systems.modules.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class ItemTags extends Module {
   private static final float GROUP_RADIUS = 24.0F;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Maximum distance to render dropped item tags.")
            .defaultValue(48.0)
            .min(4.0)
            .sliderRange(4.0, 128.0)
            .build()
      );
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("Base render scale for tags.").defaultValue(0.86).min(0.4).sliderRange(0.4, 1.8).build());
   private final Setting<Boolean> showDistance = this.sgGeneral
      .add(new BoolSetting.Builder().name("distance").description("Show distance after item name.").defaultValue(true).build());
   private final Setting<Boolean> showCount = this.sgGeneral
      .add(new BoolSetting.Builder().name("count").description("Show stack count.").defaultValue(true).build());
   private final Setting<Boolean> showIcon = this.sgGeneral
      .add(new BoolSetting.Builder().name("icon").description("Show the item icon.").defaultValue(true).build());
   private final Setting<ItemTags.BackgroundMode> backgroundMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("background-mode"))
                  .description("The render mode used for tag backgrounds."))
               .defaultValue(ItemTags.BackgroundMode.LiquidGlass))
            .build()
      );
   private final Setting<Double> backgroundRadius = this.sgRender
      .add(
         new DoubleSetting.Builder()
            .name("background-radius")
            .description("The corner radius of tag backgrounds.")
            .defaultValue(4.0)
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Boolean> backgroundGlow = this.sgRender
      .add(new BoolSetting.Builder().name("background-glow").description("Renders glow around tag backgrounds.").defaultValue(false).build());
   private final Setting<SettingColor> backgroundColor = this.sgRender
      .add(new ColorSetting.Builder().name("background-color").defaultValue(new SettingColor(12, 13, 16, 150)).build());
   private final Setting<SettingColor> accentColor = this.sgRender
      .add(new ColorSetting.Builder().name("accent-color").defaultValue(new SettingColor(232, 232, 232, 255)).build());
   private final Vector3d pos = new Vector3d();

   public ItemTags() {
      super(Categories.Render, "item-tags", "Displays tags and counts for dropped items.");
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (Utils.canUpdate() && this.mc.player != null && this.mc.level != null) {
         float partialTick = event.tickDelta;
         double maxDistanceSq = this.range.get() * this.range.get();
         float screenWidth = this.mc.getWindow().getWidth();
         float screenHeight = this.mc.getWindow().getHeight();
         List<ItemTags.TagCluster> clusters = new ArrayList<>();

         for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity itemEntity) {
               ItemStack stack = itemEntity.getItem();
               if (!stack.isEmpty()) {
                  Vec3 interpolated = new Vec3(
                     Mth.lerp(partialTick, itemEntity.xOld, itemEntity.getX()),
                     Mth.lerp(partialTick, itemEntity.yOld, itemEntity.getY()),
                     Mth.lerp(partialTick, itemEntity.zOld, itemEntity.getZ())
                  );
                  double dx = this.mc.player.getX() - interpolated.x;
                  double dy = this.mc.player.getY() - interpolated.y;
                  double dz = this.mc.player.getZ() - interpolated.z;
                  double distanceSq = dx * dx + dy * dy + dz * dz;
                  if (!(distanceSq > maxDistanceSq)) {
                     this.pos.set(interpolated.x, interpolated.y + itemEntity.getBbHeight() + 0.18, interpolated.z);
                     if (NametagUtils.to2D(this.pos, this.scale.get(), true, false)) {
                        float centerX = (float)this.pos.x;
                        float centerY = (float)this.pos.y;
                        this.addToCluster(clusters, stack, centerX, centerY, Math.sqrt(distanceSq), (float)NametagUtils.scale);
                     }
                  }
               }
            }
         }

         for (ItemTags.TagCluster cluster : clusters) {
            this.renderCluster(event, cluster, screenWidth, screenHeight);
         }
      }
   }

   private void addToCluster(List<ItemTags.TagCluster> clusters, ItemStack stack, float centerX, float centerY, double distance, float renderScale) {
      ItemTags.TagCluster target = null;
      float nearestDistanceSq = Float.MAX_VALUE;

      for (ItemTags.TagCluster cluster : clusters) {
         float dx = centerX - cluster.centerX;
         float dy = centerY - cluster.centerY;
         float distanceSq = dx * dx + dy * dy;
         float radius = 24.0F * Math.max(0.75F, Math.max(renderScale, cluster.renderScale));
         if (distanceSq <= radius * radius && distanceSq < nearestDistanceSq) {
            target = cluster;
            nearestDistanceSq = distanceSq;
         }
      }

      if (target == null) {
         target = new ItemTags.TagCluster(centerX, centerY, distance, renderScale);
         clusters.add(target);
      }

      target.add(stack, centerX, centerY, distance, renderScale);
   }

   private void renderCluster(Render2DEvent event, ItemTags.TagCluster cluster, float screenWidth, float screenHeight) {
      if (!cluster.rows.isEmpty()) {
         TextRenderer textRenderer = TextRenderer.get();
         float renderScale = cluster.renderScale;
         float textScale = renderScale * 0.72F;
         float iconSize = this.showIcon.get() ? 14.0F * renderScale : 0.0F;
         float gap = this.showIcon.get() ? 4.0F * renderScale : 0.0F;
         float paddingX = 6.0F * renderScale;
         float paddingY = 3.0F * renderScale;
         float rowGap = 2.0F * renderScale;
         float rowHeight = Math.max(iconSize, (float)textRenderer.getHeight(false) * textScale);
         boolean forceCount = cluster.rows.size() > 1;
         List<ItemTags.RenderedRow> renderedRows = new ArrayList<>(cluster.rows.size());
         float maxRowWidth = 0.0F;

         for (ItemTags.TagRow row : cluster.rows.values()) {
            String text = this.buildLabel(row, cluster.distance, forceCount);
            float textWidth = (float)textRenderer.getWidth(text, false) * textScale;
            float textHeight = (float)textRenderer.getHeight(false) * textScale;
            maxRowWidth = Math.max(maxRowWidth, iconSize + gap + textWidth);
            renderedRows.add(new ItemTags.RenderedRow(row.stack, text, textWidth, textHeight));
         }

         float width = paddingX * 2.0F + maxRowWidth;
         float height = paddingY * 2.0F + rowHeight * renderedRows.size() + rowGap * Math.max(0, renderedRows.size() - 1);
         float x = cluster.centerX - width * 0.5F;
         float y = cluster.centerY - height;
         if (!(x + width < 0.0F) && !(y + height < 0.0F) && !(x > screenWidth) && !(y > screenHeight)) {
            float radius = (float)Math.min(height / 2.0F, this.backgroundRadius.get() * renderScale);
            this.drawBackground(event, x, y, width, height, radius);
            Render2DUtil.drawRoundedRect(
               x + 3.0F * renderScale,
               y + height - 1.2F * renderScale,
               width - 6.0F * renderScale,
               0.7F * renderScale,
               0.35F * renderScale,
               this.withAlpha(this.accentColor.get(), 110)
            );
            float rowY = y + paddingY;
            textRenderer.begin(textScale, false, false);

            for (ItemTags.RenderedRow row : renderedRows) {
               if (this.showIcon.get()) {
                  ItemStack iconStack = row.stack.copy();
                  iconStack.setCount(1);
                  RenderUtils.drawItem(
                     event.graphics, iconStack, (int)(x + paddingX), (int)(rowY + (rowHeight - iconSize) * 0.5F), iconSize / 16.0F, false, null, true
                  );
               }

               float textX = x + paddingX + iconSize + gap;
               float textY = rowY + (rowHeight - row.textHeight) * 0.5F;
               textRenderer.render(row.text, textX, textY, new Color(240, 240, 240, 245), false);
               rowY += rowHeight + rowGap;
            }

            textRenderer.end();
         }
      }
   }

   private void drawBackground(Render2DEvent event, float x, float y, float width, float height, float radius) {
      if (this.backgroundMode.get() == ItemTags.BackgroundMode.LiquidGlass && LiquidGlassSetting.isEnabled()) {
         double guiScale = this.mc.getWindow().getGuiScale();
         LiquidGlassUtil.renderGui(
            event.graphics, x / guiScale, y / guiScale, width / guiScale, height / guiScale, radius / guiScale, this.backgroundColor.get()
         );
      } else if (this.backgroundMode.get() == ItemTags.BackgroundMode.Blur) {
         BlurUtil.render((int)x, (int)y, (int)width, (int)height, radius);
         Render2DUtil.drawRoundedRect(x, y, width, height, radius, this.backgroundColor.get());
      }

      if (this.backgroundGlow.get()) {
         Render2DUtil.drawRoundedGlow(x, y, width, height, radius);
      }
   }

   private String buildLabel(ItemTags.TagRow row, double distance, boolean forceCount) {
      StringBuilder label = new StringBuilder(row.name);
      if (this.showCount.get() && (forceCount || row.count > 1)) {
         label.append(" x").append(row.count);
      }

      if (this.showDistance.get()) {
         label.append(" ").append((int)distance).append("m");
      }

      return label.toString();
   }

   private Color withAlpha(SettingColor color, int alpha) {
      return new Color(color.r, color.g, color.b, Mth.clamp(alpha, 0, 255));
   }

   public enum BackgroundMode {
      Blur,
      LiquidGlass;
   }

   private record RenderedRow(ItemStack stack, String text, float textWidth, float textHeight) {
   }

   private static class TagCluster {
      private final Map<Item, ItemTags.TagRow> rows = new LinkedHashMap<>();
      private float centerX;
      private float centerY;
      private double distance;
      private float renderScale;
      private int items;

      private TagCluster(float centerX, float centerY, double distance, float renderScale) {
         this.centerX = centerX;
         this.centerY = centerY;
         this.distance = distance;
         this.renderScale = renderScale;
      }

      private void add(ItemStack stack, float x, float y, double distance, float renderScale) {
         this.items++;
         this.centerX = this.centerX + (x - this.centerX) / this.items;
         this.centerY = this.centerY + (y - this.centerY) / this.items;
         this.distance = Math.min(this.distance, distance);
         this.renderScale = Math.max(this.renderScale, renderScale);
         this.rows.computeIfAbsent(stack.getItem(), ignored -> new ItemTags.TagRow(stack)).add(stack.getCount());
      }
   }

   private static class TagRow {
      private final ItemStack stack;
      private final String name;
      private int count;

      private TagRow(ItemStack stack) {
         this.stack = stack.copy();
         this.stack.setCount(1);
         this.name = stack.getHoverName().getString();
      }

      private void add(int count) {
         this.count = this.count + Math.max(1, count);
      }
   }
}
