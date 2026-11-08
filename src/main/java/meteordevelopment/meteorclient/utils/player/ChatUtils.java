package meteordevelopment.meteorclient.utils.player;

import com.mojang.brigadier.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.client.PrefixSetting;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ChatUtils {
   private static final List<Tuple<String, Supplier<Component>>> customPrefixes = new ArrayList<>();
   private static final double GRADIENT_CHAR_STEP = 0.55;
   private static String forcedPrefixClassName;
   private static Component PREFIX;

   private ChatUtils() {
   }

   @PostInit
   public static void init() {
      PREFIX = getMeteorPrefix();
   }

   public static Component getMeteorPrefix() {
      String text = PrefixSetting.getClientName();
      int total = text.length() + 2;
      MutableComponent prefix = Component.empty().append(Component.literal("[").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getPrefixColor(0, total)))));

      for (int i = 0; i < text.length(); i++) {
         prefix = prefix.append(
            Component.literal(String.valueOf(text.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getPrefixColor(i + 1, total))))
         );
      }

      return prefix.append(Component.literal("]").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getPrefixColor(total - 1, total))))).append(" ");
   }

   public static FormattedCharSequence animateSequence(FormattedCharSequence original) {
      return applyAnimatedPrefix(original);
   }

   public static FormattedCharSequence applyAnimatedPrefix(FormattedCharSequence original) {
      String rawLine = toPlainString(original);
      ChatUtils.PrefixRange prefix = getAnimatedPrefix(rawLine);
      if (prefix == null) {
         return original;
      }

      MutableComponent animatedLine = Component.empty();
      double animationTime = System.currentTimeMillis() / 180.0 * Math.max(0.1, PrefixSetting.getSpeed() / 20.0);
      appendStyledRange(animatedLine, original, 0, prefix.startCodePoints);
      int visualIndex = 0;

      for (int offset = 0; offset < prefix.text.length(); visualIndex++) {
         int codePoint = prefix.text.codePointAt(offset);
         String character = new String(Character.toChars(codePoint));
         float blend = (float)((Math.sin(animationTime - visualIndex * 0.55) + 1.0) * 0.5);
         animatedLine.append(Component.literal(character).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(interpolatePrefixColor(blend)))));
         offset += Character.charCount(codePoint);
      }

      appendStyledSuffix(animatedLine, original, prefix.endCodePoints);
      return animatedLine.getVisualOrderText();
   }

   public static boolean shouldAnimatePrefix(FormattedCharSequence sequence) {
      return !PrefixSetting.isFlowing() ? false : getAnimatedPrefix(toPlainString(sequence)) != null;
   }

   private static ChatUtils.PrefixRange getAnimatedPrefix(String rawLine) {
      return findPrefix(rawLine, "Lurk");
   }

   private static ChatUtils.PrefixRange findPrefix(String rawLine, String name) {
      if (name != null && !name.isBlank()) {
         String token = "[" + name + "]";
         int charStart = rawLine.indexOf(token);
         if (charStart < 0) {
            return null;
         }

         int charEnd = charStart + token.length();
         if (charEnd < rawLine.length() && rawLine.charAt(charEnd) == ' ') {
            charEnd++;
         }

         String text = rawLine.substring(charStart, charEnd);
         int startCodePoints = rawLine.codePointCount(0, charStart);
         int endCodePoints = rawLine.codePointCount(0, charEnd);
         return new ChatUtils.PrefixRange(text, startCodePoints, endCodePoints);
      } else {
         return null;
      }
   }

   private static void appendStyledRange(MutableComponent component, FormattedCharSequence sequence, int startCodePoints, int endCodePoints) {
      int[] seenCodePoints = new int[]{0};
      sequence.accept((index, style, codePoint) -> {
         if (seenCodePoints[0] >= startCodePoints && seenCodePoints[0] < endCodePoints) {
            component.append(Component.literal(new String(Character.toChars(codePoint))).setStyle(style));
         }

         seenCodePoints[0]++;
         return true;
      });
   }

   private static void appendStyledSuffix(MutableComponent component, FormattedCharSequence sequence, int skipCodePoints) {
      int[] seenCodePoints = new int[]{0};
      sequence.accept((index, style, codePoint) -> {
         if (seenCodePoints[0]++ >= skipCodePoints) {
            component.append(Component.literal(new String(Character.toChars(codePoint))).setStyle(style));
         }

         return true;
      });
   }

   private static String toPlainString(FormattedCharSequence sequence) {
      StringBuilder builder = new StringBuilder();
      sequence.accept((index, style, codePoint) -> {
         builder.appendCodePoint(codePoint);
         return true;
      });
      return builder.toString();
   }

   private static int interpolatePrefixColor(float ratio) {
      SettingColor start = PrefixSetting.getStartColor();
      SettingColor end = PrefixSetting.getEndColor();
      int r = (int)(start.r + (end.r - start.r) * ratio);
      int g = (int)(start.g + (end.g - start.g) * ratio);
      int b = (int)(start.b + (end.b - start.b) * ratio);
      return r << 16 | g << 8 | b;
   }

   private static int getPrefixColor(int index, int total) {
      if (!PrefixSetting.isFlowing()) {
         return interpolatePrefixColor(index, total);
      }

      SettingColor start = PrefixSetting.getStartColor();
      SettingColor end = PrefixSetting.getEndColor();
      int safeTotal = Math.max(1, total);
      int speed = Math.max(1, PrefixSetting.getSpeed());
      double time = (double)System.currentTimeMillis() / (speed * 50L);
      double phase = ((double)index / safeTotal + time / safeTotal) * Math.PI * 2.0;
      float ratio = (float)((Math.sin(phase) + 1.0) / 2.0);
      int r = (int)(start.r + (end.r - start.r) * ratio);
      int g = (int)(start.g + (end.g - start.g) * ratio);
      int b = (int)(start.b + (end.b - start.b) * ratio);
      return r << 16 | g << 8 | b;
   }

   private static int interpolatePrefixColor(int index, int total) {
      SettingColor start = PrefixSetting.getStartColor();
      SettingColor end = PrefixSetting.getEndColor();
      float ratio = total <= 1 ? 0.0F : (float)index / (total - 1);
      int r = (int)(start.r + (end.r - start.r) * ratio);
      int g = (int)(start.g + (end.g - start.g) * ratio);
      int b = (int)(start.b + (end.b - start.b) * ratio);
      return r << 16 | g << 8 | b;
   }

   public static void registerCustomPrefix(String packageName, Supplier<Component> supplier) {
      for (Tuple<String, Supplier<Component>> pair : customPrefixes) {
         if (pair.getA().equals(packageName)) {
            pair.setB(supplier);
            return;
         }
      }

      customPrefixes.add(new Tuple<>(packageName, supplier));
   }

   public static void unregisterCustomPrefix(String packageName) {
      customPrefixes.removeIf(pair -> pair.getA().equals(packageName));
   }

   public static void forceNextPrefixClass(Class<?> klass) {
      forcedPrefixClassName = klass.getName();
   }

   public static void sendPlayerMsg(String message) {
      sendPlayerMsg(message, true);
   }

   public static void sendPlayerMsg(String message, boolean addToHistory) {
      if (addToHistory) {
         MeteorClient.mc.gui.getChat().addRecentChat(message);
      }

      if (message.startsWith("/")) {
         MeteorClient.mc.player.connection.sendCommand(message.substring(1));
      } else {
         MeteorClient.mc.player.connection.sendChat(message);
      }
   }

   public static void info(String message, Object... args) {
      sendMsg(ChatFormatting.GRAY, message, args);
   }

   public static void infoPrefix(String prefix, String message, Object... args) {
      sendMsg(0, prefix, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GRAY, message, args);
   }

   public static void warning(String message, Object... args) {
      sendMsg(ChatFormatting.YELLOW, message, args);
   }

   public static void warningPrefix(String prefix, String message, Object... args) {
      sendMsg(0, prefix, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW, message, args);
   }

   public static void error(String message, Object... args) {
      sendMsg(ChatFormatting.RED, message, args);
   }

   public static void errorPrefix(String prefix, String message, Object... args) {
      sendMsg(0, prefix, ChatFormatting.LIGHT_PURPLE, ChatFormatting.RED, message, args);
   }

   public static void sendMsg(Component message) {
      sendMsg(null, message);
   }

   public static void sendMsg(String prefix, Component message) {
      sendMsg(0, prefix, ChatFormatting.LIGHT_PURPLE, message);
   }

   public static void sendMsg(ChatFormatting color, String message, Object... args) {
      sendMsg(0, null, null, color, message, args);
   }

   public static void sendMsg(int id, ChatFormatting color, String message, Object... args) {
      sendMsg(id, null, null, color, message, args);
   }

   public static void sendMsg(
      int id, @Nullable String prefixTitle, @Nullable ChatFormatting prefixColor, ChatFormatting messageColor, String messageContent, Object... args
   ) {
      MutableComponent message = formatMsg(String.format(messageContent, args), messageColor);
      sendMsg(id, prefixTitle, prefixColor, message);
   }

   public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable ChatFormatting prefixColor, String messageContent, ChatFormatting messageColor) {
      MutableComponent message = formatMsg(messageContent, messageColor);
      sendMsg(id, prefixTitle, prefixColor, message);
   }

   public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable ChatFormatting prefixColor, Component msg) {
      if (MeteorClient.mc.level != null) {
         MutableComponent message = Component.empty();
         message.append(getPrefix());
         if (prefixTitle != null) {
            message.append(getCustomPrefix(prefixTitle, prefixColor));
         }

         message.append(msg);
         if (!Config.get().deleteChatFeedback.get()) {
            id = 0;
         }

         int finalId = id;
         MeteorClient.mc.execute(() -> ((IChatHud)MeteorClient.mc.gui.getChat()).meteor$add(message, finalId));
      }
   }

   private static MutableComponent getCustomPrefix(String prefixTitle, ChatFormatting prefixColor) {
      MutableComponent prefix = Component.empty();
      prefix.setStyle(prefix.getStyle().applyFormats(ChatFormatting.GRAY));
      prefix.append("[");
      MutableComponent moduleTitle = Component.literal(prefixTitle);
      moduleTitle.setStyle(moduleTitle.getStyle().applyFormats(prefixColor));
      prefix.append(moduleTitle);
      prefix.append("] ");
      return prefix;
   }

   private static Component getPrefix() {
      if (customPrefixes.isEmpty()) {
         forcedPrefixClassName = null;
         return getMeteorPrefix();
      }

      boolean foundChatUtils = false;
      String className = null;
      if (forcedPrefixClassName != null) {
         className = forcedPrefixClassName;
         forcedPrefixClassName = null;
      } else {
         for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (foundChatUtils) {
               if (!element.getClassName().equals(ChatUtils.class.getName())) {
                  className = element.getClassName();
                  break;
               }
            } else if (element.getClassName().equals(ChatUtils.class.getName())) {
               foundChatUtils = true;
            }
         }
      }

      if (className == null) {
         return getMeteorPrefix();
      }

      for (Tuple<String, Supplier<Component>> pair : customPrefixes) {
         if (className.startsWith(pair.getA())) {
            Component prefix = pair.getB().get();
            return prefix != null ? prefix : getMeteorPrefix();
         }
      }

      return getMeteorPrefix();
   }

   private static MutableComponent formatMsg(String message, ChatFormatting defaultColor) {
      StringReader reader = new StringReader(message);
      MutableComponent text = Component.empty();
      Style style = Style.EMPTY.applyFormats(defaultColor);
      StringBuilder result = new StringBuilder();
      boolean formatting = false;

      while (reader.canRead()) {
         char c = reader.read();
         if (c == '(') {
            text.append(Component.literal(result.toString()).setStyle(style));
            result.setLength(0);
            result.append(c);
            formatting = true;
         } else {
            result.append(c);
            if (formatting && c == ')') {
               switch (result.toString()) {
                  case "(default)":
                     style = style.applyFormats(defaultColor);
                     result.setLength(0);
                     break;
                  case "(highlight)":
                     style = style.applyFormats(ChatFormatting.WHITE);
                     result.setLength(0);
                     break;
                  case "(underline)":
                     style = style.applyFormats(ChatFormatting.UNDERLINE);
                     result.setLength(0);
                     break;
                  case "(bold)":
                     style = style.applyFormats(ChatFormatting.BOLD);
                     result.setLength(0);
               }

               formatting = false;
            }
         }
      }

      if (!result.isEmpty()) {
         text.append(Component.literal(result.toString()).setStyle(style));
      }

      return text;
   }

   public static MutableComponent formatCoords(Vec3 pos) {
      String coordsString = String.format("(highlight)(underline)%.0f, %.0f, %.0f(default)", pos.x, pos.y, pos.z);
      MutableComponent coordsText = formatMsg(coordsString, ChatFormatting.GRAY);
      if (BaritoneUtils.IS_AVAILABLE) {
         Style style = coordsText.getStyle()
            .applyFormats(ChatFormatting.BOLD)
            .withHoverEvent(new ShowText(Component.literal("Set as Baritone goal")))
            .withClickEvent(new MeteorClickEvent(String.format("%sgoto %d %d %d", BaritoneUtils.getPrefix(), (int)pos.x, (int)pos.y, (int)pos.z)));
         coordsText.setStyle(style);
      }

      return coordsText;
   }

   private record PrefixRange(String text, int startCodePoints, int endCodePoints) {
   }
}
