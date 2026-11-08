package meteordevelopment.meteorclient.gui.screens.settings.base;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.IdentifierException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public abstract class DynamicRegistryListSettingScreen<T> extends CollectionListSettingScreen<ResourceKey<T>> {
   protected final ResourceKey<Registry<T>> registryKey;

   public DynamicRegistryListSettingScreen(
      GuiTheme theme, String title, Setting<?> setting, Collection<ResourceKey<T>> collection, ResourceKey<Registry<T>> registryKey
   ) {
      super(theme, title, setting, collection, createUniverse(collection, registryKey));
      this.registryKey = registryKey;
   }

   private static <T> Iterable<ResourceKey<T>> createUniverse(Collection<ResourceKey<T>> collection, ResourceKey<Registry<T>> registryKey) {
      Set<ResourceKey<T>> set = new ReferenceOpenHashSet<>(collection);
      HolderLookup.Provider access = Minecraft.getInstance().getConnection() != null
         ? (HolderLookup.Provider)Minecraft.getInstance().getConnection().registryAccess()
         : VanillaRegistries.createLookup();
      access
         .lookup(registryKey)
         .ifPresent(registry -> registry.listElementIds().forEach(set::add));
      return set;
   }

   @Override
   protected void postWidgets(WTable left, WTable right) {
      if (!left.cells.isEmpty()) {
         left.add(this.theme.horizontalSeparator()).expandX();
         left.row();
      }

      WHorizontalList manualEntry = left.add(this.theme.horizontalList()).expandX().widget();
      WTextBox textBox = manualEntry.add(this.theme.textBox("minecraft:")).expandX().minWidth(120.0).widget();
      manualEntry.add(this.theme.plus()).expandCellX().right().widget().action = () -> {
         String entry = textBox.get().trim();

         try {
            Identifier id = entry.contains(":") ? Identifier.parse(entry) : Identifier.withDefaultNamespace(entry);
            this.addValue(ResourceKey.create(this.registryKey, id));
         } catch (IdentifierException var4x) {
         }
      };
   }
}
