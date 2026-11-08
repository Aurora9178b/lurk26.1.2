package meteordevelopment.meteorclient.gui.screens.settings;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;

public class EntityTypeListSettingScreen extends WindowScreen {
   private static Texture EMPTY_SPAWN_EGG_TEXTURE;
   private final EntityTypeListSetting setting;
   private WVerticalList list;
   private final WTextBox filter;
   private String filterText = "";
   private WSection animals;
   private WSection waterAnimals;
   private WSection monsters;
   private WSection ambient;
   private WSection misc;
   private WTable animalsT;
   private WTable waterAnimalsT;
   private WTable monstersT;
   private WTable ambientT;
   private WTable miscT;
   int hasAnimal = 0;
   int hasWaterAnimal = 0;
   int hasMonster = 0;
   int hasAmbient = 0;
   int hasMisc = 0;

   public EntityTypeListSettingScreen(GuiTheme theme, EntityTypeListSetting setting) {
      super(theme, "Select entities");
      this.setting = setting;
      this.filter = super.add(theme.textBox("")).minWidth(400.0).expandX().widget();
      this.filter.setFocused(true);
      this.filter.action = () -> {
         this.filterText = this.filter.get().trim();
         this.list.clear();
         this.initWidgets();
      };
      this.list = super.add(theme.verticalList()).expandX().widget();
   }

   @Override
   public <W extends WWidget> Cell<W> add(W widget) {
      return this.list.add(widget);
   }

   @Override
   public void initWidgets() {
      this.hasAnimal = this.hasWaterAnimal = this.hasMonster = this.hasAmbient = this.hasMisc = 0;

      for (EntityType<?> entityType : this.setting.get()) {
         if (this.setting.filter == null || this.setting.filter.test(entityType)) {
            switch (entityType.getCategory()) {
               case CREATURE:
                  this.hasAnimal++;
                  break;
               case WATER_AMBIENT:
               case WATER_CREATURE:
               case UNDERGROUND_WATER_CREATURE:
               case AXOLOTLS:
                  this.hasWaterAnimal++;
                  break;
               case MONSTER:
                  this.hasMonster++;
                  break;
               case AMBIENT:
                  this.hasAmbient++;
                  break;
               case MISC:
                  this.hasMisc++;
            }
         }
      }

      boolean first = this.animals == null;
      List<EntityType<?>> animalsE = new ArrayList<>();
      WCheckbox animalsC = this.theme.checkbox(this.hasAnimal > 0);
      this.animals = this.theme.section("Animals", this.animals != null && this.animals.isExpanded(), animalsC);
      animalsC.action = () -> this.tableChecked(animalsE, animalsC.checked);
      Cell<WSection> animalsCell = this.add(this.animals).expandX();
      this.animalsT = this.animals.add(this.theme.table()).expandX().widget();
      List<EntityType<?>> waterAnimalsE = new ArrayList<>();
      WCheckbox waterAnimalsC = this.theme.checkbox(this.hasWaterAnimal > 0);
      this.waterAnimals = this.theme.section("Water Animals", this.waterAnimals != null && this.waterAnimals.isExpanded(), waterAnimalsC);
      waterAnimalsC.action = () -> this.tableChecked(waterAnimalsE, waterAnimalsC.checked);
      Cell<WSection> waterAnimalsCell = this.add(this.waterAnimals).expandX();
      this.waterAnimalsT = this.waterAnimals.add(this.theme.table()).expandX().widget();
      List<EntityType<?>> monstersE = new ArrayList<>();
      WCheckbox monstersC = this.theme.checkbox(this.hasMonster > 0);
      this.monsters = this.theme.section("Monsters", this.monsters != null && this.monsters.isExpanded(), monstersC);
      monstersC.action = () -> this.tableChecked(monstersE, monstersC.checked);
      Cell<WSection> monstersCell = this.add(this.monsters).expandX();
      this.monstersT = this.monsters.add(this.theme.table()).expandX().widget();
      List<EntityType<?>> ambientE = new ArrayList<>();
      WCheckbox ambientC = this.theme.checkbox(this.hasAmbient > 0);
      this.ambient = this.theme.section("Ambient", this.ambient != null && this.ambient.isExpanded(), ambientC);
      ambientC.action = () -> this.tableChecked(ambientE, ambientC.checked);
      Cell<WSection> ambientCell = this.add(this.ambient).expandX();
      this.ambientT = this.ambient.add(this.theme.table()).expandX().widget();
      List<EntityType<?>> miscE = new ArrayList<>();
      WCheckbox miscC = this.theme.checkbox(this.hasMisc > 0);
      this.misc = this.theme.section("Misc", this.misc != null && this.misc.isExpanded(), miscC);
      miscC.action = () -> this.tableChecked(miscE, miscC.checked);
      Cell<WSection> miscCell = this.add(this.misc).expandX();
      this.miscT = this.misc.add(this.theme.table()).expandX().widget();
      List<Item> spawnEggItems = BuiltInRegistries.ITEM.stream().filter(item -> item.components().has(DataComponents.ENTITY_DATA)).toList();
      Consumer<EntityType<?>> entityTypeForEach = entityType -> {
         if (this.setting.filter == null || this.setting.filter.test(entityType)) {
            switch (entityType.getCategory()) {
               case CREATURE:
                  animalsE.add(entityType);
                  this.addEntityType(this.animalsT, animalsC, entityType, spawnEggItems);
                  break;
               case WATER_AMBIENT:
               case WATER_CREATURE:
               case UNDERGROUND_WATER_CREATURE:
               case AXOLOTLS:
                  waterAnimalsE.add(entityType);
                  this.addEntityType(this.waterAnimalsT, waterAnimalsC, entityType, spawnEggItems);
                  break;
               case MONSTER:
                  monstersE.add(entityType);
                  this.addEntityType(this.monstersT, monstersC, entityType, spawnEggItems);
                  break;
               case AMBIENT:
                  ambientE.add(entityType);
                  this.addEntityType(this.ambientT, ambientC, entityType, spawnEggItems);
                  break;
               case MISC:
                  miscE.add(entityType);
                  this.addEntityType(this.miscT, miscC, entityType, spawnEggItems);
            }
         }
      };
      if (this.filterText.isEmpty()) {
         BuiltInRegistries.ENTITY_TYPE.forEach(entityTypeForEach);
      } else {
         List<Tuple<EntityType<?>, Integer>> entities = new ArrayList<>();
         BuiltInRegistries.ENTITY_TYPE.forEach(entity -> {
            int words = Utils.searchInWords(Names.get((EntityType<?>)entity), this.filterText);
            int diff = Utils.searchLevenshteinDefault(Names.get((EntityType<?>)entity), this.filterText, false);
            if (words > 0 || diff < Names.get((EntityType<?>)entity).length() / 2) {
               entities.add(new Tuple<>((EntityType<?>)entity, -diff));
            }
         });
         entities.sort(Comparator.comparingInt(value -> -value.getB()));

         for (Tuple<EntityType<?>, Integer> pair : entities) {
            entityTypeForEach.accept(pair.getA());
         }
      }

      if (this.animalsT.cells.isEmpty()) {
         this.list.cells.remove(animalsCell);
      }

      if (this.waterAnimalsT.cells.isEmpty()) {
         this.list.cells.remove(waterAnimalsCell);
      }

      if (this.monstersT.cells.isEmpty()) {
         this.list.cells.remove(monstersCell);
      }

      if (this.ambientT.cells.isEmpty()) {
         this.list.cells.remove(ambientCell);
      }

      if (this.miscT.cells.isEmpty()) {
         this.list.cells.remove(miscCell);
      }

      if (first) {
         int totalCount = (
               this.hasWaterAnimal + this.waterAnimals.cells.size() + this.monsters.cells.size() + this.ambient.cells.size() + this.misc.cells.size()
            )
            / 2;
         if (totalCount <= 20) {
            if (!this.animalsT.cells.isEmpty()) {
               this.animals.setExpanded(true);
            }

            if (!this.waterAnimalsT.cells.isEmpty()) {
               this.waterAnimals.setExpanded(true);
            }

            if (!this.monstersT.cells.isEmpty()) {
               this.monsters.setExpanded(true);
            }

            if (!this.ambientT.cells.isEmpty()) {
               this.ambient.setExpanded(true);
            }

            if (!this.miscT.cells.isEmpty()) {
               this.misc.setExpanded(true);
            }
         } else {
            if (!this.animalsT.cells.isEmpty()) {
               this.animals.setExpanded(false);
            }

            if (!this.waterAnimalsT.cells.isEmpty()) {
               this.waterAnimals.setExpanded(false);
            }

            if (!this.monstersT.cells.isEmpty()) {
               this.monsters.setExpanded(false);
            }

            if (!this.ambientT.cells.isEmpty()) {
               this.ambient.setExpanded(false);
            }

            if (!this.miscT.cells.isEmpty()) {
               this.misc.setExpanded(false);
            }
         }
      }
   }

   private void tableChecked(List<EntityType<?>> entityTypes, boolean checked) {
      boolean changed = false;

      for (EntityType<?> entityType : entityTypes) {
         if (checked) {
            this.setting.get().add(entityType);
            changed = true;
         } else if (this.setting.get().remove(entityType)) {
            changed = true;
         }
      }

      if (changed) {
         this.list.clear();
         this.initWidgets();
         this.setting.onChanged();
      }
   }

   private void addEntityType(WTable table, WCheckbox tableCheckbox, EntityType<?> entityType, List<Item> spawnEggItems) {
      ItemStack stack = null;

      for (Item item : spawnEggItems) {
         TypedEntityData<EntityType<?>> component = item.components().get(DataComponents.ENTITY_DATA);
         if (component.type() == entityType) {
            stack = DisplayItemUtils.toStack(item);
            break;
         }
      }

      if (stack != null) {
         table.add(this.theme.item(stack));
      } else {
         if (EMPTY_SPAWN_EGG_TEXTURE == null) {
            EMPTY_SPAWN_EGG_TEXTURE = Texture.readResource("/assets/meteor-client/textures/empty_spawn_egg.png", false, FilterMode.NEAREST);
         }

         table.add(this.theme.texture(32.0, 32.0, 0.0, EMPTY_SPAWN_EGG_TEXTURE));
      }

      table.add(this.theme.label(Names.get(entityType)));
      WCheckbox a = table.add(this.theme.checkbox(this.setting.get().contains(entityType))).expandCellX().right().widget();
      a.action = () -> {
         if (a.checked) {
            this.setting.get().add(entityType);
            switch (entityType.getCategory()) {
               case CREATURE:
                  if (this.hasAnimal == 0) {
                     tableCheckbox.checked = true;
                  }

                  this.hasAnimal++;
                  break;
               case WATER_AMBIENT:
               case WATER_CREATURE:
               case UNDERGROUND_WATER_CREATURE:
               case AXOLOTLS:
                  if (this.hasWaterAnimal == 0) {
                     tableCheckbox.checked = true;
                  }

                  this.hasWaterAnimal++;
                  break;
               case MONSTER:
                  if (this.hasMonster == 0) {
                     tableCheckbox.checked = true;
                  }

                  this.hasMonster++;
                  break;
               case AMBIENT:
                  if (this.hasAmbient == 0) {
                     tableCheckbox.checked = true;
                  }

                  this.hasAmbient++;
                  break;
               case MISC:
                  if (this.hasMisc == 0) {
                     tableCheckbox.checked = true;
                  }

                  this.hasMisc++;
            }
         } else if (this.setting.get().remove(entityType)) {
            switch (entityType.getCategory()) {
               case CREATURE:
                  this.hasAnimal--;
                  if (this.hasAnimal == 0) {
                     tableCheckbox.checked = false;
                  }
                  break;
               case WATER_AMBIENT:
               case WATER_CREATURE:
               case UNDERGROUND_WATER_CREATURE:
               case AXOLOTLS:
                  this.hasWaterAnimal--;
                  if (this.hasWaterAnimal == 0) {
                     tableCheckbox.checked = false;
                  }
                  break;
               case MONSTER:
                  this.hasMonster--;
                  if (this.hasMonster == 0) {
                     tableCheckbox.checked = false;
                  }
                  break;
               case AMBIENT:
                  this.hasAmbient--;
                  if (this.hasAmbient == 0) {
                     tableCheckbox.checked = false;
                  }
                  break;
               case MISC:
                  this.hasMisc--;
                  if (this.hasMisc == 0) {
                     tableCheckbox.checked = false;
                  }
            }
         }

         this.setting.onChanged();
      };
      table.row();
   }
}
