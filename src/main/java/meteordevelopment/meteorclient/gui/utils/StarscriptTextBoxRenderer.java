package meteordevelopment.meteorclient.gui.utils;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.meteordev.starscript.utils.SemanticToken;
import org.meteordev.starscript.utils.SemanticTokenProvider;
import org.meteordev.starscript.utils.SemanticTokenType;

public class StarscriptTextBoxRenderer implements WTextBox.Renderer {
   private static final Color RED = new Color(225, 25, 25);
   private final List<SemanticToken> tokens = new ArrayList<>();
   private final List<StarscriptTextBoxRenderer.Section> sections = new ArrayList<>();
   private String lastText;

   @Override
   public void render(GuiRenderer renderer, double x, double y, String text, Color color) {
      if (this.lastText == null || !this.lastText.equals(text)) {
         this.lastText = text;
         SemanticTokenProvider.get(text, this.tokens);
         this.convertTokensToSections(renderer.theme);
      }

      for (StarscriptTextBoxRenderer.Section section : this.sections) {
         renderer.text(section.text, x, y, section.color, false);
         x += renderer.theme.textWidth(section.text);
      }
   }

   @Override
   public List<String> getCompletions(String text, int position) {
      List<String> completions = new ArrayList<>();
      MeteorStarscript.ss.getCompletions(text, position, (completion, function) -> completions.add(function ? completion + "(" : completion));
      completions.sort(String::compareToIgnoreCase);
      return completions;
   }

   private void convertTokensToSections(GuiTheme theme) {
      this.sections.clear();
      int start = 0;

      for (SemanticToken token : this.tokens) {
         if (start != token.start) {
            this.sections.add(new StarscriptTextBoxRenderer.Section(this.lastText.substring(start, token.start), theme.starscriptTextColor()));
         }

         String text = this.lastText.substring(token.start, token.end);
         this.sections.add(new StarscriptTextBoxRenderer.Section(text, getColorForToken(theme, token.type, text)));
         start = token.end;
      }

      if (start < this.lastText.length()) {
         this.sections.add(new StarscriptTextBoxRenderer.Section(this.lastText.substring(start), theme.starscriptTextColor()));
      }
   }

   private static Color getColorForToken(GuiTheme theme, SemanticTokenType type, String text) {
      return switch (type) {
         case Dot -> {
            Color var17 = theme.starscriptDotColor();
            yield var17;
         }
         case Comma -> {
            Color var16 = theme.starscriptCommaColor();
            yield var16;
         }
         case Operator -> {
            Color var15 = theme.starscriptOperatorColor();
            yield var15;
         }
         case String -> {
            Color var14 = theme.starscriptStringColor();
            yield var14;
         }
         case Number -> {
            Color var13 = theme.starscriptNumberColor();
            yield var13;
         }
         case Keyword -> {
            Color var12 = theme.starscriptKeywordColor();
            yield var12;
         }
         case Paren -> {
            Color var11 = theme.starscriptParenthesisColor();
            yield var11;
         }
         case Brace -> {
            Color var10 = theme.starscriptBraceColor();
            yield var10;
         }
         case Identifier -> {
            Color var9 = theme.starscriptTextColor();
            yield var9;
         }
         case Map -> {
            Color var8 = theme.starscriptAccessedObjectColor();
            yield var8;
         }
         case Section -> {
            if (text.startsWith("#")) {
               text = text.substring(1);
            }

            Color var7;
            try {
               var7 = TextHud.getSectionColor(Integer.parseInt(text));
            } catch (NumberFormatException var5) {
               var7 = theme.starscriptTextColor();
               yield var7;
            }

            yield var7;
         }
         case Error -> {
            Color var3 = RED;
            yield var3;
         }
         default -> throw new MatchException(null, null);
      };
   }

   private record Section(String text, Color color) {
   }
}
