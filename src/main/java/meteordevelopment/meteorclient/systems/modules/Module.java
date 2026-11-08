package meteordevelopment.meteorclient.systems.modules;

import java.util.Objects;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public abstract class Module implements ISerializable<Module>, Comparable<Module> {
   protected final Minecraft mc;
   public final Category category;
   public final String name;
   public final String title;
   public final String description;
   public final String[] aliases;
   public final Color color;
   public final MeteorAddon addon;
   public final Settings settings = new Settings();
   private boolean active;
   public boolean serialize = true;
   public boolean runInMainMenu = false;
   public boolean autoSubscribe = true;
   public final Keybind keybind = Keybind.none();
   public boolean toggleOnBindRelease = false;
   public boolean chatFeedback = true;
   public boolean favorite = false;

   public Module(Category category, String name, String description, String... aliases) {
      if (name.contains(" ")) {
         MeteorClient.LOG.warn("Module '{}' contains invalid characters in its name making it incompatible with Meteor Client commands.", name);
      }

      this.mc = Minecraft.getInstance();
      this.category = category;
      this.name = name;
      this.title = Utils.nameToTitle(name);
      this.description = description;
      this.aliases = aliases;
      this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1.0);
      String classname = this.getClass().getName();

      for (MeteorAddon addon : AddonManager.ADDONS) {
         if (classname.startsWith(addon.getPackage())) {
            this.addon = addon;
            return;
         }
      }

      this.addon = null;
   }

   public WWidget getWidget(GuiTheme theme) {
      return null;
   }

   public void onActivate() {
   }

   public void onDeactivate() {
   }

   public void toggle() {
      if (!this.active) {
         this.active = true;
         Modules.get().addActive(this);
         this.settings.onActivated();
         if (this.runInMainMenu || Utils.canUpdate()) {
            if (this.autoSubscribe) {
               MeteorClient.EVENT_BUS.subscribe(this);
            }

            this.onActivate();
         }
      } else {
         if (this.runInMainMenu || Utils.canUpdate()) {
            if (this.autoSubscribe) {
               MeteorClient.EVENT_BUS.unsubscribe(this);
            }

            this.onDeactivate();
         }

         this.active = false;
         Modules.get().removeActive(this);
      }
   }

   public void enable() {
      if (!this.isActive()) {
         this.toggle();
      }
   }

   public void disable() {
      if (this.isActive()) {
         this.toggle();
      }
   }

   public void sendToggledMsg() {
      if (Config.get().chatFeedback.get() && this.chatFeedback) {
         ChatUtils.forceNextPrefixClass(this.getClass());
         ChatUtils.sendMsg(this.hashCode(), ChatFormatting.GRAY, (this.isActive() ? "§2[§a+§2]§f " : "§4[§c-§4]§f ") + this.title);
      }
   }

   public void info(Component message) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(message);
   }

   public void info(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.info(message, args);
   }

   public void warning(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.warningPrefix(this.title, message, args);
   }

   public void error(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.errorPrefix(this.title, message, args);
   }

   public boolean isActive() {
      return this.active;
   }

   public String getInfoString() {
      return null;
   }

   @Override
   public CompoundTag toTag() {
      if (!this.serialize) {
         return null;
      }

      CompoundTag tag = new CompoundTag();
      tag.putString("name", this.name);
      tag.put("keybind", this.keybind.toTag());
      tag.putBoolean("toggleOnKeyRelease", this.toggleOnBindRelease);
      tag.putBoolean("chatFeedback", this.chatFeedback);
      tag.putBoolean("favorite", this.favorite);
      tag.put("settings", this.settings.toTag());
      tag.putBoolean("active", this.active);
      return tag;
   }

   public Module fromTag(CompoundTag tag) {
      this.keybind.fromTag(tag.getCompoundOrEmpty("keybind"));
      this.toggleOnBindRelease = tag.getBooleanOr("toggleOnKeyRelease", false);
      this.chatFeedback = !tag.contains("chatFeedback") || tag.getBooleanOr("chatFeedback", false);
      this.favorite = tag.getBooleanOr("favorite", false);
      if (tag.get("settings") instanceof CompoundTag compoundTag) {
         this.settings.fromTag(compoundTag);
      }

      boolean active = tag.getBooleanOr("active", false);
      if (active != this.isActive()) {
         this.toggle();
      }

      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Module module = (Module)o;
         return Objects.equals(this.name, module.name);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.name);
   }

   public int compareTo(@NotNull Module o) {
      return this.name.compareTo(o.name);
   }
}
