package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.NotebotSongArgumentType;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Notebot;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class NotebotCommand extends Command {
   private static final SimpleCommandExceptionType INVALID_SONG = new SimpleCommandExceptionType(Component.literal("Invalid song."));
   private static final DynamicCommandExceptionType INVALID_PATH = new DynamicCommandExceptionType(
      object -> Component.literal("'%s' is not a valid path.".formatted(object))
   );
   int ticks = -1;
   private final Map<Integer, List<Note>> song = new HashMap<>();

   public NotebotCommand() {
      super("notebot", "Allows you load notebot files");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("help").executes(var0 -> {
         Util.getPlatform().openUri("https://github.com/MeteorDevelopment/meteor-client/wiki/Notebot-Guide");
         return 1;
      }));
      builder.then(literal("status").executes(var1x -> {
         Notebot notebot = Modules.get().get(Notebot.class);
         this.info(notebot.getStatus());
         return 1;
      }));
      builder.then(literal("pause").executes(var0 -> {
         Notebot notebot = Modules.get().get(Notebot.class);
         notebot.pause();
         return 1;
      }));
      builder.then(literal("resume").executes(var0 -> {
         Notebot notebot = Modules.get().get(Notebot.class);
         notebot.pause();
         return 1;
      }));
      builder.then(literal("stop").executes(var0 -> {
         Notebot notebot = Modules.get().get(Notebot.class);
         notebot.stop();
         return 1;
      }));
      builder.then(literal("randomsong").executes(var0 -> {
         Notebot notebot = Modules.get().get(Notebot.class);
         notebot.playRandomSong();
         return 1;
      }));
      builder.then(literal("play").then(argument("song", NotebotSongArgumentType.create()).executes(ctx -> {
         Notebot notebot = Modules.get().get(Notebot.class);
         Path songPath = ctx.getArgument("song", Path.class);
         if (songPath != null && songPath.toFile().exists()) {
            notebot.loadSong(songPath.toFile());
            return 1;
         } else {
            throw INVALID_SONG.create();
         }
      })));
      builder.then(literal("preview").then(argument("song", NotebotSongArgumentType.create()).executes(ctx -> {
         Notebot notebot = Modules.get().get(Notebot.class);
         Path songPath = ctx.getArgument("song", Path.class);
         if (songPath != null && songPath.toFile().exists()) {
            notebot.previewSong(songPath.toFile());
            return 1;
         } else {
            throw INVALID_SONG.create();
         }
      })));
      builder.then(literal("record").then(literal("start").executes(var1x -> {
         this.ticks = -1;
         this.song.clear();
         MeteorClient.EVENT_BUS.subscribe(this);
         this.info("Recording started");
         return 1;
      })));
      builder.then(literal("record").then(literal("cancel").executes(var1x -> {
         MeteorClient.EVENT_BUS.unsubscribe(this);
         this.info("Recording cancelled");
         return 1;
      })));
      builder.then(literal("record").then(literal("save").then(argument("name", StringArgumentType.greedyString()).executes(ctx -> {
         String name = ctx.getArgument("name", String.class);
         if (name != null && !name.isEmpty()) {
            Path notebotFolder = MeteorClient.FOLDER.toPath().resolve("notebot");
            Path path = notebotFolder.resolve(String.format("%s.txt", name)).normalize();
            if (!path.startsWith(notebotFolder)) {
               throw INVALID_PATH.create(path);
            }

            this.saveRecording(path);
            return 1;
         } else {
            throw INVALID_PATH.create(name);
         }
      }))));
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.ticks != -1) {
         this.ticks++;
      }
   }

   @EventHandler
   private void onReadPacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundSoundPacket sound && sound.getSound().value().location().getPath().contains("note_block")) {
         if (this.ticks == -1) {
            this.ticks = 0;
         }

         List<Note> notes = this.song.computeIfAbsent(this.ticks, var0 -> new ArrayList<>());
         Note note = this.getNote(sound);
         if (note != null) {
            notes.add(note);
         }
      }
   }

   private void saveRecording(Path path) {
      if (this.song.isEmpty()) {
         MeteorClient.EVENT_BUS.unsubscribe(this);
      } else {
         try {
            MeteorClient.EVENT_BUS.unsubscribe(this);
            FileWriter file = new FileWriter(path.toFile());

            for (Entry<Integer, List<Note>> entry : this.song.entrySet()) {
               int tick = entry.getKey();

               for (Note note : entry.getValue()) {
                  NoteBlockInstrument instrument = note.getInstrument();
                  int noteLevel = note.getNoteLevel();
                  file.write(String.format("%d:%d:%d\n", tick, noteLevel, instrument.ordinal()));
               }
            }

            file.close();
            this.info("Song saved.");
         } catch (IOException var11) {
            this.info("Couldn't create the file.");
            MeteorClient.EVENT_BUS.unsubscribe(this);
         }
      }
   }

   private Note getNote(ClientboundSoundPacket soundPacket) {
      float pitch = soundPacket.getPitch();
      int noteLevel = -1;

      for (int n = 0; n < 25; n++) {
         if ((float)Math.pow(2.0, (n - 12) / 12.0) - 0.01 < pitch && (float)Math.pow(2.0, (n - 12) / 12.0) + 0.01 > pitch) {
            noteLevel = n;
            break;
         }
      }

      if (noteLevel == -1) {
         this.error("Error while bruteforcing a note level! Sound: " + soundPacket.getSound().value() + " Pitch: " + pitch);
         return null;
      } else {
         NoteBlockInstrument instrument = this.getInstrumentFromSound(soundPacket.getSound().value());
         if (instrument == null) {
            this.error("Can't find the instrument from sound! Sound: " + soundPacket.getSound().value());
            return null;
         } else {
            return new Note(instrument, noteLevel);
         }
      }
   }

   private NoteBlockInstrument getInstrumentFromSound(SoundEvent sound) {
      String path = sound.location().getPath();
      if (path.contains("harp")) {
         return NoteBlockInstrument.HARP;
      } else if (path.contains("basedrum")) {
         return NoteBlockInstrument.BASEDRUM;
      } else if (path.contains("snare")) {
         return NoteBlockInstrument.SNARE;
      } else if (path.contains("hat")) {
         return NoteBlockInstrument.HAT;
      } else if (path.contains("bass")) {
         return NoteBlockInstrument.BASS;
      } else if (path.contains("flute")) {
         return NoteBlockInstrument.FLUTE;
      } else if (path.contains("bell")) {
         return NoteBlockInstrument.BELL;
      } else if (path.contains("guitar")) {
         return NoteBlockInstrument.GUITAR;
      } else if (path.contains("chime")) {
         return NoteBlockInstrument.CHIME;
      } else if (path.contains("xylophone")) {
         return NoteBlockInstrument.XYLOPHONE;
      } else if (path.contains("iron_xylophone")) {
         return NoteBlockInstrument.IRON_XYLOPHONE;
      } else if (path.contains("cow_bell")) {
         return NoteBlockInstrument.COW_BELL;
      } else if (path.contains("didgeridoo")) {
         return NoteBlockInstrument.DIDGERIDOO;
      } else if (path.contains("bit")) {
         return NoteBlockInstrument.BIT;
      } else if (path.contains("banjo")) {
         return NoteBlockInstrument.BANJO;
      } else {
         return path.contains("pling") ? NoteBlockInstrument.PLING : null;
      }
   }
}
