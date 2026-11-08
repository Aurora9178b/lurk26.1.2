package meteordevelopment.meteorclient.gui.screens;

import java.util.Optional;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;

public class ModuleScreen extends WindowScreen {
   private final Module module;
   private WContainer settingsContainer;
   private WKeybind keybind;
   private WCheckbox active;

   public ModuleScreen(GuiTheme theme, Module module) {
      super(theme, theme.favorite(module.favorite), module.title);
      ((WFavorite)this.window.icon).action = () -> module.favorite = ((WFavorite)this.window.icon).checked;
      this.module = module;
   }

   @Override
   public void initWidgets() {
      this.add(this.theme.label(this.module.description, Utils.getWindowWidth() / 2.0));
      if (this.module.addon != null && this.module.addon != MeteorClient.ADDON) {
         WHorizontalList addon = this.add(this.theme.horizontalList()).expandX().widget();
         addon.add(this.theme.label("From: ").color(this.theme.textSecondaryColor())).widget();
         addon.add(this.theme.label(this.module.addon.name).color(this.module.addon.color)).widget();
      }

      if (!this.module.settings.groups.isEmpty()) {
         this.settingsContainer = this.add(this.theme.verticalList()).expandX().widget();
         this.settingsContainer.add(this.theme.settings(this.module.settings)).expandX();
      }

      WWidget widget = this.module.getWidget(this.theme);
      if (widget != null) {
         this.add(this.theme.horizontalSeparator()).expandX();
         Cell<WWidget> cell = this.add(widget);
         if (widget instanceof WContainer) {
            cell.expandX();
         }
      }

      WSection section = this.add(this.theme.section("Bind", true)).expandX().widget();
      WHorizontalList bind = section.add(this.theme.horizontalList()).expandX().widget();
      bind.add(this.theme.label("Bind: "));
      this.keybind = bind.add(this.theme.keybind(this.module.keybind)).expandX().widget();
      this.keybind.actionOnSet = () -> Modules.get().setModuleToBind(this.module);
      WButton reset = bind.add(this.theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
      reset.action = this.keybind::resetBind;
      reset.tooltip = "Reset";
      WHorizontalList tobr = section.add(this.theme.horizontalList()).widget();
      tobr.add(this.theme.label("Toggle on bind release: "));
      WCheckbox tobrC = tobr.add(this.theme.checkbox(this.module.toggleOnBindRelease)).widget();
      tobrC.action = () -> this.module.toggleOnBindRelease = tobrC.checked;
      WHorizontalList cf = section.add(this.theme.horizontalList()).widget();
      cf.add(this.theme.label("Chat Feedback: "));
      WCheckbox cfC = cf.add(this.theme.checkbox(this.module.chatFeedback)).widget();
      cfC.action = () -> this.module.chatFeedback = cfC.checked;
      this.add(this.theme.horizontalSeparator()).expandX();
      WHorizontalList bottom = this.add(this.theme.horizontalList()).expandX().widget();
      bottom.add(this.theme.label("Active: "));
      this.active = bottom.add(this.theme.checkbox(this.module.isActive())).expandCellX().widget();
      this.active.action = () -> {
         if (this.module.isActive() != this.active.checked) {
            this.module.toggle();
         }
      };
      WHorizontalList sharing = bottom.add(this.theme.horizontalList()).right().widget();
      WButton copy = sharing.add(this.theme.button(GuiRenderer.COPY)).widget();
      copy.action = () -> {
         if (this.toClipboard()) {
            OkPrompt.create()
               .title("Module copied!")
               .message("The settings for this module are now in your clipboard.")
               .message("You can also copy settings using Ctrl+C.")
               .message("Settings can be imported using Ctrl+V or the paste button.")
               .id("config-sharing-guide")
               .show();
         }
      };
      copy.tooltip = "Copy config";
      WButton paste = sharing.add(this.theme.button(GuiRenderer.PASTE)).widget();
      paste.action = this::fromClipboard;
      paste.tooltip = "Paste config";
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return !Modules.get().isBinding();
   }

   @Override
   public void tick() {
      super.tick();
      this.module.settings.tick(this.settingsContainer, this.theme);
   }

   @EventHandler
   private void onModuleBindChanged(ModuleBindChangedEvent event) {
      this.keybind.reset();
   }

   @EventHandler
   private void onActiveModulesChanged(ActiveModulesChangedEvent event) {
      this.active.checked = this.module.isActive();
   }

   @Override
   public boolean toClipboard() {
      CompoundTag tag = new CompoundTag();
      tag.putString("name", this.module.name);
      CompoundTag settingsTag = this.module.settings.toTag();
      if (!settingsTag.isEmpty()) {
         tag.put("settings", settingsTag);
      }

      return NbtUtils.toClipboard(tag);
   }

   @Override
   public boolean fromClipboard() {
      CompoundTag tag = NbtUtils.fromClipboard();
      if (tag == null) {
         return false;
      }

      if (!tag.getStringOr("name", "").equals(this.module.name)) {
         return false;
      }

      Optional<CompoundTag> settings = tag.getCompound("settings");
      if (settings.isPresent()) {
         this.module.settings.fromTag(settings.get());
      } else {
         this.module.settings.reset();
      }

      if (this.parent instanceof WidgetScreen p) {
         p.reload();
      }

      this.reload();
      return true;
   }
}
