package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.mixin.MapTextureManagerAccessor;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.resources.MapTextureManager.MapInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class SaveMapCommand extends Command {
   private static final SimpleCommandExceptionType MAP_NOT_FOUND = new SimpleCommandExceptionType(Component.literal("You must be holding a filled map."));
   private static final SimpleCommandExceptionType OOPS = new SimpleCommandExceptionType(Component.literal("Something went wrong."));
   private final PointerBuffer filters = BufferUtils.createPointerBuffer(1);

   public SaveMapCommand() {
      super("save-map", "Saves a map to an image.", "sm");
      ByteBuffer pngFilter = MemoryUtil.memASCII("*.png");
      this.filters.put(pngFilter);
      this.filters.rewind();
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.executes(var1x -> {
         this.saveMap(128);
         return 1;
      }).then(argument("scale", IntegerArgumentType.integer(1)).executes(context -> {
         this.saveMap(IntegerArgumentType.getInteger(context, "scale"));
         return 1;
      }));
   }

   private void saveMap(int scale) throws CommandSyntaxException {
      ItemStack map = this.getMap();
      MapItemSavedData state = this.getMapState();
      if (map != null && state != null) {
         File path = this.getPath();
         if (path == null) {
            throw OOPS.create();
         }

         MapTextureManagerAccessor textureManager = (MapTextureManagerAccessor)mc.gameRenderer.getMinecraft().getMapTextureManager();
         MapInstance texture = textureManager.meteor$invokeGetOrCreateMapInstance(map.get(DataComponents.MAP_ID), state);
         if (texture.texture.getPixels() == null) {
            throw OOPS.create();
         }

         try {
            if (scale == 128) {
               texture.texture.getPixels().writeToFile(path);
            } else {
               int[] data = texture.texture.getPixels().makePixelArray();
               BufferedImage image = new BufferedImage(128, 128, 2);
               image.setRGB(0, 0, image.getWidth(), image.getHeight(), data, 0, 128);
               BufferedImage scaledImage = new BufferedImage(scale, scale, 2);
               scaledImage.createGraphics().drawImage(image, 0, 0, scale, scale, null);
               ImageIO.write(scaledImage, "png", path);
            }
         } catch (IOException e) {
            this.error("Error writing map texture");
            MeteorClient.LOG.error(e.toString());
         }
      } else {
         throw MAP_NOT_FOUND.create();
      }
   }

   @Nullable
   private MapItemSavedData getMapState() {
      ItemStack map = this.getMap();
      return map == null ? null : MapItem.getSavedData(map.get(DataComponents.MAP_ID), mc.level);
   }

   @Nullable
   private File getPath() {
      String path = TinyFileDialogs.tinyfd_saveFileDialog("Save image", null, this.filters, null);
      if (path == null) {
         return null;
      }

      if (!path.endsWith(".png")) {
         path = path + ".png";
      }

      return new File(path);
   }

   @Nullable
   private ItemStack getMap() {
      ItemStack itemStack = mc.player.getMainHandItem();
      if (itemStack.getItem() == Items.FILLED_MAP) {
         return itemStack;
      }

      itemStack = mc.player.getOffhandItem();
      return itemStack.getItem() == Items.FILLED_MAP ? itemStack : null;
   }
}
