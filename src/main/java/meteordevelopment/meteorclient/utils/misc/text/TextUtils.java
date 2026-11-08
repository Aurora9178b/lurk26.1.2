package meteordevelopment.meteorclient.utils.misc.text;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

public class TextUtils {
   private TextUtils() {
   }

   public static List<ColoredText> toColoredTextList(Component text) {
      Deque<ColoredText> stack = new ArrayDeque<>();
      List<ColoredText> coloredTexts = new ArrayList<>();
      preOrderTraverse(text, stack, coloredTexts);
      coloredTexts.removeIf(e -> e.text().isEmpty());
      return coloredTexts;
   }

   public static MutableComponent parseOrderedText(FormattedCharSequence orderedText) {
      MutableComponent parsedText = Component.empty();
      orderedText.accept((var1x, style, codePoint) -> {
         parsedText.append(Component.literal(new String(Character.toChars(codePoint))).setStyle(style));
         return true;
      });
      return parsedText;
   }

   public static Color getMostPopularColor(Component text) {
      Entry<Color> biggestEntry = null;

      for (Entry<Color> entry : getColoredCharacterCount(toColoredTextList(text)).object2IntEntrySet()) {
         if (biggestEntry == null) {
            biggestEntry = entry;
         } else if (entry.getIntValue() > biggestEntry.getIntValue()) {
            biggestEntry = entry;
         }
      }

      return biggestEntry == null ? new Color(255, 255, 255) : biggestEntry.getKey();
   }

   public static Object2IntMap<Color> getColoredCharacterCount(List<ColoredText> coloredTexts) {
      Object2IntMap<Color> colorCount = new Object2IntOpenHashMap<>();

      for (ColoredText coloredText : coloredTexts) {
         if (colorCount.containsKey(coloredText.color())) {
            colorCount.put(coloredText.color(), colorCount.getInt(coloredText.color()) + coloredText.text().length());
         } else {
            colorCount.put(coloredText.color(), coloredText.text().length());
         }
      }

      return colorCount;
   }

   private static void preOrderTraverse(Component text, Deque<ColoredText> stack, List<ColoredText> coloredTexts) {
      if (text != null) {
         String textString = text.getString();
         TextColor mcTextColor = text.getStyle().getColor();
         Color textColor;
         if (mcTextColor == null) {
            if (stack.isEmpty()) {
               textColor = new Color(255, 255, 255);
            } else {
               textColor = stack.peek().color();
            }
         } else {
            textColor = new Color(text.getStyle().getColor().getValue() | 0xFF000000);
         }

         ColoredText coloredText = new ColoredText(textString, textColor);
         coloredTexts.add(coloredText);
         stack.push(coloredText);

         for (Component child : text.getSiblings()) {
            preOrderTraverse(child, stack, coloredTexts);
         }

         stack.pop();
      }
   }
}
