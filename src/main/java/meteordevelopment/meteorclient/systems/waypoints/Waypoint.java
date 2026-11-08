package meteordevelopment.meteorclient.systems.waypoints;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ProvidedStringSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class Waypoint implements ISerializable<Waypoint> {
   public final Settings settings = new Settings();
   private final SettingGroup sgVisual = this.settings.createGroup("Visual");
   private final SettingGroup sgPosition = this.settings.createGroup("Position");
   public Setting<String> name = this.sgVisual
      .add(new StringSetting.Builder().name("name").description("The name of the waypoint.").defaultValue("Home").build());
   public Setting<String> icon = this.sgVisual
      .add(
         new ProvidedStringSetting.Builder()
            .name("icon")
            .description("The icon of the waypoint.")
            .defaultValue("Square")
            .supplier(() -> Waypoints.BUILTIN_ICONS)
            .onChanged(var1 -> this.validateIcon())
            .build()
      );
   public Setting<SettingColor> color = this.sgVisual
      .add(new ColorSetting.Builder().name("color").description("The color of the waypoint.").defaultValue(MeteorClient.ADDON.color.toSetting()).build());
   public Setting<Boolean> visible = this.sgVisual
      .add(new BoolSetting.Builder().name("visible").description("Whether to show the waypoint.").defaultValue(true).build());
   public Setting<Integer> maxVisible = this.sgVisual
      .add(new IntSetting.Builder().name("max-visible-distance").description("How far away to render the waypoint.").defaultValue(5000).build());
   public Setting<Double> scale = this.sgVisual
      .add(new DoubleSetting.Builder().name("scale").description("The scale of the waypoint.").defaultValue(1.5).build());
   public Setting<BlockPos> pos = this.sgPosition
      .add(new BlockPosSetting.Builder().name("location").description("The location of the waypoint.").defaultValue(BlockPos.ZERO).build());
   public Setting<Dimension> dimension = this.sgPosition
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("dimension"))
                  .description("Which dimension the waypoint is in."))
               .defaultValue(Dimension.Overworld))
            .build()
      );
   public Setting<Boolean> opposite = this.sgPosition
      .add(
         new BoolSetting.Builder()
            .name("opposite-dimension")
            .description("Whether to show the waypoint in the opposite dimension.")
            .defaultValue(true)
            .visible(() -> this.dimension.get() != Dimension.End)
            .build()
      );
   public Setting<Waypoint.NearAction> actionWhenNear = this.sgPosition
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("action-when-near"))
                  .description("Action to be performed when the player is near."))
               .defaultValue(Waypoint.NearAction.Disabled))
            .build()
      );
   public Setting<Integer> actionWhenNearDistance = this.sgPosition
      .add(
         new IntSetting.Builder()
            .name("action-when-near-distance")
            .description("How close (in blocks) the player has to be for the near action to be performed.")
            .defaultValue(8)
            .sliderRange(0, 32)
            .visible(() -> this.actionWhenNear.get() != Waypoint.NearAction.Disabled)
            .build()
      );
   public final UUID uuid;
   public final long createdAt;
   final int waypointActionCooldown = 1000;

   private Waypoint() {
      this.uuid = UUID.randomUUID();
      this.createdAt = System.currentTimeMillis();
   }

   public Waypoint(Tag tag) {
      CompoundTag nbt = (CompoundTag)tag;
      this.uuid = nbt.read("uuid", UUIDUtil.CODEC).orElse(UUID.randomUUID());
      this.createdAt = System.currentTimeMillis();
      this.fromTag(nbt);
   }

   public void renderIcon(double x, double y, double a, double size) {
      AbstractTexture texture = Waypoints.get().icons.get(this.icon.get());
      if (texture != null) {
         int preA = this.color.get().a;
         SettingColor var10000 = this.color.get();
         var10000.a = (int)(var10000.a * a);
         Renderer2D.TEXTURE.begin();
         Renderer2D.TEXTURE.texQuad(x, y, size, size, this.color.get());
         Renderer2D.TEXTURE.render(texture.getTextureView(), texture.getSampler());
         this.color.get().a = preA;
      }
   }

   public BlockPos getPos() {
      Dimension dim = this.dimension.get();
      BlockPos pos = this.pos.get();
      Dimension currentDim = PlayerUtils.getDimension();
      if (dim != currentDim && !dim.equals(Dimension.End)) {
         return switch (dim) {
            case Overworld -> new BlockPos(pos.getX() / 8, pos.getY(), pos.getZ() / 8);
            case Nether -> new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8);
            default -> null;
         };
      } else {
         return this.pos.get();
      }
   }

   public boolean actionWhenNearCheck(int distance) {
      return System.currentTimeMillis() - this.createdAt < 1000L ? false : this.actionWhenNearDistance.get() >= distance;
   }

   private void validateIcon() {
      Map<String, AbstractTexture> icons = Waypoints.get().icons;
      AbstractTexture texture = icons.get(this.icon.get());
      if (texture == null && !icons.isEmpty()) {
         this.icon.set(icons.keySet().iterator().next());
      }
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.store("uuid", UUIDUtil.CODEC, this.uuid);
      tag.put("settings", this.settings.toTag());
      return tag;
   }

   public Waypoint fromTag(CompoundTag tag) {
      if (tag.contains("settings")) {
         this.settings.fromTag(tag.getCompoundOrEmpty("settings"));
      }

      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Waypoint waypoint = (Waypoint)o;
         return Objects.equals(this.uuid, waypoint.uuid);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.uuid);
   }

   @Override
   public String toString() {
      return this.name.get();
   }

   public static class Builder {
      private String name = "";
      private String icon = "";
      private BlockPos pos = BlockPos.ZERO;
      private Dimension dimension = Dimension.Overworld;

      public Waypoint.Builder name(String name) {
         this.name = name;
         return this;
      }

      public Waypoint.Builder icon(String icon) {
         this.icon = icon;
         return this;
      }

      public Waypoint.Builder pos(BlockPos pos) {
         this.pos = pos;
         return this;
      }

      public Waypoint.Builder dimension(Dimension dimension) {
         this.dimension = dimension;
         return this;
      }

      public Waypoint build() {
         Waypoint waypoint = new Waypoint();
         if (!this.name.equals(waypoint.name.getDefaultValue())) {
            waypoint.name.set(this.name);
         }

         if (!this.icon.equals(waypoint.icon.getDefaultValue())) {
            waypoint.icon.set(this.icon);
         }

         if (!this.pos.equals(waypoint.pos.getDefaultValue())) {
            waypoint.pos.set(this.pos);
         }

         if (!this.dimension.equals(waypoint.dimension.getDefaultValue())) {
            waypoint.dimension.set(this.dimension);
         }

         return waypoint;
      }
   }

   public enum NearAction {
      Disabled,
      Hide,
      Delete;
   }
}
