package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.world.item.Items;

public class Categories {
   public static final Category Combat = new Category("Combat", () -> DisplayItemUtils.toStack(Items.GOLDEN_SWORD));
   public static final Category Player = new Category("Player", () -> DisplayItemUtils.toStack(Items.ARMOR_STAND));
   public static final Category Movement = new Category("Movement", () -> DisplayItemUtils.toStack(Items.DIAMOND_BOOTS));
   public static final Category Render = new Category("Render", () -> DisplayItemUtils.toStack(Items.GLASS));
   public static final Category World = new Category("World", () -> DisplayItemUtils.toStack(Items.GRASS_BLOCK));
   public static final Category Exploit = new Category("Exploit", () -> DisplayItemUtils.toStack(Items.COMMAND_BLOCK));
   public static final Category Misc = new Category("Misc", () -> DisplayItemUtils.toStack(Items.LAVA_BUCKET));
   public static final Category Client = new Category("Client", () -> DisplayItemUtils.toStack(Items.LAVA_BUCKET));
   public static boolean REGISTERING;

   public static void init() {
      REGISTERING = true;
      Modules.registerCategory(Combat);
      Modules.registerCategory(Player);
      Modules.registerCategory(Movement);
      Modules.registerCategory(Render);
      Modules.registerCategory(World);
      Modules.registerCategory(Exploit);
      Modules.registerCategory(Misc);
      Modules.registerCategory(Client);
      AddonManager.ADDONS.forEach(MeteorAddon::onRegisterCategories);
      REGISTERING = false;
   }
}
