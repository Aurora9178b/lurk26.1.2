package meteordevelopment.meteorclient.commands.arguments;

import net.minecraft.commands.arguments.StringRepresentableArgument;
import net.minecraft.core.Direction;

public class DirectionArgumentType extends StringRepresentableArgument<Direction> {
   private static final DirectionArgumentType INSTANCE = new DirectionArgumentType();

   private DirectionArgumentType() {
      super(Direction.CODEC, Direction::values);
   }

   public static DirectionArgumentType create() {
      return INSTANCE;
   }
}
