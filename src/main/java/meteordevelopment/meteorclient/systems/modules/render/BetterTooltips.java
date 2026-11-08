package meteordevelopment.meteorclient.systems.modules.render;

import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.events.render.TooltipDataEvent;
import meteordevelopment.meteorclient.gui.screens.ContainerInventoryScreen;
import meteordevelopment.meteorclient.mixin.EntityAccessor;
import meteordevelopment.meteorclient.mixin.MobBucketItemAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ByteCountDataOutput;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.tooltip.BannerTooltipComponent;
import meteordevelopment.meteorclient.utils.tooltip.BookTooltipComponent;
import meteordevelopment.meteorclient.utils.tooltip.BundleTooltipComponent;
import meteordevelopment.meteorclient.utils.tooltip.ContainerTooltipComponent;
import meteordevelopment.meteorclient.utils.tooltip.EntityTooltipComponent;
import meteordevelopment.meteorclient.utils.tooltip.MapTooltipComponent;
import meteordevelopment.meteorclient.utils.tooltip.TextTooltipComponent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.SuspiciousStewEffects.Entry;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Builder;
import net.minecraft.world.level.saveddata.maps.MapId;

public class BetterTooltips extends Module {
   public static final Color ECHEST_COLOR = new Color(0, 50, 50);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPreviews = this.settings.createGroup("Previews");
   private final SettingGroup sgOther = this.settings.createGroup("Other");
   private final SettingGroup sgHideFlags = this.settings.createGroup("Hide Flags");
   private final Setting<BetterTooltips.DisplayWhen> displayWhen = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("display-when"))
                     .description("When to display previews."))
                  .defaultValue(BetterTooltips.DisplayWhen.Keybind))
               .onChanged(var1 -> this.updateTooltips = true))
            .build()
      );
   private final Setting<Keybind> keybind = this.sgGeneral
      .add(
         new KeybindSetting.Builder()
            .name("keybind")
            .description("The bind for keybind mode.")
            .defaultValue(Keybind.fromKey(342))
            .visible(() -> this.displayWhen.get() == BetterTooltips.DisplayWhen.Keybind)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   private final Setting<Boolean> openContents = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("open-contents")
            .description("Opens a GUI window with the inventory of the storage block or book when you click the item.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Keybind> openContentsKey = this.sgGeneral
      .add(
         new KeybindSetting.Builder()
            .name("keybind")
            .description("Key to open contents (containers, books, etc.) when pressed on items.")
            .defaultValue(Keybind.fromButton(2))
            .visible(this.openContents::get)
            .build()
      );
   private final Setting<Boolean> pauseInCreative = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("pause-in-creative")
            .description("Pauses middle click open while the player is in creative mode.")
            .defaultValue(true)
            .visible(this.openContents::get)
            .build()
      );
   private final Setting<Boolean> shulkers = this.sgPreviews
      .add(
         new BoolSetting.Builder()
            .name("containers")
            .description("Shows a preview of a containers when hovering over it in an inventory.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   private final Setting<Boolean> shulkerCompactTooltip = this.sgPreviews
      .add(new BoolSetting.Builder().name("compact-shulker-tooltip").description("Compacts the lines of the shulker tooltip.").defaultValue(true).build());
   private final Setting<Boolean> echest = this.sgPreviews
      .add(
         new BoolSetting.Builder()
            .name("echests")
            .description("Shows a preview of your echest when hovering over it in an inventory.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   private final Setting<Boolean> maps = this.sgPreviews
      .add(
         new BoolSetting.Builder()
            .name("maps")
            .description("Shows a preview of a map when hovering over it in an inventory.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   public final Setting<Double> mapsScale = this.sgPreviews
      .add(
         new DoubleSetting.Builder()
            .name("map-scale")
            .description("The scale of the map preview.")
            .defaultValue(1.0)
            .min(0.001)
            .sliderMax(1.0)
            .visible(this.maps::get)
            .build()
      );
   private final Setting<Boolean> books = this.sgPreviews
      .add(
         new BoolSetting.Builder()
            .name("books")
            .description("Shows contents of a book when hovering over it in an inventory.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   private final Setting<Boolean> banners = this.sgPreviews
      .add(
         new BoolSetting.Builder()
            .name("banners")
            .description("Shows banners' patterns when hovering over it in an inventory. Also works with shields.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   private final Setting<Boolean> entitiesInBuckets = this.sgPreviews
      .add(
         new BoolSetting.Builder()
            .name("entities-in-buckets")
            .description("Shows entities in buckets when hovering over it in an inventory.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   private final Setting<Boolean> bundles = this.sgPreviews
      .add(
         new BoolSetting.Builder()
            .name("bundles")
            .description("Shows a preview of bundle contents when hovering over it in an inventory.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   private final Setting<Boolean> foodInfo = this.sgPreviews
      .add(
         new BoolSetting.Builder()
            .name("food-info")
            .description("Shows hunger and saturation values for food items.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   public final Setting<Boolean> byteSize = this.sgOther
      .add(
         new BoolSetting.Builder()
            .name("byte-size")
            .description("Displays an item's size in bytes in the tooltip.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   private final Setting<BetterTooltips.SortSize> sizeType = this.sgOther
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("byte-size-format"))
                     .description("The format by which to display the item's byte size."))
                  .defaultValue(BetterTooltips.SortSize.Dynamic))
               .visible(this.byteSize::get))
            .build()
      );
   private final Setting<Boolean> statusEffects = this.sgOther
      .add(
         new BoolSetting.Builder()
            .name("status-effects")
            .description("Adds list of status effects to tooltips of food items.")
            .defaultValue(true)
            .onChanged(var1 -> this.updateTooltips = true)
            .build()
      );
   public final Setting<Boolean> tooltip = this.sgHideFlags
      .add(new BoolSetting.Builder().name("tooltip").description("Show the tooltip when it's hidden.").defaultValue(false).build());
   public final Setting<Boolean> additional = this.sgHideFlags
      .add(
         new BoolSetting.Builder()
            .name("tooltip-components")
            .description("Shows tooltip components when they're hidden - e.g. enchantments, attributes, lore, etc.")
            .defaultValue(false)
            .build()
      );
   private boolean updateTooltips = false;
   private static final ItemStack[] PREVIEW = new ItemStack[27];
   private static final ItemStack[] PEEK_SCREEN = new ItemStack[27];

   public BetterTooltips() {
      super(Categories.Render, "better-tooltips", "Displays more useful tooltips for certain items.");
   }

   @EventHandler
   private void appendTooltip(ItemStackTooltipEvent event) {
      if (!this.tooltip.get() && event.list().isEmpty()) {
         this.appendPreviewTooltipText(event, false);
      } else {
         if (this.statusEffects.get()) {
            if (event.itemStack().getItem() == Items.SUSPICIOUS_STEW) {
               SuspiciousStewEffects stewEffectsComponent = event.itemStack().get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
               if (stewEffectsComponent != null) {
                  for (Entry effectTag : stewEffectsComponent.effects()) {
                     MobEffectInstance effect = new MobEffectInstance(effectTag.effect(), effectTag.duration(), 0);
                     event.appendStart(this.getStatusText(effect));
                  }
               }
            } else {
               Consumable consumable = event.itemStack().get(DataComponents.CONSUMABLE);
               if (consumable != null) {
                  consumable.onConsumeEffects()
                     .stream()
                     .filter(ApplyStatusEffectsConsumeEffect.class::isInstance)
                     .map(ApplyStatusEffectsConsumeEffect.class::cast)
                     .flatMap(apply -> apply.effects().stream())
                     .forEach(effect -> event.appendStart(this.getStatusText(effect)));
               }
            }
         }

         if (this.foodInfo.get() && event.itemStack().has(DataComponents.FOOD)) {
            FoodProperties food = event.itemStack().get(DataComponents.FOOD);
            event.appendStart(
               Component.literal(String.format("\ud83c\udf56 %d (\ud83d\udc9b %.1f)", food.nutrition(), food.saturation())).withStyle(ChatFormatting.GRAY)
            );
         }

         if (this.byteSize.get()) {
            switch (ItemStack.CODEC.encodeStart(this.mc.player.registryAccess().createSerializationContext(NbtOps.INSTANCE), event.itemStack())) {
               case Success<Tag> success:
                  try {
                     success.value().write(ByteCountDataOutput.INSTANCE);
                     int byteCount = ByteCountDataOutput.INSTANCE.getCount();

                     String count = switch ((BetterTooltips.SortSize)this.sizeType.get()) {
                        case Bytes -> String.format("%d bytes", byteCount);
                        case Kilobytes -> String.format("%.2f kB", byteCount / 1024.0F);
                        case Megabytes -> String.format("%.4f MB", byteCount / 1048576.0F);
                        case Dynamic -> byteCount >= 1048576
                           ? String.format("%.2f MB", byteCount / 1048576.0F)
                           : (byteCount >= 1024 ? String.format("%.2f kB", byteCount / 1024.0F) : String.format("%d bytes", byteCount));
                     };
                     ByteCountDataOutput.INSTANCE.reset();
                     event.appendEnd(Component.literal(count).withStyle(ChatFormatting.DARK_GRAY));
                  } catch (Exception var7) {
                     event.appendEnd(Component.literal("Error getting bytes.").withStyle(ChatFormatting.RED));
                  }
                  break;
               case Error var14:
                  event.appendEnd(Component.literal("Error getting bytes.").withStyle(ChatFormatting.RED));
                  break;
               default:
                  throw new MatchException(null, null);
            }
         }

         this.appendPreviewTooltipText(event, true);
      }
   }

   @EventHandler
   private void getTooltipData(TooltipDataEvent event) {
      if (this.previewShulkers() && Utils.hasItems(event.itemStack)) {
         Utils.getItemsInContainerItem(event.itemStack, PREVIEW);
         event.tooltipData = new ContainerTooltipComponent(PREVIEW, Utils.getShulkerColor(event.itemStack));
      } else if (event.itemStack.getItem() == Items.ENDER_CHEST && this.previewEChest()) {
         event.tooltipData = EChestMemory.isKnown()
            ? new ContainerTooltipComponent(EChestMemory.ITEMS.toArray(ItemStack[]::new), ECHEST_COLOR)
            : new TextTooltipComponent(Component.literal("Unknown inventory.").withStyle(ChatFormatting.DARK_RED));
      } else if (event.itemStack.getItem() == Items.FILLED_MAP && this.previewMaps()) {
         MapId mapIdComponent = event.itemStack.get(DataComponents.MAP_ID);
         if (mapIdComponent != null) {
            event.tooltipData = new MapTooltipComponent(mapIdComponent.id());
         }
      } else if ((event.itemStack.getItem() == Items.WRITABLE_BOOK || event.itemStack.getItem() == Items.WRITTEN_BOOK) && this.previewBooks()) {
         Component page = this.getFirstPage(event.itemStack);
         if (page != null) {
            int pageCount = this.getBookPageCount(event.itemStack);
            MutableComponent pageWithCount = page.copy().append(Component.literal(String.format(" (%d pages)", pageCount)).withStyle(ChatFormatting.GRAY));
            event.tooltipData = new BookTooltipComponent(pageWithCount);
         }
      } else if (event.itemStack.getItem() instanceof BannerItem && this.previewBanners()) {
         event.tooltipData = new BannerTooltipComponent(event.itemStack);
      } else if (event.itemStack.has(DataComponents.PROVIDES_BANNER_PATTERNS) && this.previewBanners()) {
         event.tooltipData = this.createBannerFromBannerPatternItem(event.itemStack);
      } else if (event.itemStack.getItem() != Items.SHIELD || !this.previewBanners()) {
         if (event.itemStack.getItem() instanceof MobBucketItem bucketItem && this.previewEntities()) {
            EntityType<?> type = ((MobBucketItemAccessor)bucketItem).meteor$getType();
            LivingEntity entity = (LivingEntity)type.create(this.mc.level, EntitySpawnReason.NATURAL);
            if (entity != null) {
               CustomData nbtComponent = event.itemStack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, null);
               if (nbtComponent == null) {
                  return;
               }

               entity.applyComponentsFromItemStack(event.itemStack);
               ((Bucketable)entity).loadFromBucketTag(nbtComponent.copyTag());
               ((EntityAccessor)entity).meteor$setInWater(true);
               event.tooltipData = new EntityTooltipComponent(entity);
            }
         } else if (event.itemStack.getItem() instanceof BundleItem && this.previewBundles() && event.itemStack.has(DataComponents.BUNDLE_CONTENTS)) {
            BundleContents bundleContents = event.itemStack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundleContents != null && !bundleContents.isEmpty()) {
               ItemStack[] bundleItems = new ItemStack[bundleContents.size()];
               int index = 0;

               for (ItemStackTemplate template : bundleContents.items()) {
                  bundleItems[index++] = template.create();
               }

               event.tooltipData = new BundleTooltipComponent(bundleItems, bundleContents);
            }
         }
      } else if (!event.itemStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().isEmpty()) {
         event.tooltipData = this.createBannerFromShield(event.itemStack);
      }
   }

   public void applyCompactShulkerTooltip(List<Optional<ItemStackTemplate>> stacks, Consumer<Component> textConsumer) {
      Object2IntMap<Item> counts = new Object2IntOpenHashMap<>();

      for (Optional<ItemStackTemplate> opt : stacks) {
         if (!opt.isEmpty()) {
            Item stackItem = opt.get().item().value();
            int stackCount = opt.get().count();
            if (stackCount != 0) {
               int count = counts.getInt(stackItem);
               counts.put(stackItem, count + stackCount);
            }
         }
      }

      counts.keySet().stream().sorted(Comparator.comparingInt(value -> -counts.getInt(value))).limit(5L).forEach(item -> {
         MutableComponent mutableText = item.components().get(DataComponents.ITEM_NAME).plainCopy();
         mutableText.append(Component.literal(" x").append(String.valueOf(counts.getInt(item))).withStyle(ChatFormatting.GRAY));
         textConsumer.accept(mutableText);
      });
      if (counts.size() > 5) {
         textConsumer.accept(Component.translatable("item.container.more_items", counts.size() - 5).withStyle(ChatFormatting.ITALIC));
      }
   }

   private void appendPreviewTooltipText(ItemStackTooltipEvent event, boolean spacer) {
      boolean showPreviewText = !this.isPressed()
         && (
            this.shulkers.get() && Utils.hasItems(event.itemStack())
               || event.itemStack().getItem() == Items.ENDER_CHEST && this.echest.get()
               || event.itemStack().getItem() == Items.FILLED_MAP && this.maps.get()
               || event.itemStack().getItem() == Items.WRITABLE_BOOK && this.books.get()
               || event.itemStack().getItem() == Items.WRITTEN_BOOK && this.books.get()
               || event.itemStack().getItem() instanceof MobBucketItem && this.entitiesInBuckets.get()
               || event.itemStack().getItem() instanceof BundleItem && this.bundles.get()
               || event.itemStack().getItem() instanceof BannerItem && this.banners.get()
               || event.itemStack().has(DataComponents.PROVIDES_BANNER_PATTERNS) && this.banners.get()
               || event.itemStack().getItem() == Items.SHIELD && this.banners.get()
         );
      if (showPreviewText) {
         if (spacer) {
            event.appendEnd(Component.literal(""));
         }

         event.appendEnd(Component.literal("Hold " + ChatFormatting.YELLOW + this.keybind + ChatFormatting.RESET + " to preview"));
      }
   }

   private MutableComponent getStatusText(MobEffectInstance effect) {
      MutableComponent text = Component.translatable(effect.getDescriptionId());
      if (effect.getAmplifier() != 0) {
         text.append(
            String.format(
               " %d (%s)", effect.getAmplifier() + 1, MobEffectUtil.formatDuration(effect, 1.0F, this.mc.level.tickRateManager().tickrate()).getString()
            )
         );
      } else {
         text.append(String.format(" (%s)", MobEffectUtil.formatDuration(effect, 1.0F, this.mc.level.tickRateManager().tickrate()).getString()));
      }

      return effect.getEffect().value().isBeneficial() ? text.withStyle(ChatFormatting.BLUE) : text.withStyle(ChatFormatting.RED);
   }

   private Component getFirstPage(ItemStack bookItem) {
      if (bookItem.get(DataComponents.WRITABLE_BOOK_CONTENT) != null) {
         List<Filterable<String>> pages = bookItem.get(DataComponents.WRITABLE_BOOK_CONTENT).pages();
         return pages.isEmpty() ? null : Component.literal(pages.getFirst().get(false));
      } else if (bookItem.get(DataComponents.WRITTEN_BOOK_CONTENT) != null) {
         List<Filterable<Component>> pages = bookItem.get(DataComponents.WRITTEN_BOOK_CONTENT).pages();
         return pages.isEmpty() ? null : pages.getFirst().get(false);
      } else {
         return null;
      }
   }

   private int getBookPageCount(ItemStack bookItem) {
      if (bookItem.get(DataComponents.WRITABLE_BOOK_CONTENT) != null) {
         return bookItem.get(DataComponents.WRITABLE_BOOK_CONTENT).pages().size();
      } else {
         return bookItem.get(DataComponents.WRITTEN_BOOK_CONTENT) != null ? bookItem.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().size() : 0;
      }
   }

   private BannerTooltipComponent createBannerFromBannerPatternItem(ItemStack item) {
      HolderSet<BannerPattern> providedPatterns = item.get(DataComponents.PROVIDES_BANNER_PATTERNS);
      if (providedPatterns != null && providedPatterns.size() != 0) {
         BannerPatternLayers component = new Builder().add(providedPatterns.get(0), DyeColor.WHITE).build();
         return new BannerTooltipComponent(DyeColor.GRAY, component);
      } else {
         return new BannerTooltipComponent(DyeColor.GRAY, BannerPatternLayers.EMPTY);
      }
   }

   private BannerTooltipComponent createBannerFromShield(ItemStack shieldItem) {
      DyeColor dyeColor2 = shieldItem.getOrDefault(DataComponents.BASE_COLOR, DyeColor.WHITE);
      BannerPatternLayers bannerPatternsComponent = shieldItem.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      return new BannerTooltipComponent(dyeColor2, bannerPatternsComponent);
   }

   public boolean openContents() {
      return this.isActive() && this.openContents.get() && (!this.pauseInCreative.get() || !this.mc.player.hasInfiniteMaterials());
   }

   public boolean shouldOpenContents(InputWithModifiers input) {
      if (input instanceof MouseButtonEvent click) {
         return this.openContents() && this.openContentsKey.get().matches(click.buttonInfo());
      } else {
         return !(input instanceof KeyEvent keyInput) ? false : this.openContents() && this.openContentsKey.get().matches(keyInput);
      }
   }

   public boolean openContent(ItemStack itemStack) {
      if (!this.openContents() || itemStack.isEmpty()) {
         return false;
      }

      if (itemStack.getItem() instanceof BundleItem) {
         if (this.mc.screen instanceof AbstractContainerScreen) {
            this.mc.screen.onClose();
         }

         this.mc.setScreen(new ContainerInventoryScreen(itemStack));
         return true;
      } else {
         if (Utils.hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST) {
            Utils.openContainer(itemStack, PEEK_SCREEN, false);
            return true;
         }

         if (itemStack.getItem() != Items.WRITABLE_BOOK && itemStack.getItem() != Items.WRITTEN_BOOK) {
            return false;
         }

         if (this.mc.screen instanceof AbstractContainerScreen) {
            this.mc.screen.onClose();
         }

         this.mc.setScreen(new BookViewScreen(BookAccess.fromItem(itemStack)));
         return true;
      }
   }

   public boolean previewShulkers() {
      return this.isActive() && this.isPressed() && this.shulkers.get();
   }

   public boolean shulkerCompactTooltip() {
      return this.isActive() && this.shulkerCompactTooltip.get();
   }

   private boolean previewEChest() {
      return this.isPressed() && this.echest.get();
   }

   private boolean previewMaps() {
      return this.isPressed() && this.maps.get();
   }

   private boolean previewBooks() {
      return this.isPressed() && this.books.get();
   }

   private boolean previewBanners() {
      return this.isPressed() && this.banners.get();
   }

   private boolean previewEntities() {
      return this.isPressed() && this.entitiesInBuckets.get();
   }

   public boolean previewBundles() {
      return this.isPressed() && this.bundles.get();
   }

   private boolean isPressed() {
      return this.keybind.get().isPressed() && this.displayWhen.get() == BetterTooltips.DisplayWhen.Keybind
         || this.displayWhen.get() == BetterTooltips.DisplayWhen.Always;
   }

   public boolean updateTooltips() {
      if (this.updateTooltips && this.isActive()) {
         this.updateTooltips = false;
         return true;
      } else {
         return false;
      }
   }

   public enum DisplayWhen {
      Keybind,
      Always;
   }

   public enum SortSize {
      Bytes,
      Kilobytes,
      Megabytes,
      Dynamic;
   }
}
