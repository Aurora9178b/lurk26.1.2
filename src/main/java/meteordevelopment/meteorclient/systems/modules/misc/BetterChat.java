package meteordevelopment.meteorclient.systems.modules.misc;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.mixin.ChatComponentAccessor;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.mixininterface.IGuiMessage;
import meteordevelopment.meteorclient.mixininterface.IGuiMessageVisible;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.CopyToClipboard;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.UnknownNullability;

public class BetterChat extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgFilter = this.settings.createGroup("Filter");
   private final SettingGroup sgLongerChat = this.settings.createGroup("Longer Chat");
   private final SettingGroup sgPrefix = this.settings.createGroup("Prefix");
   private final SettingGroup sgSuffix = this.settings.createGroup("Suffix");
   private final Setting<Boolean> annoy = this.sgGeneral
      .add(new BoolSetting.Builder().name("annoy").description("Makes your messages aNnOyInG.").defaultValue(false).build());
   private final Setting<Boolean> fancy = this.sgGeneral
      .add(new BoolSetting.Builder().name("fancy-chat").description("Makes your messages ғᴀɴᴄʏ!").defaultValue(false).build());
   private final Setting<Boolean> timestamps = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("timestamps")
            .description("Adds client-side time stamps to the beginning of chat messages.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> showSeconds = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("show-seconds")
            .description("Shows seconds in the chat message timestamps")
            .defaultValue(false)
            .visible(this.timestamps::get)
            .onChanged(var1x -> this.updateDateFormat())
            .build()
      );
   private final Setting<Boolean> playerHeads = this.sgGeneral
      .add(new BoolSetting.Builder().name("player-heads").description("Displays player heads next to their messages.").defaultValue(true).build());
   private final Setting<Boolean> coordsProtection = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("coords-protection")
            .description("Prevents you from sending messages in chat that may contain coordinates.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> keepHistory = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("keep-history")
            .description("Prevents the chat history from being cleared when disconnecting.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> antiSpam = this.sgFilter
      .add(new BoolSetting.Builder().name("anti-spam").description("Blocks duplicate messages from filling your chat.").defaultValue(true).build());
   private final Setting<Integer> antiSpamDepth = this.sgFilter
      .add(
         new IntSetting.Builder()
            .name("depth")
            .description("How many messages to filter.")
            .defaultValue(20)
            .min(1)
            .sliderMin(1)
            .visible(this.antiSpam::get)
            .build()
      );
   private final Setting<Boolean> antiClear = this.sgFilter
      .add(new BoolSetting.Builder().name("anti-clear").description("Prevents servers from clearing chat.").defaultValue(true).build());
   private final Setting<Boolean> filterRegex = this.sgFilter
      .add(new BoolSetting.Builder().name("filter-regex").description("Filter out chat messages that match the regex filter.").defaultValue(false).build());
   private final Setting<List<String>> regexFilters = this.sgFilter
      .add(
         new StringListSetting.Builder()
            .name("regex-filter")
            .description("Regex filter used for filtering chat messages.")
            .visible(this.filterRegex::get)
            .onChanged(var1x -> this.compileFilterRegexList())
            .build()
      );
   private final Setting<Boolean> infiniteChatBox = this.sgLongerChat
      .add(new BoolSetting.Builder().name("infinite-chat-box").description("Lets you type infinitely long messages.").defaultValue(true).build());
   private final Setting<Boolean> longerChatHistory = this.sgLongerChat
      .add(new BoolSetting.Builder().name("longer-chat-history").description("Extends chat length.").defaultValue(true).build());
   private final Setting<Integer> longerChatLines = this.sgLongerChat
      .add(
         new IntSetting.Builder()
            .name("extra-lines")
            .description("The amount of extra chat lines.")
            .defaultValue(1000)
            .min(0)
            .sliderRange(0, 1000)
            .visible(this.longerChatHistory::get)
            .build()
      );
   private final Setting<Boolean> prefix = this.sgPrefix
      .add(new BoolSetting.Builder().name("prefix").description("Adds a prefix to your chat messages.").defaultValue(false).build());
   private final Setting<Boolean> prefixRandom = this.sgPrefix
      .add(new BoolSetting.Builder().name("random").description("Uses a random number as your prefix.").defaultValue(false).build());
   private final Setting<String> prefixText = this.sgPrefix
      .add(
         new StringSetting.Builder()
            .name("text")
            .description("The text to add as your prefix.")
            .defaultValue("> ")
            .visible(() -> !this.prefixRandom.get())
            .build()
      );
   private final Setting<Boolean> prefixSmallCaps = this.sgPrefix
      .add(
         new BoolSetting.Builder()
            .name("small-caps")
            .description("Uses small caps in the prefix.")
            .defaultValue(false)
            .visible(() -> !this.prefixRandom.get())
            .build()
      );
   private final Setting<Boolean> suffix = this.sgSuffix
      .add(new BoolSetting.Builder().name("suffix").description("Adds a suffix to your chat messages.").defaultValue(false).build());
   private final Setting<Boolean> suffixRandom = this.sgSuffix
      .add(new BoolSetting.Builder().name("random").description("Uses a random number as your suffix.").defaultValue(false).build());
   private final Setting<String> suffixText = this.sgSuffix
      .add(
         new StringSetting.Builder()
            .name("text")
            .description("The text to add as your suffix.")
            .defaultValue(" | meteor on crack!")
            .visible(() -> !this.suffixRandom.get())
            .build()
      );
   private final Setting<Boolean> suffixSmallCaps = this.sgSuffix
      .add(
         new BoolSetting.Builder()
            .name("small-caps")
            .description("Uses small caps in the suffix.")
            .defaultValue(true)
            .visible(() -> !this.suffixRandom.get())
            .build()
      );
   private static final Pattern antiSpamRegex = Pattern.compile(" \\((\\d{1,9})\\)$");
   private static final Pattern antiClearRegex = Pattern.compile("\\n(\\n|\\s)+\\n");
   private static final Pattern timestampRegex = Pattern.compile("^(<\\d{2}:\\d{2}(?::\\d{2})?> )");
   private static final Pattern copyButtonRegex = Pattern.compile("^(?:\\[[cC]]\\s*)?(?:\\[x]\\s*)?");
   private static final Pattern usernameRegex = Pattern.compile("^(?:<\\d{2}:\\d{2}(?::\\d{2})?>\\s)?(?:\\[[cC]]\\s*)?(?:\\[x]\\s*)?<(.*?)>.*");
   private final Char2CharMap SMALL_CAPS = new Char2CharOpenHashMap();
   public final IntList lines = new IntArrayList();
   private static final List<BetterChat.CustomHeadEntry> CUSTOM_HEAD_ENTRIES = new ArrayList<>();
   private static final Pattern TIMESTAMP_REGEX = Pattern.compile("^<\\d{1,2}:\\d{1,2}(?::\\d{1,2})?>");
   public Line line;
   private SimpleDateFormat dateFormat;
   private final List<Pattern> filterRegexList = new ArrayList<>();
   private static final Pattern coordRegex = Pattern.compile("(?<x>-?\\d{3,}(?:\\.\\d*)?)(?:\\s+(?<y>-?\\d{1,3}(?:\\.\\d*)?))?\\s+(?<z>-?\\d{3,}(?:\\.\\d*)?)");

   public BetterChat() {
      super(Categories.Misc, "better-chat", "Improves your chat experience in various ways.");
      String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
      String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴩqʀꜱᴛᴜᴠᴡxyᴢ".split("");

      for (int i = 0; i < a.length; i++) {
         this.SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
      }

      this.compileFilterRegexList();
   }

   @EventHandler
   private void onMessageReceive(ReceiveMessageEvent event) {
      Component message = event.getMessage();
      if (this.filterRegex.get()) {
         String messageString = message.getString();

         for (Pattern pattern : this.filterRegexList) {
            if (pattern.matcher(messageString).find()) {
               event.cancel();
               return;
            }
         }
      }

      if (this.antiClear.get()) {
         String messageString = message.getString();
         if (antiClearRegex.matcher(messageString).find()) {
            MutableComponent newMessage = Component.empty();
            message.visit((style, string) -> {
               Matcher antiClearMatcher = antiClearRegex.matcher(string);
               newMessage.append(Component.literal(antiClearMatcher.find() ? antiClearMatcher.replaceAll("\n\n") : string).setStyle(style));
               return Optional.empty();
            }, Style.EMPTY);
            message = newMessage;
         }
      }

      if (this.antiSpam.get()) {
         MutableComponent antiSpammed = this.appendAntiSpam(message);
         if (antiSpammed != null) {
            message = antiSpammed;
         }
      }

      String copyText = message.getString();
      MutableComponent decoratedMessage = Component.empty();
      if (this.timestamps.get()) {
         MutableComponent timestamp = Component.literal("<" + this.dateFormat.format(new Date()) + "> ").withStyle(ChatFormatting.GRAY);
         decoratedMessage.append(timestamp);
      }

      decoratedMessage.append(this.getMessageButtons(copyText, decoratedMessage)).append(message);
      event.setMessage(decoratedMessage);
   }

   @EventHandler
   private void onMessageSend(SendMessageEvent event) {
      String message = event.message;
      if (this.annoy.get()) {
         message = this.applyAnnoy(message);
      }

      if (this.fancy.get()) {
         message = this.applyFancy(message);
      }

      message = this.getPrefix() + message + this.getSuffix();
      if (this.coordsProtection.get() && this.containsCoordinates(message)) {
         MutableComponent warningMessage = Component.literal("It looks like there are coordinates in your message! ");
         MutableComponent sendButton = this.getSendButton(message);
         warningMessage.append(sendButton);
         ChatUtils.sendMsg(warningMessage);
         event.cancel();
      } else {
         event.message = message;
      }
   }

   private MutableComponent appendAntiSpam(@UnknownNullability Component text) {
      String textString = text.getString();
      MutableComponent returnText = null;
      int messageIndex = -1;
      List<GuiMessage> messages = ((ChatComponentAccessor)this.mc.gui.getChat()).meteor$getAllMessages();
      if (messages.isEmpty()) {
         return null;
      }

      for (int i = 0; i < Math.min(this.antiSpamDepth.get(), messages.size()); i++) {
         String stringToCheck = messages.get(i).content().getString();
         stringToCheck = this.stripChatDecorations(stringToCheck);
         if (textString.equals(stringToCheck)) {
            messageIndex = i;
            returnText = text.copy().append(Component.literal(" (2)").withStyle(ChatFormatting.GRAY));
            break;
         }

         Matcher matcher = antiSpamRegex.matcher(stringToCheck);
         if (matcher.find()) {
            String group = matcher.group(matcher.groupCount());
            int number = Integer.parseInt(group);
            if (stringToCheck.substring(0, matcher.start()).equals(textString)) {
               messageIndex = i;
               returnText = text.copy().append(Component.literal(" (" + (number + 1) + ")").withStyle(ChatFormatting.GRAY));
               break;
            }
         }
      }

      if (returnText != null) {
         List<Line> visible = ((ChatComponentAccessor)this.mc.gui.getChat()).meteor$getTrimmedMessages();
         int start = -1;

         for (int i = 0; i < messageIndex; i++) {
            start += this.lines.getInt(i);
         }

         for (int i = this.lines.getInt(messageIndex); i > 0; i--) {
            visible.remove(start + 1);
         }

         messages.remove(messageIndex);
         this.lines.removeInt(messageIndex);
      }

      return returnText;
   }

   public void removeLine(int index) {
      if (index >= this.lines.size()) {
         if (this.antiSpam.get()) {
            this.error(
               "Issue detected with the anti-spam system! Likely a compatibility issue with another mod. Disabling anti-spam to protect chat integrity."
            );
            this.antiSpam.set(false);
         }
      } else {
         this.lines.removeInt(index);
      }
   }

   public static void registerCustomHead(String prefix, Identifier texture) {
      CUSTOM_HEAD_ENTRIES.add(new BetterChat.CustomHeadEntry(prefix, texture));
   }

   public int modifyChatWidth(int width) {
      return this.isActive() && this.playerHeads.get() ? width + 10 : width;
   }

   public void beforeDrawMessage(GuiGraphicsExtractor graphics, int y, int color) {
      if (this.isActive() && this.playerHeads.get() && this.line != null) {
         if (((IGuiMessageVisible)(Object)this.line).meteor$isStartOfEntry()) {
            this.drawTexture(graphics, (IGuiMessage)(Object)this.line, y, color);
         }
      }
   }

   public void afterDrawMessage() {
      if (this.isActive() && this.playerHeads.get()) {
         this.line = null;
      }
   }

   private void drawTexture(GuiGraphicsExtractor graphics, IGuiMessage line, int y, int color) {
      String text = line.meteor$getText().trim();
      int startOffset = 0;

      try {
         Matcher m = TIMESTAMP_REGEX.matcher(text);
         if (m.find()) {
            startOffset = m.end() + 1;
         }

         Matcher c = copyButtonRegex.matcher(text.substring(Math.min(startOffset, text.length())));
         if (c.find()) {
            startOffset += c.end();
         }
      } catch (IllegalStateException var9) {
      }

      for (BetterChat.CustomHeadEntry entry : CUSTOM_HEAD_ENTRIES) {
         if (text.startsWith(entry.prefix(), startOffset)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, entry.texture(), 0, y, 0.0F, 0.0F, 8, 8, 64, 64, 64, 64, color);
            return;
         }
      }

      GameProfile sender = this.getSender(line, text);
      if (sender != null) {
         PlayerInfo entry = this.mc.getConnection().getPlayerInfo(sender.id());
         if (entry != null) {
            PlayerFaceExtractor.extractRenderState(graphics, entry.getSkin(), 0, y, 8, color);
         }
      }
   }

   private GameProfile getSender(IGuiMessage line, String text) {
      GameProfile sender = line.meteor$getSender();
      if (sender == null) {
         Matcher usernameMatcher = usernameRegex.matcher(text);
         if (usernameMatcher.matches()) {
            String username = usernameMatcher.group(1);
            PlayerInfo entry = this.mc.getConnection().getPlayerInfo(username);
            if (entry != null) {
               sender = entry.getProfile();
            }
         }
      }

      return sender;
   }

   private void updateDateFormat() {
      this.dateFormat = new SimpleDateFormat(this.showSeconds.get() ? "HH:mm:ss" : "HH:mm");
   }

   private MutableComponent getMessageButtons(String message, Component decoratedMessage) {
      MutableComponent buttons = Component.empty();
      MutableComponent copyButton = Component.literal("[c]");
      copyButton.setStyle(
         copyButton.getStyle()
            .applyFormat(ChatFormatting.GREEN)
            .withClickEvent(new CopyToClipboard(message))
            .withHoverEvent(new ShowText(Component.literal("Copy message")))
      );
      MutableComponent spacer = Component.literal(" ");
      MutableComponent deleteButton = Component.literal("[x]");
      deleteButton.setStyle(deleteButton.getStyle().applyFormat(ChatFormatting.RED).withClickEvent(new RunnableClickEvent(() -> {
         if (this.mc.gui != null) {
            ((IChatHud)this.mc.gui.getChat()).meteor$delete(decoratedMessage);
         }
      })).withHoverEvent(new ShowText(Component.literal("Delete message"))));
      return buttons.append(copyButton).append(spacer).append(deleteButton).append(Component.literal(" "));
   }

   private String stripChatDecorations(String message) {
      Matcher timestampMatcher = timestampRegex.matcher(message);
      if (timestampMatcher.find()) {
         message = message.substring(timestampMatcher.end());
      }

      Matcher copyMatcher = copyButtonRegex.matcher(message);
      if (copyMatcher.find()) {
         message = message.substring(copyMatcher.end());
      }

      return message;
   }

   private String applyAnnoy(String message) {
      StringBuilder sb = new StringBuilder(message.length());
      boolean upperCase = true;

      for (int cp : message.codePoints().toArray()) {
         if (upperCase) {
            sb.appendCodePoint(Character.toUpperCase(cp));
         } else {
            sb.appendCodePoint(Character.toLowerCase(cp));
         }

         upperCase = !upperCase;
      }

      return sb.toString();
   }

   private String applyFancy(String message) {
      StringBuilder sb = new StringBuilder();

      for (char ch : message.toCharArray()) {
         sb.append(this.SMALL_CAPS.getOrDefault(ch, ch));
      }

      return sb.toString();
   }

   private void compileFilterRegexList() {
      this.filterRegexList.clear();

      for (int i = 0; i < this.regexFilters.get().size(); i++) {
         try {
            this.filterRegexList.add(Pattern.compile(this.regexFilters.get().get(i)));
         } catch (PatternSyntaxException var4) {
            String removed = this.regexFilters.get().remove(i);
            this.error("Removing Invalid regex: %s", removed);
         }
      }
   }

   private String getPrefix() {
      return this.prefix.get() ? this.getAffix(this.prefixText.get(), this.prefixSmallCaps.get(), this.prefixRandom.get()) : "";
   }

   private String getSuffix() {
      return this.suffix.get() ? this.getAffix(this.suffixText.get(), this.suffixSmallCaps.get(), this.suffixRandom.get()) : "";
   }

   private String getAffix(String text, boolean smallcaps, boolean random) {
      if (random) {
         return String.format("(%03d) ", Utils.random(0, 1000));
      } else {
         return smallcaps ? this.applyFancy(text) : text;
      }
   }

   private boolean containsCoordinates(String message) {
      return coordRegex.matcher(message).find();
   }

   private MutableComponent getSendButton(String message) {
      MutableComponent sendButton = Component.literal("[SEND ANYWAY]");
      MutableComponent hintBaseText = Component.literal("");
      MutableComponent hintMsg = Component.literal("Send your message to the global chat even if there are coordinates:");
      hintMsg.setStyle(hintBaseText.getStyle().applyFormat(ChatFormatting.GRAY));
      hintBaseText.append(hintMsg);
      hintBaseText.append(Component.literal("\n" + message));
      sendButton.setStyle(
         sendButton.getStyle()
            .applyFormat(ChatFormatting.DARK_RED)
            .withClickEvent(new MeteorClickEvent(Commands.get("say").toString(message)))
            .withHoverEvent(new ShowText(hintBaseText))
      );
      return sendButton;
   }

   public boolean isInfiniteChatBox() {
      return this.isActive() && this.infiniteChatBox.get();
   }

   public boolean isLongerChat() {
      return this.isActive() && this.longerChatHistory.get();
   }

   public boolean keepHistory() {
      return this.isActive() && this.keepHistory.get();
   }

   public int getExtraChatLines() {
      return this.longerChatLines.get();
   }

   static {
      registerCustomHead("[Meteor]", MeteorClient.identifier("textures/icons/chat/meteor.png"));
      registerCustomHead("[Baritone]", MeteorClient.identifier("textures/icons/chat/baritone.png"));
   }

   private record CustomHeadEntry(String prefix, Identifier texture) {
   }
}
