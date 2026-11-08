package meteordevelopment.meteorclient.gui;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.settings.BlockDataSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.BlockListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.BlockSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ColorSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.EnchantmentListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.EntityTypeListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.FontFaceSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ModuleListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.PacketBoolSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ParticleTypeListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.PotionSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ScreenHandlerSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.SoundEventListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.StatusEffectAmplifierMapSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.StatusEffectListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.StorageBlockListSettingScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLabel;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.widgets.WItem;
import meteordevelopment.meteorclient.gui.widgets.WItemWithLabel;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WQuad;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WBlockPosEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WDoubleEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.settings.BlockDataSetting;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BlockSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorListSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.FontFaceSetting;
import meteordevelopment.meteorclient.settings.GenericSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.ParticleTypeListSetting;
import meteordevelopment.meteorclient.settings.PotionSetting;
import meteordevelopment.meteorclient.settings.ProvidedStringSetting;
import meteordevelopment.meteorclient.settings.ScreenHandlerListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.SoundEventListSetting;
import meteordevelopment.meteorclient.settings.StatusEffectAmplifierMapSetting;
import meteordevelopment.meteorclient.settings.StatusEffectListSetting;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.settings.Vector3dSetting;
import meteordevelopment.meteorclient.systems.hud.elements.keyboard.KeyboardHud;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.Strings;

public class DefaultSettingsWidgetFactory extends SettingsWidgetFactory {
   private static final SettingColor WHITE = new SettingColor();

   public DefaultSettingsWidgetFactory(GuiTheme theme) {
      super(theme);
      this.factories.put(BoolSetting.class, (table, setting) -> this.boolW(table, (BoolSetting)setting));
      this.factories.put(IntSetting.class, (table, setting) -> this.intW(table, (IntSetting)setting));
      this.factories.put(DoubleSetting.class, (table, setting) -> this.doubleW(table, (DoubleSetting)setting));
      this.factories.put(StringSetting.class, (table, setting) -> this.stringW(table, (StringSetting)setting));
      this.factories.put(EnumSetting.class, (table, setting) -> this.enumW(table, (EnumSetting)setting));
      this.factories.put(ProvidedStringSetting.class, (table, setting) -> this.providedStringW(table, (ProvidedStringSetting)setting));
      this.factories.put(GenericSetting.class, (table, setting) -> this.genericW(table, (GenericSetting<?>)setting));
      this.factories.put(ColorSetting.class, (table, setting) -> this.colorW(table, (ColorSetting)setting));
      this.factories.put(KeybindSetting.class, (table, setting) -> this.keybindW(table, (KeybindSetting)setting));
      this.factories.put(BlockSetting.class, (table, setting) -> this.blockW(table, (BlockSetting)setting));
      this.factories.put(BlockListSetting.class, (table, setting) -> this.blockListW(table, (BlockListSetting)setting));
      this.factories.put(ItemSetting.class, (table, setting) -> this.itemW(table, (ItemSetting)setting));
      this.factories.put(ItemListSetting.class, (table, setting) -> this.itemListW(table, (ItemListSetting)setting));
      this.factories.put(EntityTypeListSetting.class, (table, setting) -> this.entityTypeListW(table, (EntityTypeListSetting)setting));
      this.factories.put(EnchantmentListSetting.class, (table, setting) -> this.enchantmentListW(table, (EnchantmentListSetting)setting));
      this.factories.put(ModuleListSetting.class, (table, setting) -> this.moduleListW(table, (ModuleListSetting)setting));
      this.factories.put(PacketListSetting.class, (table, setting) -> this.packetListW(table, (PacketListSetting)setting));
      this.factories.put(ParticleTypeListSetting.class, (table, setting) -> this.particleTypeListW(table, (ParticleTypeListSetting)setting));
      this.factories.put(SoundEventListSetting.class, (table, setting) -> this.soundEventListW(table, (SoundEventListSetting)setting));
      this.factories
         .put(StatusEffectAmplifierMapSetting.class, (table, setting) -> this.statusEffectAmplifierMapW(table, (StatusEffectAmplifierMapSetting)setting));
      this.factories.put(StatusEffectListSetting.class, (table, setting) -> this.statusEffectListW(table, (StatusEffectListSetting)setting));
      this.factories.put(StorageBlockListSetting.class, (table, setting) -> this.storageBlockListW(table, (StorageBlockListSetting)setting));
      this.factories.put(ScreenHandlerListSetting.class, (table, setting) -> this.screenHandlerListW(table, (ScreenHandlerListSetting)setting));
      this.factories.put(BlockDataSetting.class, (table, setting) -> this.blockDataW(table, (BlockDataSetting<?>)setting));
      this.factories.put(PotionSetting.class, (table, setting) -> this.potionW(table, (PotionSetting)setting));
      this.factories.put(StringListSetting.class, (table, setting) -> this.stringListW(table, (StringListSetting)setting));
      this.factories.put(BlockPosSetting.class, (table, setting) -> this.blockPosW(table, (BlockPosSetting)setting));
      this.factories.put(ColorListSetting.class, (table, setting) -> this.colorListW(table, (ColorListSetting)setting));
      this.factories.put(FontFaceSetting.class, (table, setting) -> this.fontW(table, (FontFaceSetting)setting));
      this.factories.put(Vector3dSetting.class, (table, setting) -> this.vector3dW(table, (Vector3dSetting)setting));
      this.factories.put(DoubleListSetting.class, (table, setting) -> DoubleListSetting.doubleListW(table, (DoubleListSetting)setting));
      this.factories.put(KeyboardHud.CustomKeyListSetting.class, (table, setting) -> this.customKeyListW(table, (KeyboardHud.CustomKeyListSetting)setting));
   }

   @Override
   public WWidget create(GuiTheme theme, Settings settings, String filter) {
      WVerticalList list = theme.verticalList();
      List<DefaultSettingsWidgetFactory.RemoveInfo> removeInfoList = new ArrayList<>();

      for (SettingGroup group : settings.groups) {
         this.group(list, group, filter, removeInfoList);
      }

      list.calculateSize();
      list.minWidth = list.width;

      for (DefaultSettingsWidgetFactory.RemoveInfo removeInfo : removeInfoList) {
         removeInfo.remove(list);
      }

      return list;
   }

   protected double settingTitleTopMargin() {
      return 6.0;
   }

   private void group(WVerticalList list, SettingGroup group, String filter, List<DefaultSettingsWidgetFactory.RemoveInfo> removeInfoList) {
      WSection section = list.add(this.theme.section(group.name, group.sectionExpanded)).expandX().widget();
      section.action = () -> group.sectionExpanded = section.isExpanded();
      WTable table = section.add(this.theme.table()).expandX().widget();
      DefaultSettingsWidgetFactory.RemoveInfo removeInfo = null;

      for (Setting<?> setting : group) {
         if (Strings.CI.contains(setting.title, filter)) {
            boolean visible = setting.isVisible();
            setting.lastWasVisible = visible;
            if (!visible) {
               if (removeInfo == null) {
                  removeInfo = new DefaultSettingsWidgetFactory.RemoveInfo(section, table);
               }

               removeInfo.markRowForRemoval();
            }

            table.add(this.theme.label(setting.title)).top().marginTop(this.settingTitleTopMargin()).widget().tooltip = setting.description;
            SettingsWidgetFactory.Factory factory = this.getFactory(setting.getClass());
            if (factory != null) {
               factory.create(table, setting);
            }

            table.row();
         }
      }

      if (removeInfo != null) {
         removeInfoList.add(removeInfo);
      }
   }

   private void boolW(WTable table, BoolSetting setting) {
      WCheckbox checkbox = table.add(this.theme.checkbox(setting.get())).expandCellX().widget();
      checkbox.action = () -> setting.set(checkbox.checked);
      this.reset(table, setting, () -> checkbox.checked = setting.get());
   }

   private void intW(WTable table, IntSetting setting) {
      WIntEdit edit = table.add(this.theme.intEdit(setting.get(), setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.noSlider))
         .expandX()
         .widget();
      edit.action = () -> {
         if (!setting.set(edit.get())) {
            edit.set(setting.get());
         }
      };
      this.reset(table, setting, () -> edit.set(setting.get()));
   }

   private void doubleW(WTable table, DoubleSetting setting) {
      WDoubleEdit edit = this.theme
         .doubleEdit(setting.get(), setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.decimalPlaces, setting.noSlider);
      table.add(edit).expandX();
      Runnable action = () -> {
         if (!setting.set(edit.get())) {
            edit.set(setting.get());
         }
      };
      if (setting.onSliderRelease) {
         edit.actionOnRelease = action;
      } else {
         edit.action = action;
      }

      this.reset(table, setting, () -> edit.set(setting.get()));
   }

   private void stringW(WTable table, StringSetting setting) {
      CharFilter filter = setting.filter == null ? (var0, var1x) -> true : setting.filter;
      Cell<WTextBox> cell = table.add(this.theme.textBox(setting.get(), setting.placeholder, filter, setting.renderer));
      if (setting.wide) {
         cell.minWidth(Utils.getWindowWidth() - Utils.getWindowWidth() / 4.0);
      }

      WTextBox textBox = cell.expandX().widget();
      textBox.action = () -> setting.set(textBox.get());
      this.reset(table, setting, () -> textBox.set(setting.get()));
   }

   private void stringListW(WTable table, StringListSetting setting) {
      WTable wtable = table.add(this.theme.table()).expandX().widget();
      StringListSetting.fillTable(this.theme, wtable, setting);
   }

   private <T extends Enum<?>> void enumW(WTable table, EnumSetting<T> setting) {
      WDropdown<T> dropdown = table.add(this.theme.dropdown(setting.get())).expandCellX().widget();
      dropdown.action = () -> setting.set(dropdown.get());
      this.reset(table, setting, () -> dropdown.set(setting.get()));
   }

   private void providedStringW(WTable table, ProvidedStringSetting setting) {
      WDropdown<String> dropdown = table.add(this.theme.dropdown(setting.supplier.get(), setting.get())).expandCellX().widget();
      dropdown.action = () -> setting.set(dropdown.get());
      this.reset(table, setting, () -> dropdown.set(setting.get()));
   }

   private void genericW(WTable table, GenericSetting<?> setting) {
      WButton edit = table.add(this.theme.button(GuiRenderer.EDIT)).widget();
      edit.action = () -> MeteorClient.mc.setScreen(setting.createScreen(this.theme));
      this.reset(table, setting, null);
   }

   private void colorW(WTable table, ColorSetting setting) {
      WHorizontalList list = table.add(this.theme.horizontalList()).expandX().widget();
      WQuad quad = list.add(this.theme.quad(setting.get())).widget();
      WButton edit = list.add(this.theme.button(GuiRenderer.EDIT)).widget();
      edit.action = () -> MeteorClient.mc.setScreen(new ColorSettingScreen(this.theme, setting));
      this.reset(table, setting, () -> quad.color = setting.get());
   }

   private void keybindW(WTable table, KeybindSetting setting) {
      WHorizontalList list = table.add(this.theme.horizontalList()).expandX().widget();
      WKeybind keybind = list.add(this.theme.keybind(setting.get(), setting.getDefaultValue())).expandX().widget();
      keybind.action = setting::onChanged;
      setting.widget = keybind;
      WButton reset = list.add(this.theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
      reset.action = keybind::resetBind;
      reset.tooltip = "Reset";
   }

   private void blockW(WTable table, BlockSetting setting) {
      WHorizontalList list = table.add(this.theme.horizontalList()).expandX().widget();
      WItem item = list.add(this.theme.item(DisplayItemUtils.toStack(setting.get().asItem()))).widget();
      WButton select = list.add(this.theme.button("Select")).widget();
      select.action = () -> {
         BlockSettingScreen screen = new BlockSettingScreen(this.theme, setting);
         screen.onClosed(() -> item.set(DisplayItemUtils.toStack(setting.get().asItem())));
         MeteorClient.mc.setScreen(screen);
      };
      this.reset(table, setting, () -> item.set(DisplayItemUtils.toStack(setting.get().asItem())));
   }

   private void blockPosW(WTable table, BlockPosSetting setting) {
      WBlockPosEdit edit = table.add(this.theme.blockPosEdit(setting.get())).expandX().widget();
      edit.actionOnRelease = () -> {
         if (!setting.set(edit.get())) {
            edit.set(setting.get());
         }
      };
      this.reset(table, setting, () -> edit.set(setting.get()));
   }

   private void blockListW(WTable table, BlockListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new BlockListSettingScreen(this.theme, setting)));
   }

   private void itemW(WTable table, ItemSetting setting) {
      WHorizontalList list = table.add(this.theme.horizontalList()).expandX().widget();
      WItem item = list.add(this.theme.item(DisplayItemUtils.toStack(setting.get()))).widget();
      WButton select = list.add(this.theme.button("Select")).widget();
      select.action = () -> {
         ItemSettingScreen screen = new ItemSettingScreen(this.theme, setting);
         screen.onClosed(() -> item.set(DisplayItemUtils.toStack(setting.get())));
         MeteorClient.mc.setScreen(screen);
      };
      this.reset(table, setting, () -> item.set(DisplayItemUtils.toStack(setting.get())));
   }

   private void itemListW(WTable table, ItemListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new ItemListSettingScreen(this.theme, setting)));
   }

   private void entityTypeListW(WTable table, EntityTypeListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new EntityTypeListSettingScreen(this.theme, setting)));
   }

   private void enchantmentListW(WTable table, EnchantmentListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new EnchantmentListSettingScreen(this.theme, setting)));
   }

   private void moduleListW(WTable table, ModuleListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new ModuleListSettingScreen(this.theme, setting)));
   }

   private void packetListW(WTable table, PacketListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new PacketBoolSettingScreen(this.theme, setting)));
   }

   private void particleTypeListW(WTable table, ParticleTypeListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new ParticleTypeListSettingScreen(this.theme, setting)));
   }

   private void soundEventListW(WTable table, SoundEventListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new SoundEventListSettingScreen(this.theme, setting)));
   }

   private void statusEffectAmplifierMapW(WTable table, StatusEffectAmplifierMapSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new StatusEffectAmplifierMapSettingScreen(this.theme, setting)));
   }

   private void statusEffectListW(WTable table, StatusEffectListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new StatusEffectListSettingScreen(this.theme, setting)));
   }

   private void storageBlockListW(WTable table, StorageBlockListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new StorageBlockListSettingScreen(this.theme, setting)));
   }

   private void screenHandlerListW(WTable table, ScreenHandlerListSetting setting) {
      this.selectW(table, setting, () -> MeteorClient.mc.setScreen(new ScreenHandlerSettingScreen(this.theme, setting)));
   }

   private void blockDataW(WTable table, BlockDataSetting<?> setting) {
      WButton button = table.add(this.theme.button(GuiRenderer.EDIT)).expandCellX().widget();
      button.action = () -> MeteorClient.mc.setScreen(new BlockDataSettingScreen<>(this.theme, setting));
      this.reset(table, setting, null);
   }

   private void potionW(WTable table, PotionSetting setting) {
      ItemStack potion = setting.get().potion.get();
      WHorizontalList list = table.add(this.theme.horizontalList()).expandX().widget();
      WItemWithLabel item = list.add(this.theme.itemWithLabel(potion, I18n.get(potion.getItem().getDescriptionId()))).widget();
      WButton button = list.add(this.theme.button("Select")).expandCellX().widget();
      button.action = () -> {
         WidgetScreen screen = new PotionSettingScreen(this.theme, setting);
         screen.onClosed(() -> item.set(potion));
         MeteorClient.mc.setScreen(screen);
      };
      this.reset(list, setting, () -> item.set(potion));
   }

   private void fontW(WTable table, FontFaceSetting setting) {
      WHorizontalList list = table.add(this.theme.horizontalList()).expandX().widget();
      WLabel label = list.add(this.theme.label(setting.get().info.family())).widget();
      WButton button = list.add(this.theme.button("Select")).expandCellX().widget();
      button.action = () -> {
         WidgetScreen screen = new FontFaceSettingScreen(this.theme, setting);
         screen.onClosed(() -> label.set(setting.get().info.family()));
         MeteorClient.mc.setScreen(screen);
      };
      this.reset(list, setting, () -> label.set(Fonts.DEFAULT_FONT.info.family()));
   }

   private void colorListW(WTable table, ColorListSetting setting) {
      WTable tab = table.add(this.theme.table()).expandX().widget();
      WTable t = tab.add(this.theme.table()).expandX().widget();
      tab.row();
      this.colorListWFill(t, setting);
      WPlus add = tab.add(this.theme.plus()).expandCellX().widget();
      add.action = () -> {
         setting.get().add(new SettingColor());
         setting.onChanged();
         t.clear();
         this.colorListWFill(t, setting);
      };
      this.reset(tab, setting, () -> {
         t.clear();
         this.colorListWFill(t, setting);
      });
   }

   private void colorListWFill(WTable t, ColorListSetting setting) {
      int i = 0;

      for (SettingColor color : setting.get()) {
         int _i = i;
         t.add(this.theme.label(i + ":"));
         t.add(this.theme.quad(color)).widget();
         WButton edit = t.add(this.theme.button(GuiRenderer.EDIT)).widget();
         edit.action = () -> {
            SettingColor defaultValue = WHITE;
            if (_i < setting.getDefaultValue().size()) {
               defaultValue = setting.getDefaultValue().get(_i);
            }

            ColorSetting set = new ColorSetting(setting.name, setting.description, defaultValue, settingColor -> {
               setting.get().get(_i).set(settingColor);
               setting.onChanged();
            }, null, null);
            set.set(setting.get().get(_i));
            MeteorClient.mc.setScreen(new ColorSettingScreen(this.theme, set));
         };
         WMinus remove = t.add(this.theme.minus()).expandCellX().right().widget();
         remove.action = () -> {
            setting.get().remove(_i);
            setting.onChanged();
            t.clear();
            this.colorListWFill(t, setting);
         };
         t.row();
         i++;
      }
   }

   private void vector3dW(WTable table, Vector3dSetting setting) {
      WTable internal = table.add(this.theme.table()).expandX().widget();
      WDoubleEdit x = this.addVectorComponent(internal, "X", setting.get().x, val -> setting.get().x = val, setting);
      WDoubleEdit y = this.addVectorComponent(internal, "Y", setting.get().y, val -> setting.get().y = val, setting);
      WDoubleEdit z = this.addVectorComponent(internal, "Z", setting.get().z, val -> setting.get().z = val, setting);
      this.reset(table, setting, () -> {
         x.set(setting.get().x);
         y.set(setting.get().y);
         z.set(setting.get().z);
      });
   }

   private WDoubleEdit addVectorComponent(WTable table, String label, double value, DoubleConsumer update, Vector3dSetting setting) {
      table.add(this.theme.label(label + ": "));
      WDoubleEdit component = table.add(
            this.theme.doubleEdit(value, setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.decimalPlaces, setting.noSlider)
         )
         .expandX()
         .widget();
      if (setting.onSliderRelease) {
         component.actionOnRelease = () -> {
            update.accept(component.get());
            setting.onChanged();
         };
      } else {
         component.action = () -> {
            update.accept(component.get());
            setting.onChanged();
         };
      }

      table.row();
      return component;
   }

   private void customKeyListW(WTable table, KeyboardHud.CustomKeyListSetting setting) {
      WTable wtable = table.add(this.theme.table()).expandX().widget();
      KeyboardHud.fillTable(this.theme, wtable, setting);
   }

   private void selectW(WContainer c, Setting<?> setting, Runnable action) {
      boolean addCount = DefaultSettingsWidgetFactory.WSelectedCountLabel.getSize(setting) != -1;
      WContainer c2 = c;
      if (addCount) {
         c2 = c.add(this.theme.horizontalList()).expandCellX().widget();
         ((WHorizontalList)c2).spacing *= 2.0;
      }

      WButton button = c2.add(this.theme.button("Select")).expandCellX().widget();
      button.action = action;
      if (addCount) {
         c2.add(new DefaultSettingsWidgetFactory.WSelectedCountLabel(setting).color(this.theme.textSecondaryColor()));
      }

      this.reset(c, setting, null);
   }

   private void reset(WContainer c, Setting<?> setting, Runnable action) {
      WButton reset = c.add(this.theme.button(GuiRenderer.RESET)).widget();
      reset.action = () -> {
         setting.reset();
         if (action != null) {
            action.run();
         }
      };
      reset.tooltip = "Reset";
   }

   private static class RemoveInfo {
      private final WSection section;
      private final WTable table;
      private final IntList rowIds = new IntArrayList();

      public RemoveInfo(WSection section, WTable table) {
         this.section = section;
         this.table = table;
      }

      public void markRowForRemoval() {
         this.rowIds.add(this.table.rowI());
      }

      public void remove(WVerticalList list) {
         for (int i = 0; i < this.rowIds.size(); i++) {
            this.table.removeRow(this.rowIds.getInt(i) - i);
         }

         if (this.table.cells.isEmpty()) {
            list.cells.removeIf(cell -> cell.widget() == this.section);
         }
      }
   }

   private static class WSelectedCountLabel extends WMeteorLabel {
      private final Setting<?> setting;
      private int lastSize = -1;

      public WSelectedCountLabel(Setting<?> setting) {
         super("", false);
         this.setting = setting;
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         int size = getSize(this.setting);
         if (size != this.lastSize) {
            this.set("(" + size + " selected)");
            this.lastSize = size;
         }

         super.onRender(renderer, mouseX, mouseY, delta);
      }

      public static int getSize(Setting<?> setting) {
         if (setting.get() instanceof Collection<?> collection) {
            return collection.size();
         } else {
            return setting.get() instanceof Map<?, ?> map ? map.size() : -1;
         }
      }
   }
}
