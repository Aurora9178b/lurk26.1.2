package meteordevelopment.meteorclient.utils.misc.input;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.utils.misc.CursorStyle;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Input {
   private static final boolean[] keys = new boolean[512];
   private static final boolean[] buttons = new boolean[16];
   private static CursorStyle lastCursorStyle = CursorStyle.Default;

   private Input() {
   }

   public static void setKeyState(int key, boolean pressed) {
      if (key >= 0 && key < keys.length) {
         keys[key] = pressed;
      }
   }

   public static void setButtonState(int button, boolean pressed) {
      if (button >= 0 && button < buttons.length) {
         buttons[button] = pressed;
      }
   }

   public static int getKey(KeyMapping bind) {
      return ((KeyMappingAccessor)bind).meteor$getKey().getValue();
   }

   public static void setKeyState(KeyMapping bind, boolean pressed) {
      setKeyState(getKey(bind), pressed);
   }

   public static boolean isPressed(KeyMapping bind) {
      return isKeyPressed(getKey(bind)) || isButtonPressed(getKey(bind));
   }

   public static boolean isKeyPressed(int key) {
      if (!GuiKeyEvents.canUseKeys) {
         return false;
      } else {
         return key == -1 ? false : key < keys.length && keys[key];
      }
   }

   public static boolean isButtonPressed(int button) {
      return button == -1 ? false : button < buttons.length && buttons[button];
   }

   public static void setCursorStyle(CursorStyle style) {
      if (lastCursorStyle != style) {
         GLFW.glfwSetCursor(MeteorClient.mc.getWindow().handle(), style.getGlfwCursor());
         lastCursorStyle = style;
      }
   }

   public static int getModifier(int key) {
      return switch (key) {
         case 340, 344 -> 1;
         case 341, 345 -> 2;
         case 342, 346 -> 4;
         case 343, 347 -> 8;
         default -> 0;
      };
   }
}
