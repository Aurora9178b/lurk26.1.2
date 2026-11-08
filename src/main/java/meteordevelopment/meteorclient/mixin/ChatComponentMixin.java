package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.mixininterface.IChatListener;
import meteordevelopment.meteorclient.mixininterface.IGuiMessage;
import meteordevelopment.meteorclient.mixininterface.IGuiMessageVisible;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.ClientSetting;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.ChatBackgroundRenderer;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.DisplayMode;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements IChatHud {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private List<Line> trimmedMessages;
   @Shadow
   @Final
   private List<GuiMessage> allMessages;
   @Unique
   private BetterChat betterChat;
   @Unique
   private int nextId;
   @Unique
   private float meteor$animatedChatHeight;
   @Unique
   private boolean meteor$chatHeightInitialized;

   @Shadow
   public abstract void addClientSystemMessage(Component var1);

   @Shadow
   public abstract int getLinesPerPage();

   @Shadow
   public abstract void refreshTrimmedMessages();

   @Inject(
      method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
      at = @At("HEAD")
   )
   private void onExtractRenderStateBegin(
      GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY, DisplayMode displayMode, boolean insertionClickMode, CallbackInfo ci
   ) {
      BlurUtil.beginFrame(graphics);
      this.meteor$requestChatBackground(graphics.guiHeight(), ticks, displayMode);
   }

   @Override
   public void meteor$add(Component message, int id) {
      this.nextId = id;
      this.addClientSystemMessage(message);
      this.nextId = 0;
   }

   @Override
   public void meteor$delete(Component message) {
      this.allMessages.removeIf(guiMessage -> guiMessage.content() == message);
      this.refreshTrimmedMessages();
   }

   @Inject(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", shift = Shift.AFTER))
   private void onAddMessageAfterNewGuiMessageVisible(GuiMessage message, CallbackInfo ci) {
      ((IGuiMessage)(Object)this.trimmedMessages.getFirst()).meteor$setId(this.nextId);
   }

   @Inject(method = "addMessageToQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", shift = Shift.AFTER))
   private void onAddMessageAfterNewGuiMessage(GuiMessage message, CallbackInfo ci) {
      ((IGuiMessage)(Object)this.allMessages.getFirst()).meteor$setId(this.nextId);
   }

   @ModifyExpressionValue(
      method = "addMessageToDisplayQueue",
      at = @At(
         value = "NEW",
         target = "(Lnet/minecraft/client/multiplayer/chat/GuiMessage;Lnet/minecraft/util/FormattedCharSequence;Z)Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;"
      )
   )
   private Line onAddMessage_modifyGuiMessageLine(Line line, @Local(name = "i") int i) {
      IChatListener handler = (IChatListener)(Object)this.minecraft.getChatListener();
      if (handler == null) {
         return line;
      }

      IGuiMessageVisible meteorLine = (IGuiMessageVisible)(Object)line;
      meteorLine.meteor$setSender(handler.meteor$getSender());
      meteorLine.meteor$setStartOfEntry(i == 0);
      return line;
   }

   @ModifyExpressionValue(
      method = "addMessage",
      at = @At(
         value = "NEW",
         target = "(ILnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)Lnet/minecraft/client/multiplayer/chat/GuiMessage;"
      )
   )
   private GuiMessage onAddMessage_modifyGuiMessage(GuiMessage line) {
      IChatListener handler = (IChatListener)(Object)this.minecraft.getChatListener();
      if (handler == null) {
         return line;
      }

      ((IGuiMessage)(Object)line).meteor$setSender(handler.meteor$getSender());
      return line;
   }

   @Inject(at = @At("HEAD"), method = "addMessage", cancellable = true)
   private void onAddMessage(
      Component message,
      MessageSignature signature,
      GuiMessageSource source,
      GuiMessageTag indicator,
      CallbackInfo ci,
      @Local(argsOnly = true, name = "contents") LocalRef<Component> contents,
      @Local(argsOnly = true, name = "tag") LocalRef<GuiMessageTag> tag
   ) {
      ReceiveMessageEvent event = (ReceiveMessageEvent)MeteorClient.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, this.nextId));
      if (event.isCancelled()) {
         ci.cancel();
      } else {
         this.trimmedMessages.removeIf(msg -> ((IGuiMessage)(Object)msg).meteor$getId() == this.nextId && this.nextId != 0);

         for (int i = this.allMessages.size() - 1; i > -1; i--) {
            if (((IGuiMessage)(Object)this.allMessages.get(i)).meteor$getId() == this.nextId && this.nextId != 0) {
               this.allMessages.remove(i);
               this.getBetterChat().removeLine(i);
            }
         }

         if (event.isModified()) {
            contents.set(event.getMessage());
            tag.set(event.getIndicator());
         }
      }
   }

   @ModifyExpressionValue(method = "addMessageToQueue", at = @At(value = "CONSTANT", args = "intValue=100"))
   private int maxLength(int size) {
      return Modules.get() != null && this.getBetterChat().isLongerChat() ? size + this.betterChat.getExtraChatLines() : size;
   }

   @ModifyExpressionValue(method = "addMessageToDisplayQueue", at = @At(value = "CONSTANT", args = "intValue=100"))
   private int maxLengthVisible(int size) {
      return Modules.get() != null && this.getBetterChat().isLongerChat() ? size + this.betterChat.getExtraChatLines() : size;
   }

   @ModifyExpressionValue(
      method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;ceil(F)I")
   )
   private int onRender_modifyWidth(int width) {
      return this.getBetterChat().modifyChatWidth(width);
   }

   @Inject(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;isChatFocused()Z"))
   private void onBreakChatMessageLines(GuiMessage message, CallbackInfo ci, @Local(name = "lines") List<FormattedCharSequence> lines) {
      if (Modules.get() != null) {
         this.getBetterChat().lines.addFirst(lines.size());
      }
   }

   @Inject(method = "addMessageToQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;removeLast()Ljava/lang/Object;"))
   private void onRemoveMessage(GuiMessage message, CallbackInfo ci) {
      if (Modules.get() != null) {
         int extra = this.getBetterChat().isLongerChat() ? this.getBetterChat().getExtraChatLines() : 0;

         for (int size = this.betterChat.lines.size(); size > 100 + extra; size--) {
            this.betterChat.lines.removeLast();
         }
      }
   }

   @Inject(method = "clearMessages", at = @At("HEAD"))
   private void onClearMessages(boolean history, CallbackInfo ci) {
      this.getBetterChat().lines.clear();
      this.meteor$resetChatHeightAnimation();
   }

   @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
   private void onRefreshTrimmedMessages(CallbackInfo ci) {
      this.getBetterChat().lines.clear();
      this.meteor$resetChatHeightAnimation();
   }

   @Unique
   private BetterChat getBetterChat() {
      if (this.betterChat == null) {
         this.betterChat = Modules.get().get(BetterChat.class);
      }

      return this.betterChat;
   }

   @Unique
   private void meteor$requestChatBackground(int guiHeight, int ticks, DisplayMode displayMode) {
      float alpha = this.meteor$getBackgroundAlpha(displayMode.foreground, ticks);
      int lineCount = this.meteor$getVisibleLineCount(displayMode.foreground, ticks);
      if (lineCount > 0 || displayMode.showRestrictedPrompt) {
         double chatScale = this.minecraft.options.chatScale().get();
         if (!(chatScale <= 0.0)) {
            double lineSpacing = this.minecraft.options.chatLineSpacing().get();
            int lineHeight = (int)(9.0 * (lineSpacing + 1.0));
            int rows = lineCount + (displayMode.showRestrictedPrompt ? 1 : 0);
            int visibleHeight = Math.max(lineHeight, rows * lineHeight);
            float animatedHeight = this.meteor$getAnimatedChatHeight(visibleHeight);
            int bottom = (int)Math.floor((guiHeight - 40) / chatScale);
            float top = bottom - animatedHeight;
            int width = (int)Math.ceil(ChatComponent.getWidth(this.minecraft.options.chatWidth().get()) / chatScale) + 16;
            SettingColor color = ClientSetting.getChatBackgroundColor();
            int fadedAlpha = displayMode.foreground ? color.a : Math.round(color.a * alpha);
            if (fadedAlpha > 0) {
               ChatBackgroundRenderer.request(
                  0.0,
                  Math.max(0.0F, top) * chatScale,
                  width * chatScale,
                  animatedHeight * chatScale,
                  5.0 * chatScale,
                  new Color(color.r, color.g, color.b, fadedAlpha),
                  displayMode.foreground ? 1.0F : alpha
               );
            }
         }
      }
   }

   @Unique
   private float meteor$getAnimatedChatHeight(float targetHeight) {
      if (!this.meteor$chatHeightInitialized) {
         this.meteor$animatedChatHeight = targetHeight;
         this.meteor$chatHeightInitialized = true;
         return this.meteor$animatedChatHeight;
      }

      this.meteor$animatedChatHeight = AnimationUtility.ease(this.meteor$animatedChatHeight, targetHeight, 8.0F);
      if (Math.abs(this.meteor$animatedChatHeight - targetHeight) < 0.25F) {
         this.meteor$animatedChatHeight = targetHeight;
      }

      return Math.max(1.0F, this.meteor$animatedChatHeight);
   }

   @Unique
   private void meteor$resetChatHeightAnimation() {
      this.meteor$animatedChatHeight = 0.0F;
      this.meteor$chatHeightInitialized = false;
   }

   @Unique
   private int meteor$getVisibleLineCount(boolean focused, int ticks) {
      int linesPerPage = this.getLinesPerPage();
      int available = Math.min(this.trimmedMessages.size(), linesPerPage);
      int visible = 0;

      for (int i = available - 1; i >= 0; i--) {
         Line line = this.trimmedMessages.get(i);
         if (focused || this.meteor$getLineAlpha(ticks, line) > 1.0E-5F) {
            visible++;
         }
      }

      return visible;
   }

   @Unique
   private float meteor$getLineAlpha(int ticks, Line line) {
      double progress = 1.0 - (ticks - line.addedTime()) / 200.0;
      progress = Math.max(0.0, Math.min(1.0, progress * 10.0));
      return (float)(progress * progress);
   }

   @Unique
   private float meteor$getBackgroundAlpha(boolean focused, int ticks) {
      if (focused) {
         return 1.0F;
      }

      int linesPerPage = this.getLinesPerPage();
      int available = Math.min(this.trimmedMessages.size(), linesPerPage);
      float alpha = 0.0F;

      for (int i = available - 1; i >= 0; i--) {
         alpha = Math.max(alpha, this.meteor$getLineAlpha(ticks, this.trimmedMessages.get(i)));
      }

      return alpha;
   }
}
