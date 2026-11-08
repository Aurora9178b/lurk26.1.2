package meteordevelopment.meteorclient.systems.hud.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.joml.Matrix4fStack;

public class CombatHud extends HudElement {
   private static final Color GREEN = new Color(15, 255, 15);
   private static final Color RED = new Color(255, 15, 15);
   private static final Color BLACK = new Color(0, 0, 0, 255);
   public static final HudElementInfo<CombatHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "combat", "Displays information about your combat target.", CombatHud::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgEnchantments = this.settings.createGroup("Enchantments");
   private final SettingGroup sgHealth = this.settings.createGroup("Health");
   private final SettingGroup sgDistance = this.settings.createGroup("Distance");
   private final SettingGroup sgPing = this.settings.createGroup("Ping");
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("The range to target players.").defaultValue(100.0).min(1.0).sliderMax(200.0).build());
   private final Setting<SettingColor> healthColor1 = this.sgHealth
      .add(
         new ColorSetting.Builder()
            .name("health-stage-1")
            .description("The color on the left of the health gradient.")
            .defaultValue(new SettingColor(255, 15, 15))
            .build()
      );
   private final Setting<SettingColor> healthColor2 = this.sgHealth
      .add(
         new ColorSetting.Builder()
            .name("health-stage-2")
            .description("The color in the middle of the health gradient.")
            .defaultValue(new SettingColor(255, 150, 15))
            .build()
      );
   private final Setting<SettingColor> healthColor3 = this.sgHealth
      .add(
         new ColorSetting.Builder()
            .name("health-stage-3")
            .description("The color on the right of the health gradient.")
            .defaultValue(new SettingColor(15, 255, 15))
            .build()
      );
   private final Setting<Set<ResourceKey<Enchantment>>> displayedEnchantments = this.sgEnchantments
      .add(
         new EnchantmentListSetting.Builder()
            .name("displayed-enchantments")
            .description("The enchantments that are shown on nametags.")
            .vanillaDefaults()
            .build()
      );
   private final Setting<SettingColor> enchantmentTextColor = this.sgEnchantments
      .add(new ColorSetting.Builder().name("enchantment-color").description("Color of enchantment text.").defaultValue(new SettingColor(255, 255, 255)).build());
   private final Setting<Boolean> displayPing = this.sgPing
      .add(new BoolSetting.Builder().name("ping").description("Shows the player's ping.").defaultValue(true).build());
   private final Setting<SettingColor> pingColor1 = this.sgPing
      .add(
         new ColorSetting.Builder()
            .name("ping-stage-1")
            .description("Color of ping text when under 75.")
            .defaultValue(new SettingColor(15, 255, 15))
            .visible(this.displayPing::get)
            .build()
      );
   private final Setting<SettingColor> pingColor2 = this.sgPing
      .add(
         new ColorSetting.Builder()
            .name("ping-stage-2")
            .description("Color of ping text when between 75 and 200.")
            .defaultValue(new SettingColor(255, 150, 15))
            .visible(this.displayPing::get)
            .build()
      );
   private final Setting<SettingColor> pingColor3 = this.sgPing
      .add(
         new ColorSetting.Builder()
            .name("ping-stage-3")
            .description("Color of ping text when over 200.")
            .defaultValue(new SettingColor(255, 15, 15))
            .visible(this.displayPing::get)
            .build()
      );
   private final Setting<Boolean> displayDistance = this.sgDistance
      .add(new BoolSetting.Builder().name("distance").description("Shows the distance between you and the player.").defaultValue(true).build());
   private final Setting<SettingColor> distColor1 = this.sgDistance
      .add(
         new ColorSetting.Builder()
            .name("distance-stage-1")
            .description("The color when a player is within 10 blocks of you.")
            .defaultValue(new SettingColor(255, 15, 15))
            .visible(this.displayDistance::get)
            .build()
      );
   private final Setting<SettingColor> distColor2 = this.sgDistance
      .add(
         new ColorSetting.Builder()
            .name("distance-stage-2")
            .description("The color when a player is within 50 blocks of you.")
            .defaultValue(new SettingColor(255, 150, 15))
            .visible(this.displayDistance::get)
            .build()
      );
   private final Setting<SettingColor> distColor3 = this.sgDistance
      .add(
         new ColorSetting.Builder()
            .name("distance-stage-3")
            .description("The color when a player is greater then 50 blocks away from you.")
            .defaultValue(new SettingColor(15, 255, 15))
            .visible(this.displayDistance::get)
            .build()
      );
   public final Setting<Boolean> customScale = this.sgScale
      .add(
         new BoolSetting.Builder()
            .name("custom-scale")
            .description("Applies a custom scale to this hud element.")
            .defaultValue(false)
            .onChanged(var1 -> this.calculateSize())
            .build()
      );
   public final Setting<Double> scale = this.sgScale
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Custom scale.")
            .visible(this.customScale::get)
            .defaultValue(2.0)
            .onChanged(var1 -> this.calculateSize())
            .min(0.5)
            .sliderRange(0.5, 3.0)
            .build()
      );
   public final Setting<Boolean> background = this.sgBackground
      .add(new BoolSetting.Builder().name("background").description("Displays background.").defaultValue(false).build());
   public final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(this.background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
      );
   private Player playerEntity;

   public CombatHud() {
      super(INFO);
      this.calculateSize();
   }

   private void calculateSize() {
      this.setSize(175.0 * this.getScale(), 95.0 * this.getScale());
   }

   @Override
   public void render(HudRenderer renderer) {
      renderer.post(
         () -> {
            double x = this.x;
            double y = this.y;
            Color primaryColor = TextHud.getSectionColor(0);
            Color secondaryColor = TextHud.getSectionColor(1);
            if (this.isInEditor()) {
               this.playerEntity = MeteorClient.mc.player;
            } else {
               this.playerEntity = TargetUtils.getPlayerTarget(this.range.get(), SortPriority.LowestDistance);
            }

            if (this.playerEntity != null || this.isInEditor()) {
               if (this.background.get()) {
                  Renderer2D.COLOR.begin();
                  Renderer2D.COLOR.quad(x, y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
               }

               if (this.playerEntity == null) {
                  if (this.isInEditor()) {
                     renderer.line(x, y, x + this.getWidth(), y + this.getHeight(), Color.GRAY);
                     renderer.line(x + this.getWidth(), y, x, y + this.getHeight(), Color.GRAY);
                     Renderer2D.COLOR.render();
                  }
               } else {
                  Renderer2D.COLOR.render();
                  renderer.entity(
                     this.playerEntity,
                     (int)(x + 5.0 * this.getScale()),
                     (int)(y + 10.0 * this.getScale()),
                     (int)(50.0 * this.getScale()),
                     (int)(60.0 * this.getScale()),
                     -Mth.wrapDegrees(
                        this.playerEntity.yRotO
                           + (this.playerEntity.getYRot() - this.playerEntity.yRotO) * MeteorClient.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)
                     ),
                     -this.playerEntity.getXRot()
                  );
                  x += 50.0 * this.getScale();
                  y += 5.0 * this.getScale();
                  String breakText = " | ";
                  String nameText = this.playerEntity.getName().getString();
                  Color nameColor = PlayerUtils.getPlayerColor(this.playerEntity, primaryColor);
                  int ping = EntityUtils.getPing(this.playerEntity);
                  String pingText = ping + "ms";
                  Color pingColor;
                  if (ping <= 75) {
                     pingColor = this.pingColor1.get();
                  } else if (ping <= 200) {
                     pingColor = this.pingColor2.get();
                  } else {
                     pingColor = this.pingColor3.get();
                  }

                  double dist = 0.0;
                  if (!this.isInEditor()) {
                     dist = Math.round(MeteorClient.mc.player.distanceTo(this.playerEntity) * 100.0) / 100.0;
                  }

                  String distText = dist + "m";
                  Color distColor;
                  if (dist <= 10.0) {
                     distColor = this.distColor1.get();
                  } else if (dist <= 50.0) {
                     distColor = this.distColor2.get();
                  } else {
                     distColor = this.distColor3.get();
                  }

                  String friendText = "Unknown";
                  Color friendColor = primaryColor;
                  if (Friends.get().isFriend(this.playerEntity)) {
                     friendText = "Friend";
                     friendColor = Config.get().friendColor.get();
                  } else {
                     boolean naked = true;

                     for (int position = 3; position >= 0; position--) {
                        ItemStack itemStack = this.getItem(position);
                        if (!itemStack.isEmpty()) {
                           naked = false;
                        }
                     }

                     if (naked) {
                        friendText = "Naked";
                        friendColor = GREEN;
                     } else {
                        boolean threat = false;

                        for (int position = 5; position >= 0; position--) {
                           ItemStack itemStack = this.getItem(position);
                           if (itemStack.is(ItemTags.SWORDS)
                              || itemStack.getItem() == Items.END_CRYSTAL
                              || itemStack.getItem() == Items.RESPAWN_ANCHOR
                              || itemStack.getItem() instanceof BedItem) {
                              threat = true;
                           }
                        }

                        if (threat) {
                           friendText = "Threat";
                           friendColor = RED;
                        }
                     }
                  }

                  TextRenderer.get().begin(0.45 * this.getScale(), false, true);
                  double breakWidth = TextRenderer.get().getWidth(breakText);
                  double pingWidth = TextRenderer.get().getWidth(pingText);
                  double friendWidth = TextRenderer.get().getWidth(friendText);
                  TextRenderer.get().render(nameText, x, y, nameColor != null ? nameColor : primaryColor);
                  y += TextRenderer.get().getHeight();
                  TextRenderer.get().render(friendText, x, y, friendColor);
                  if (this.displayPing.get()) {
                     TextRenderer.get().render(breakText, x + friendWidth, y, secondaryColor);
                     TextRenderer.get().render(pingText, x + friendWidth + breakWidth, y, pingColor);
                     if (this.displayDistance.get()) {
                        TextRenderer.get().render(breakText, x + friendWidth + breakWidth + pingWidth, y, secondaryColor);
                        TextRenderer.get().render(distText, x + friendWidth + breakWidth + pingWidth + breakWidth, y, distColor);
                     }
                  } else if (this.displayDistance.get()) {
                     TextRenderer.get().render(breakText, x + friendWidth, y, secondaryColor);
                     TextRenderer.get().render(distText, x + friendWidth + breakWidth, y, distColor);
                  }

                  TextRenderer.get().end();
                  y += 10.0 * this.getScale();
                  int slot = 5;
                  Matrix4fStack matrices = RenderSystem.getModelViewStack();
                  matrices.pushMatrix();
                  matrices.scale((float)this.getScale(), (float)this.getScale(), 1.0F);
                  TextRenderer.get().begin(0.35, false, true);

                  for (int position = 0; position < 6; position++) {
                     double armorX = x + position * 20 * this.getScale();
                     double armorY = y;
                     ItemStack itemStack = this.getItem(slot);
                     renderer.item(itemStack, (int)armorX, (int)armorY, (float)this.getScale(), true);
                     armorY = y / this.getScale() + 18.0;
                     ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);
                     List<ObjectIntPair<Holder<Enchantment>>> enchantmentsToShow = new ArrayList<>();

                     for (Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                        if (entry.getKey().is(this.displayedEnchantments.get()::contains)) {
                           enchantmentsToShow.add(new ObjectIntImmutablePair<>(entry.getKey(), entry.getIntValue()));
                        }
                     }

                     for (ObjectIntPair<Holder<Enchantment>> entry : enchantmentsToShow) {
                        String enchantName = Utils.getEnchantSimpleName(entry.left(), 3) + " " + entry.rightInt();
                        double enchX = x / this.getScale() + position * 20 + 8.0 - TextRenderer.get().getWidth(enchantName) / 2.0;
                        TextRenderer.get().render(enchantName, enchX, armorY, entry.left().is(EnchantmentTags.CURSE) ? RED : this.enchantmentTextColor.get());
                        armorY += TextRenderer.get().getHeight();
                     }

                     slot--;
                  }

                  TextRenderer.get().end();
                  y = (int)(this.y + 75.0 * this.getScale());
                  x = this.x;
                  x /= this.getScale();
                  y /= this.getScale();
                  x += 5.0;
                  y += 5.0;
                  Renderer2D.COLOR.begin();
                  Renderer2D.COLOR.boxLines(x, y, 165.0, 11.0, BLACK);
                  Renderer2D.COLOR.render();
                  x += 2.0;
                  y += 2.0;
                  float maxHealth = this.playerEntity.getMaxHealth();
                  int maxAbsorb = 16;
                  int maxTotal = (int)(maxHealth + maxAbsorb);
                  int totalHealthWidth = (int)(161.0F * maxHealth / maxTotal);
                  int totalAbsorbWidth = 161 * maxAbsorb / maxTotal;
                  float health = this.playerEntity.getHealth();
                  float absorb = this.playerEntity.getAbsorptionAmount();
                  double healthPercent = health / maxHealth;
                  double absorbPercent = absorb / maxAbsorb;
                  int healthWidth = (int)(totalHealthWidth * healthPercent);
                  int absorbWidth = (int)(totalAbsorbWidth * absorbPercent);
                  Renderer2D.COLOR.begin();
                  Renderer2D.COLOR
                     .quad(x, y, healthWidth, 7.0, this.healthColor1.get(), this.healthColor2.get(), this.healthColor2.get(), this.healthColor1.get());
                  Renderer2D.COLOR
                     .quad(
                        x + healthWidth,
                        y,
                        absorbWidth,
                        7.0,
                        this.healthColor2.get(),
                        this.healthColor3.get(),
                        this.healthColor3.get(),
                        this.healthColor2.get()
                     );
                  Renderer2D.COLOR.render();
                  matrices.popMatrix();
               }
            }
         }
      );
   }

   private ItemStack getItem(int i) {
      if (this.isInEditor()) {
         return switch (i) {
            case 0 -> DisplayItemUtils.toStack(Items.NETHERITE_BOOTS);
            case 1 -> DisplayItemUtils.toStack(Items.NETHERITE_LEGGINGS);
            case 2 -> DisplayItemUtils.toStack(Items.NETHERITE_CHESTPLATE);
            case 3 -> DisplayItemUtils.toStack(Items.NETHERITE_HELMET);
            case 4 -> DisplayItemUtils.toStack(Items.TOTEM_OF_UNDYING);
            case 5 -> DisplayItemUtils.toStack(Items.END_CRYSTAL);
            default -> ItemStack.EMPTY;
         };
      } else {
         if (this.playerEntity == null) {
            return ItemStack.EMPTY;
         }

         return switch (i) {
            case 0 -> this.playerEntity.getItemBySlot(EquipmentSlot.FEET);
            case 1 -> this.playerEntity.getItemBySlot(EquipmentSlot.LEGS);
            case 2 -> this.playerEntity.getItemBySlot(EquipmentSlot.CHEST);
            case 3 -> this.playerEntity.getItemBySlot(EquipmentSlot.HEAD);
            case 4 -> this.playerEntity.getOffhandItem();
            case 5 -> this.playerEntity.getMainHandItem();
            default -> ItemStack.EMPTY;
         };
      }
   }

   private double getScale() {
      return this.customScale.get() ? this.scale.get() : Hud.get().getTextScale();
   }
}
