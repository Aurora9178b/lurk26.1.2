package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider.TextCoordinates;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class BlockPosArgumentType implements ArgumentType<BlockPosArgumentType.PosArgument> {
   private static final BlockPosArgumentType INSTANCE = new BlockPosArgumentType();
   private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");
   public static final SimpleCommandExceptionType UNLOADED_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.pos.unloaded"));
   public static final SimpleCommandExceptionType OUT_OF_WORLD_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.pos.outofworld"));
   public static final SimpleCommandExceptionType OUT_OF_BOUNDS_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.pos.outofbounds"));

   private BlockPosArgumentType() {
   }

   public static BlockPosArgumentType blockPos() {
      return INSTANCE;
   }

   public static <S> BlockPos getLoadedBlockPos(CommandContext<S> context, String name) throws CommandSyntaxException {
      ClientLevel clientLevel = MeteorClient.mc.level;
      return getLoadedBlockPos(context, clientLevel, name);
   }

   public static <S> BlockPos getLoadedBlockPos(CommandContext<S> context, ClientLevel level, String name) throws CommandSyntaxException {
      BlockPos blockPos = getBlockPos(context, name);
      ChunkPos chunkPos = new ChunkPos(blockPos.getX(), blockPos.getZ());
      if (!level.getChunkSource().hasChunk(chunkPos.x(), chunkPos.z())) {
         throw UNLOADED_EXCEPTION.create();
      } else if (!level.isInWorldBounds(blockPos)) {
         throw OUT_OF_WORLD_EXCEPTION.create();
      } else {
         return blockPos;
      }
   }

   public static <S> BlockPos getBlockPos(CommandContext<S> context, String name) {
      return context.getArgument(name, BlockPosArgumentType.PosArgument.class).getBlockPos(context.getSource());
   }

   public static <S> BlockPos getValidBlockPos(CommandContext<S> context, String name) throws CommandSyntaxException {
      BlockPos blockPos = getBlockPos(context, name);
      if (!Level.isInSpawnableBounds(blockPos)) {
         throw OUT_OF_BOUNDS_EXCEPTION.create();
      } else {
         return blockPos;
      }
   }

   public BlockPosArgumentType.PosArgument parse(StringReader stringReader) throws CommandSyntaxException {
      return stringReader.canRead() && stringReader.peek() == '^'
         ? BlockPosArgumentType.LookingPosArgument.parse(stringReader)
         : BlockPosArgumentType.DefaultPosArgument.parse(stringReader);
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      if (!(context.getSource() instanceof SharedSuggestionProvider sharedSuggestionProvider)) {
         return Suggestions.empty();
      } else {
         String string = builder.getRemaining();
         Collection<TextCoordinates> collection;
         if (!string.isEmpty() && string.charAt(0) == '^') {
            collection = Collections.singleton(TextCoordinates.DEFAULT_LOCAL);
         } else {
            collection = sharedSuggestionProvider.getRelevantCoordinates();
         }

         return SharedSuggestionProvider.suggestCoordinates(string, collection, builder, Commands.createValidator(this::parse));
      }
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class DefaultPosArgument implements BlockPosArgumentType.PosArgument {
      private final WorldCoordinate x;
      private final WorldCoordinate y;
      private final WorldCoordinate z;

      public DefaultPosArgument(WorldCoordinate x, WorldCoordinate y, WorldCoordinate z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }

      @Override
      public <S> Vec3 getPosition(S source) {
         Vec3 vec3 = MeteorClient.mc.player.position();
         return new Vec3(this.x.get(vec3.x), this.y.get(vec3.y), this.z.get(vec3.z));
      }

      @Override
      public <S> Vec2 getRotation(S source) {
         Vec2 vec2 = MeteorClient.mc.player.getRotationVector();
         return new Vec2((float)this.x.get(vec2.x), (float)this.y.get(vec2.y));
      }

      @Override
      public boolean isXRelative() {
         return this.x.isRelative();
      }

      @Override
      public boolean isYRelative() {
         return this.y.isRelative();
      }

      @Override
      public boolean isZRelative() {
         return this.z.isRelative();
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else {
            return !(o instanceof BlockPosArgumentType.DefaultPosArgument defaultPosArgument)
               ? false
               : this.x.equals(defaultPosArgument.x) && this.y.equals(defaultPosArgument.y) && this.z.equals(defaultPosArgument.z);
         }
      }

      public static BlockPosArgumentType.DefaultPosArgument parse(StringReader reader) throws CommandSyntaxException {
         int cursor = reader.getCursor();
         WorldCoordinate worldCoordinate = WorldCoordinate.parseInt(reader);
         if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            WorldCoordinate worldCoordinate2 = WorldCoordinate.parseInt(reader);
            if (reader.canRead() && reader.peek() == ' ') {
               reader.skip();
               WorldCoordinate worldCoordinate3 = WorldCoordinate.parseInt(reader);
               return new BlockPosArgumentType.DefaultPosArgument(worldCoordinate, worldCoordinate2, worldCoordinate3);
            }
         }

         reader.setCursor(cursor);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
      }

      public static BlockPosArgumentType.DefaultPosArgument parse(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
         int cursor = reader.getCursor();
         WorldCoordinate worldCoordinate = WorldCoordinate.parseDouble(reader, centerIntegers);
         if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            WorldCoordinate worldCoordinate2 = WorldCoordinate.parseDouble(reader, false);
            if (reader.canRead() && reader.peek() == ' ') {
               reader.skip();
               WorldCoordinate worldCoordinate3 = WorldCoordinate.parseDouble(reader, centerIntegers);
               return new BlockPosArgumentType.DefaultPosArgument(worldCoordinate, worldCoordinate2, worldCoordinate3);
            }
         }

         reader.setCursor(cursor);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
      }

      public static BlockPosArgumentType.DefaultPosArgument absolute(double x, double y, double z) {
         return new BlockPosArgumentType.DefaultPosArgument(new WorldCoordinate(false, x), new WorldCoordinate(false, y), new WorldCoordinate(false, z));
      }

      public static BlockPosArgumentType.DefaultPosArgument absolute(Vec2 vec) {
         return new BlockPosArgumentType.DefaultPosArgument(
            new WorldCoordinate(false, vec.x), new WorldCoordinate(false, vec.y), new WorldCoordinate(true, 0.0)
         );
      }

      public static BlockPosArgumentType.DefaultPosArgument current() {
         return new BlockPosArgumentType.DefaultPosArgument(new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0));
      }

      @Override
      public int hashCode() {
         int i = this.x.hashCode();
         i = 31 * i + this.y.hashCode();
         return 31 * i + this.z.hashCode();
      }
   }

   public static class LookingPosArgument implements BlockPosArgumentType.PosArgument {
      private final double x;
      private final double y;
      private final double z;

      public LookingPosArgument(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }

      @Override
      public <S> Vec3 getPosition(S source) {
         Vec2 vec2 = MeteorClient.mc.player.getRotationVector();
         Vec3 vec3 = Anchor.FEET.apply(MeteorClient.mc.player);
         float f = Mth.cos((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
         float g = Mth.sin((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
         float h = Mth.cos(-vec2.x * (float) (Math.PI / 180.0));
         float i = Mth.sin(-vec2.x * (float) (Math.PI / 180.0));
         float j = Mth.cos((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
         float k = Mth.sin((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
         Vec3 vec32 = new Vec3(f * h, i, g * h);
         Vec3 vec33 = new Vec3(f * j, k, g * j);
         Vec3 vec34 = vec32.cross(vec33).scale(-1.0);
         double d = vec32.x * this.z + vec33.x * this.y + vec34.x * this.x;
         double e = vec32.y * this.z + vec33.y * this.y + vec34.y * this.x;
         double l = vec32.z * this.z + vec33.z * this.y + vec34.z * this.x;
         return new Vec3(vec3.x + d, vec3.y + e, vec3.z + l);
      }

      @Override
      public <S> Vec2 getRotation(S source) {
         return Vec2.ZERO;
      }

      @Override
      public boolean isXRelative() {
         return true;
      }

      @Override
      public boolean isYRelative() {
         return true;
      }

      @Override
      public boolean isZRelative() {
         return true;
      }

      public static BlockPosArgumentType.LookingPosArgument parse(StringReader reader) throws CommandSyntaxException {
         int cursor = reader.getCursor();
         double d = readCoordinate(reader, cursor);
         if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            double e = readCoordinate(reader, cursor);
            if (reader.canRead() && reader.peek() == ' ') {
               reader.skip();
               double f = readCoordinate(reader, cursor);
               return new BlockPosArgumentType.LookingPosArgument(d, e, f);
            } else {
               reader.setCursor(cursor);
               throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
            }
         } else {
            reader.setCursor(cursor);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
         }
      }

      private static double readCoordinate(StringReader reader, int startingCursorPos) throws CommandSyntaxException {
         if (!reader.canRead()) {
            throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
         } else if (reader.peek() != '^') {
            reader.setCursor(startingCursorPos);
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
         } else {
            reader.skip();
            return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
         }
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else {
            return !(o instanceof BlockPosArgumentType.LookingPosArgument lookingPosArgument)
               ? false
               : this.x == lookingPosArgument.x && this.y == lookingPosArgument.y && this.z == lookingPosArgument.z;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.x, this.y, this.z);
      }
   }

   public interface PosArgument {
      <S> Vec3 getPosition(S var1);

      <S> Vec2 getRotation(S var1);

      default <S> BlockPos getBlockPos(S source) {
         return BlockPos.containing(this.getPosition(source));
      }

      boolean isXRelative();

      boolean isYRelative();

      boolean isZRelative();
   }
}
