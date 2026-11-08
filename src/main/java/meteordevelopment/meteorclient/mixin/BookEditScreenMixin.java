package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
   @Shadow
   @Final
   private List<String> pages;
   @Shadow
   private int currentPage;

   @Shadow
   protected abstract void updatePageContent();

   @Shadow
   protected abstract void pageForward();

   @Shadow
   protected abstract void pageBack();

   public BookEditScreenMixin(Component title) {
      super(title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      this.addRenderableWidget(new Builder(Component.literal("Copy"), var1x -> {
         ListTag listTag = new ListTag();
         this.pages.stream().map(StringTag::valueOf).forEach(listTag::add);
         CompoundTag tag = new CompoundTag();
         tag.put("pages", listTag);
         tag.putInt("currentPage", this.currentPage);
         FastByteArrayOutputStream bytes = new FastByteArrayOutputStream();
         DataOutputStream out = new DataOutputStream(bytes);

         try {
            NbtIo.write(tag, out);
         } catch (IOException e) {
            MeteorClient.LOG.error("Error writing the book to the output stream", e);
         }

         try {
            GLFW.glfwSetClipboardString(MeteorClient.mc.getWindow().handle(), Base64.getEncoder().encodeToString(bytes.array));
         } catch (OutOfMemoryError exception) {
            GLFW.glfwSetClipboardString(MeteorClient.mc.getWindow().handle(), exception.toString());
         }
      }).pos(4, 4).size(120, 20).build());
      this.addRenderableWidget(new Builder(Component.literal("Paste"), var1x -> {
         String clipboard = GLFW.glfwGetClipboardString(MeteorClient.mc.getWindow().handle());
         if (clipboard != null) {
            byte[] bytes;
            try {
               bytes = Base64.getDecoder().decode(clipboard);
            } catch (IllegalArgumentException var8) {
               return;
            }

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

            try {
               CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
               ListTag listTag = tag.getListOrEmpty("pages").copy();
               this.pages.clear();

               for (int i = 0; i < listTag.size(); i++) {
                  this.pages.add(listTag.getStringOr(i, ""));
               }

               if (this.pages.isEmpty()) {
                  this.pages.add("");
               }

               this.currentPage = tag.getIntOr("currentPage", 0);
               this.updatePageContent();
            } catch (IOException e) {
               MeteorClient.LOG.error("Error reading the data from your clipboard", e);
            }
         }
      }).pos(4, 26).size(120, 20).build());
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount == 0.0) {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }

      if (verticalAmount < 0.0) {
         this.pageForward();
      } else {
         this.pageBack();
      }

      return true;
   }
}
