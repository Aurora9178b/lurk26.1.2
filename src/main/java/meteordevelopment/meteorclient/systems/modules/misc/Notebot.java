package meteordevelopment.meteorclient.systems.modules.misc;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.notebot.NotebotUtils;
import meteordevelopment.meteorclient.utils.notebot.decoder.SongDecoders;
import meteordevelopment.meteorclient.utils.notebot.instrumentdetect.InstrumentDetectMode;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import meteordevelopment.meteorclient.utils.notebot.song.Song;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class Notebot extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgNoteMap = this.settings.createGroup("Note Map", false);
   private final SettingGroup sgRender = this.settings.createGroup("Render", true);
   public final Setting<Integer> tickDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("tick-delay").description("The delay when loading a song.").defaultValue(1).sliderRange(1, 20).min(1).build());
   public final Setting<Integer> concurrentTuneBlocks = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("concurrent-tune-blocks")
            .description("How many noteblocks can be tuned at the same time. On Paper it is recommended to set it to 1 to avoid bugs.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .build()
      );
   public final Setting<NotebotUtils.NotebotMode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Select mode of notebot"))
               .defaultValue(NotebotUtils.NotebotMode.ExactInstruments))
            .build()
      );
   public final Setting<InstrumentDetectMode> instrumentDetectMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("instrument-detect-mode"))
                  .description(
                     "Select an instrument detect mode. Can be useful when server has a plugin that modifies noteblock state (e.g ItemsAdder) but noteblock can still play the right note"
                  ))
               .defaultValue(InstrumentDetectMode.BlockState))
            .build()
      );
   public final Setting<Boolean> polyphonic = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("polyphonic")
            .description("Whether or not to allow multiple notes to be played at the same time")
            .defaultValue(true)
            .build()
      );
   public final Setting<Boolean> autoRotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-rotate").description("Should client look at note block when it wants to hit it").defaultValue(true).build());
   public final Setting<Boolean> autoPlay = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-play").description("Auto plays random songs").defaultValue(false).build());
   public final Setting<Boolean> roundOutOfRange = this.sgGeneral
      .add(new BoolSetting.Builder().name("round-out-of-range").description("Rounds out of range notes").defaultValue(false).build());
   public final Setting<Boolean> swingArm = this.sgGeneral
      .add(new BoolSetting.Builder().name("swing-arm").description("Should swing arm on hit").defaultValue(true).build());
   public final Setting<Integer> checkNoteblocksAgainDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("check-noteblocks-again-delay")
            .description("How much delay should be between end of tuning and checking again")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 20)
            .build()
      );
   public final Setting<Boolean> renderText = this.sgRender
      .add(new BoolSetting.Builder().name("render-text").description("Whether or not to render the text above noteblocks.").defaultValue(true).build());
   public final Setting<Boolean> renderBoxes = this.sgRender
      .add(new BoolSetting.Builder().name("render-boxes").description("Whether or not to render the outline around the noteblocks.").defaultValue(true).build());
   public final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   public final Setting<SettingColor> untunedSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("untuned-side-color")
            .description("The color of the sides of the untuned blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
      );
   public final Setting<SettingColor> untunedLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("untuned-line-color")
            .description("The color of the lines of the untuned blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
      );
   public final Setting<SettingColor> tunedSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tuned-side-color")
            .description("The color of the sides of the tuned blocks being rendered.")
            .defaultValue(new SettingColor(0, 204, 0, 10))
            .build()
      );
   public final Setting<SettingColor> tunedLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tuned-line-color")
            .description("The color of the lines of the tuned blocks being rendered.")
            .defaultValue(new SettingColor(0, 204, 0, 255))
            .build()
      );
   public final Setting<SettingColor> tuneHitSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("hit-side-color")
            .description("The color of the sides being rendered on noteblock tune hit.")
            .defaultValue(new SettingColor(255, 153, 0, 10))
            .build()
      );
   private final Setting<SettingColor> tuneHitLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("hit-line-color")
            .description("The color of the lines being rendered on noteblock tune hit.")
            .defaultValue(new SettingColor(255, 153, 0, 255))
            .build()
      );
   public final Setting<SettingColor> scannedNoteblockSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("scanned-noteblock-side-color")
            .description("The color of the sides of the scanned noteblocks being rendered.")
            .defaultValue(new SettingColor(255, 255, 0, 30))
            .build()
      );
   private final Setting<SettingColor> scannedNoteblockLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("scanned-noteblock-line-color")
            .description("The color of the lines of the scanned noteblocks being rendered.")
            .defaultValue(new SettingColor(255, 255, 0, 255))
            .build()
      );
   public final Setting<Double> noteTextScale = this.sgRender
      .add(new DoubleSetting.Builder().name("note-text-scale").description("The scale.").defaultValue(1.5).min(0.0).build());
   public final Setting<Boolean> showScannedNoteblocks = this.sgRender
      .add(new BoolSetting.Builder().name("show-scanned-noteblocks").description("Show scanned Noteblocks").defaultValue(false).build());
   private CompletableFuture<Song> loadingSongFuture = null;
   private Song song;
   private final Map<Note, BlockPos> noteBlockPositions = new HashMap<>();
   private final Multimap<Note, BlockPos> scannedNoteblocks = MultimapBuilder.linkedHashKeys().arrayListValues().build();
   private final List<BlockPos> clickedBlocks = new ArrayList<>();
   private Notebot.Stage stage = Notebot.Stage.None;
   private Notebot.PlayingMode playingMode = Notebot.PlayingMode.None;
   private boolean isPlaying = false;
   private int currentTick = 0;
   private int ticks = 0;
   private WLabel status;
   private boolean anyNoteblockTuned = false;
   private final Map<BlockPos, Integer> tuneHits = new HashMap<>();
   private int waitTicks = -1;

   public Notebot() {
      super(Categories.Misc, "notebot", "Plays noteblock nicely");

      for (NoteBlockInstrument inst : NoteBlockInstrument.values()) {
         NotebotUtils.OptionalInstrument optionalInstrument = NotebotUtils.OptionalInstrument.fromMinecraftInstrument(inst);
         if (optionalInstrument != null) {
            this.sgNoteMap
               .add(
                  ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name(this.beautifyText(inst.name())))
                           .defaultValue(optionalInstrument))
                        .visible(() -> this.mode.get() == NotebotUtils.NotebotMode.ExactInstruments))
                     .build()
               );
         }
      }
   }

   @Override
   public String getInfoString() {
      return this.stage == Notebot.Stage.None ? "None" : this.playingMode.toString() + " | " + this.stage.toString();
   }

   @Override
   public void onActivate() {
      this.ticks = 0;
      this.resetVariables();
   }

   private void resetVariables() {
      if (this.loadingSongFuture != null) {
         this.loadingSongFuture.cancel(true);
         this.loadingSongFuture = null;
      }

      this.clickedBlocks.clear();
      this.tuneHits.clear();
      this.anyNoteblockTuned = false;
      this.currentTick = 0;
      this.playingMode = Notebot.PlayingMode.None;
      this.isPlaying = false;
      this.stage = Notebot.Stage.None;
      this.song = null;
      this.noteBlockPositions.clear();
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.renderBoxes.get()) {
         if (this.stage == Notebot.Stage.SetUp || this.stage == Notebot.Stage.Tune || this.stage == Notebot.Stage.WaitingToCheckNoteblocks || this.isPlaying) {
            if (this.showScannedNoteblocks.get()) {
               for (BlockPos blockPos : this.scannedNoteblocks.values()) {
                  double x1 = blockPos.getX();
                  double y1 = blockPos.getY();
                  double z1 = blockPos.getZ();
                  double x2 = blockPos.getX() + 1;
                  double y2 = blockPos.getY() + 1;
                  double z2 = blockPos.getZ() + 1;
                  event.renderer
                     .box(x1, y1, z1, x2, y2, z2, this.scannedNoteblockSideColor.get(), this.scannedNoteblockLineColor.get(), this.shapeMode.get(), 0);
               }
            } else {
               for (Entry<Note, BlockPos> entry : this.noteBlockPositions.entrySet()) {
                  Note note = entry.getKey();
                  BlockPos blockPos = entry.getValue();
                  BlockState state = this.mc.level.getBlockState(blockPos);
                  if (state.getBlock() == Blocks.NOTE_BLOCK) {
                     int level = state.getValue(NoteBlock.NOTE);
                     double x1 = blockPos.getX();
                     double y1 = blockPos.getY();
                     double z1 = blockPos.getZ();
                     double x2 = blockPos.getX() + 1;
                     double y2 = blockPos.getY() + 1;
                     double z2 = blockPos.getZ() + 1;
                     Color sideColor;
                     Color lineColor;
                     if (this.clickedBlocks.contains(blockPos)) {
                        sideColor = this.tuneHitSideColor.get();
                        lineColor = this.tuneHitLineColor.get();
                     } else if (note.getNoteLevel() == level) {
                        sideColor = this.tunedSideColor.get();
                        lineColor = this.tunedLineColor.get();
                     } else {
                        sideColor = this.untunedSideColor.get();
                        lineColor = this.untunedLineColor.get();
                     }

                     event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, this.shapeMode.get(), 0);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.renderText.get()) {
         if (this.stage == Notebot.Stage.SetUp || this.stage == Notebot.Stage.Tune || this.stage == Notebot.Stage.WaitingToCheckNoteblocks || this.isPlaying) {
            Vector3d pos = new Vector3d();

            for (BlockPos blockPos : this.noteBlockPositions.values()) {
               BlockState state = this.mc.level.getBlockState(blockPos);
               if (state.getBlock() == Blocks.NOTE_BLOCK) {
                  double x = blockPos.getX() + 0.5;
                  double y = blockPos.getY() + 1;
                  double z = blockPos.getZ() + 0.5;
                  pos.set(x, y, z);
                  String levelText = String.valueOf(state.getValue(NoteBlock.NOTE));
                  String tuneHitsText = null;
                  if (this.tuneHits.containsKey(blockPos)) {
                     tuneHitsText = " -" + this.tuneHits.get(blockPos);
                  }

                  if (NametagUtils.to2D(pos, this.noteTextScale.get(), true)) {
                     TextRenderer text = TextRenderer.get();
                     NametagUtils.begin(pos);
                     text.beginBig();
                     double xScreen = text.getWidth(levelText) / 2.0;
                     if (tuneHitsText != null) {
                        xScreen += text.getWidth(tuneHitsText) / 2.0;
                     }

                     double hX = text.render(levelText, -xScreen, 0.0, Color.GREEN);
                     if (tuneHitsText != null) {
                        text.render(tuneHitsText, hX, 0.0, Color.RED);
                     }

                     text.end();
                     NametagUtils.end();
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.ticks++;
      this.clickedBlocks.clear();
      if (this.stage == Notebot.Stage.WaitingToCheckNoteblocks) {
         this.waitTicks--;
         if (this.waitTicks == 0) {
            this.waitTicks = -1;
            this.info("Checking noteblocks again...");
            this.setupTuneHitsMap();
            this.stage = Notebot.Stage.Tune;
         }
      } else if (this.stage == Notebot.Stage.SetUp) {
         this.scanForNoteblocks();
         if (this.scannedNoteblocks.isEmpty()) {
            this.error("Can't find any nearby noteblock!");
            this.stop();
            return;
         }

         this.setupNoteblocksMap();
         if (this.noteBlockPositions.isEmpty()) {
            this.error("Can't find any valid noteblock to play song.");
            this.stop();
            return;
         }

         this.setupTuneHitsMap();
         this.stage = Notebot.Stage.Tune;
      } else if (this.stage == Notebot.Stage.Tune) {
         this.tune();
      } else if (this.stage == Notebot.Stage.Playing) {
         if (!this.isPlaying) {
            return;
         }

         if (this.mc.player == null || this.currentTick > this.song.getLastTick()) {
            this.onSongEnd();
            return;
         }

         if (this.song.getNotesMap().containsKey(this.currentTick)) {
            if (this.playingMode == Notebot.PlayingMode.Preview) {
               this.onTickPreview();
            } else {
               if (this.mc.player.getAbilities().instabuild) {
                  this.error("You need to be in survival mode.");
                  this.stop();
                  return;
               }

               this.onTickPlay();
            }
         }

         this.currentTick++;
         this.updateStatus();
      }
   }

   private void setupNoteblocksMap() {
      this.noteBlockPositions.clear();
      List<Note> uniqueNotesToUse = new ArrayList<>(this.song.getRequirements());
      Map<NoteBlockInstrument, List<BlockPos>> incorrectNoteBlocks = new HashMap<>();

      for (Entry<Note, Collection<BlockPos>> entry : this.scannedNoteblocks.asMap().entrySet()) {
         Note note = entry.getKey();
         List<BlockPos> noteblocks = new ArrayList<>(entry.getValue());
         if (uniqueNotesToUse.contains(note)) {
            this.noteBlockPositions.put(note, noteblocks.removeFirst());
            uniqueNotesToUse.remove(note);
         }

         if (!noteblocks.isEmpty()) {
            if (!incorrectNoteBlocks.containsKey(note.getInstrument())) {
               incorrectNoteBlocks.put(note.getInstrument(), new ArrayList<>());
            }

            incorrectNoteBlocks.get(note.getInstrument()).addAll(noteblocks);
         }
      }

      for (Entry<NoteBlockInstrument, List<BlockPos>> entry : incorrectNoteBlocks.entrySet()) {
         List<BlockPos> positions = entry.getValue();
         if (this.mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
            NoteBlockInstrument inst = entry.getKey();
            List<Note> foundNotes = uniqueNotesToUse.stream().filter(notex -> notex.getInstrument() == inst).collect(Collectors.toList());
            if (!foundNotes.isEmpty()) {
               for (BlockPos pos : positions) {
                  if (foundNotes.isEmpty()) {
                     break;
                  }

                  Note note = foundNotes.removeFirst();
                  this.noteBlockPositions.put(note, pos);
                  uniqueNotesToUse.remove(note);
               }
            }
         } else {
            for (BlockPos pos : positions) {
               if (uniqueNotesToUse.isEmpty()) {
                  break;
               }

               Note note = uniqueNotesToUse.removeFirst();
               this.noteBlockPositions.put(note, pos);
            }
         }
      }

      if (!uniqueNotesToUse.isEmpty()) {
         for (Note note : uniqueNotesToUse) {
            this.warning("Missing note: " + note.getInstrument() + ", " + note.getNoteLevel());
         }

         this.warning(uniqueNotesToUse.size() + " missing notes!");
      }
   }

   private void setupTuneHitsMap() {
      this.tuneHits.clear();

      for (Entry<Note, BlockPos> entry : this.noteBlockPositions.entrySet()) {
         int targetLevel = entry.getKey().getNoteLevel();
         BlockPos blockPos = entry.getValue();
         BlockState blockState = this.mc.level.getBlockState(blockPos);
         if (blockState.getBlock() == Blocks.NOTE_BLOCK) {
            int currentLevel = blockState.getValue(NoteBlock.NOTE);
            if (targetLevel != currentLevel) {
               this.tuneHits.put(blockPos, calcNumberOfHits(currentLevel, targetLevel));
            }
         }
      }
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      WTable table = theme.table();
      WButton openSongGUI = table.add(theme.button("Open Song GUI")).expandX().minWidth(100.0).widget();
      openSongGUI.action = () -> this.mc.setScreen(theme.notebotSongs());
      table.row();
      WButton alignCenter = table.add(theme.button("Align Center")).expandX().minWidth(100.0).widget();
      alignCenter.action = () -> {
         if (this.mc.player != null) {
            Vec3 pos = Vec3.atBottomCenterOf(this.mc.player.blockPosition());
            this.mc.player.setPos(pos.x, this.mc.player.getY(), pos.z);
         }
      };
      table.row();
      this.status = table.add(theme.label(this.getStatus())).expandCellX().widget();
      WButton pause = table.add(theme.button(this.isPlaying ? "Pause" : "Resume")).right().widget();
      pause.action = () -> {
         this.pause();
         pause.set(this.isPlaying ? "Pause" : "Resume");
         this.updateStatus();
      };
      WButton stop = table.add(theme.button("Stop")).right().widget();
      stop.action = this::stop;
      return table;
   }

   public String getStatus() {
      if (!this.isActive()) {
         return "Module disabled.";
      } else if (this.song == null) {
         return "No song loaded.";
      } else if (this.isPlaying) {
         return String.format("Playing song. %d/%d", this.currentTick, this.song.getLastTick());
      } else if (this.stage == Notebot.Stage.Playing) {
         return "Ready to play.";
      } else {
         return this.stage != Notebot.Stage.SetUp && this.stage != Notebot.Stage.Tune && this.stage != Notebot.Stage.WaitingToCheckNoteblocks
            ? String.format("Stage: %s.", this.stage.toString())
            : "Setting up the noteblocks.";
      }
   }

   public void play() {
      if (this.mc.player != null) {
         if (this.mc.player.getAbilities().instabuild && this.playingMode != Notebot.PlayingMode.Preview) {
            this.error("You need to be in survival mode.");
         } else if (this.stage == Notebot.Stage.Playing) {
            this.isPlaying = true;
            this.info("Playing.");
         } else {
            this.error("No song loaded.");
         }
      }
   }

   public void pause() {
      this.enable();
      if (this.isPlaying) {
         this.info("Pausing.");
         this.isPlaying = false;
      } else {
         this.info("Resuming.");
         this.isPlaying = true;
      }
   }

   public void stop() {
      this.info("Stopping.");
      this.disableNotebot();
      this.updateStatus();
   }

   public void onSongEnd() {
      if (this.autoPlay.get() && this.playingMode != Notebot.PlayingMode.Preview) {
         this.playRandomSong();
      } else {
         this.stop();
      }
   }

   public void playRandomSong() {
      File[] files = MeteorClient.FOLDER.toPath().resolve("notebot").toFile().listFiles();
      if (files != null) {
         File randomSong = files[ThreadLocalRandom.current().nextInt(files.length)];
         if (SongDecoders.hasDecoder(randomSong)) {
            this.loadSong(randomSong);
         } else {
            this.playRandomSong();
         }
      }
   }

   public void disableNotebot() {
      this.resetVariables();
      this.enable();
   }

   public void loadSong(File file) {
      this.enable();
      this.resetVariables();
      this.playingMode = Notebot.PlayingMode.Noteblocks;
      if (!this.loadFileToMap(file, () -> this.stage = Notebot.Stage.SetUp)) {
         this.onSongEnd();
      } else {
         this.updateStatus();
      }
   }

   public void previewSong(File file) {
      this.enable();
      this.resetVariables();
      this.playingMode = Notebot.PlayingMode.Preview;
      this.loadFileToMap(file, () -> {
         this.stage = Notebot.Stage.Playing;
         this.play();
      });
      this.updateStatus();
   }

   public boolean loadFileToMap(File file, Runnable callback) {
      if (!file.exists() || !file.isFile()) {
         this.error("File not found");
         return false;
      } else if (!SongDecoders.hasDecoder(file)) {
         this.error("File is in wrong format. Decoder not found.");
         return false;
      } else {
         this.info("Loading song \"%s\".", FilenameUtils.getBaseName(file.getName()));
         this.loadingSongFuture = CompletableFuture.supplyAsync(() -> {
            try {
               return SongDecoders.parse(file);
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         });
         this.loadingSongFuture.completeOnTimeout(null, 60L, TimeUnit.SECONDS);
         this.stage = Notebot.Stage.LoadingSong;
         long time1 = System.currentTimeMillis();
         this.loadingSongFuture.whenComplete((song, ex) -> {
            if (ex == null) {
               if (song == null) {
                  this.error("Loading song '" + FilenameUtils.getBaseName(file.getName()) + "' timed out.");
                  this.onSongEnd();
                  return;
               }

               this.song = song;
               long time2 = System.currentTimeMillis();
               long diff = time2 - time1;
               this.info("Song '" + FilenameUtils.getBaseName(file.getName()) + "' has been loaded to the memory! Took " + diff + "ms");
               callback.run();
            } else if (ex instanceof CancellationException) {
               this.error("Loading song '" + FilenameUtils.getBaseName(file.getName()) + "' was cancelled.");
            } else {
               this.error("An error occurred while loading song '" + FilenameUtils.getBaseName(file.getName()) + "'. See the logs for more details");
               MeteorClient.LOG.error("An error occurred while loading song '{}'", FilenameUtils.getBaseName(file.getName()), ex);
               this.onSongEnd();
            }
         });
         return true;
      }
   }

   private void scanForNoteblocks() {
      if (this.mc.gameMode != null && this.mc.level != null && this.mc.player != null) {
         this.scannedNoteblocks.clear();
         int min = (int)(-this.mc.player.blockInteractionRange()) - 2;
         int max = (int)this.mc.player.blockInteractionRange() + 2;

         for (int y = min; y < max; y++) {
            for (int x = min; x < max; x++) {
               for (int z = min; z < max; z++) {
                  BlockPos pos = this.mc.player.blockPosition().offset(x, y + 1, z);
                  BlockState blockState = this.mc.level.getBlockState(pos);
                  if (blockState.getBlock() == Blocks.NOTE_BLOCK && this.mc.player.isWithinBlockInteractionRange(pos, 1.0) && this.isValidScanSpot(pos)) {
                     Note note = NotebotUtils.getNoteFromNoteBlock(
                        blockState, pos, this.mode.get(), this.instrumentDetectMode.get().getInstrumentDetectFunction()
                     );
                     this.scannedNoteblocks.put(note, pos);
                  }
               }
            }
         }
      }
   }

   private void onTickPreview() {
      for (Note note : this.song.getNotesMap().get(this.currentTick)) {
         if (this.mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
            this.mc.player.playSound(note.getInstrument().getSoundEvent().value(), 2.0F, (float)Math.pow(2.0, (note.getNoteLevel() - 12) / 12.0));
         } else {
            this.mc.player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 2.0F, (float)Math.pow(2.0, (note.getNoteLevel() - 12) / 12.0));
         }
      }
   }

   private void tune() {
      if (this.tuneHits.isEmpty()) {
         if (this.anyNoteblockTuned) {
            this.anyNoteblockTuned = false;
            this.waitTicks = this.checkNoteblocksAgainDelay.get();
            this.stage = Notebot.Stage.WaitingToCheckNoteblocks;
            this.info("Delaying check for noteblocks");
         } else {
            this.stage = Notebot.Stage.Playing;
            this.info("Loading done.");
            this.play();
         }
      } else if (this.ticks >= this.tickDelay.get()) {
         this.tuneBlocks();
         this.ticks = 0;
      }
   }

   private void tuneBlocks() {
      if (this.mc.level == null || this.mc.player == null) {
         this.disableNotebot();
      }

      if (this.swingArm.get()) {
         this.mc.player.swing(InteractionHand.MAIN_HAND);
      }

      int iterations = 0;
      Iterator<Entry<BlockPos, Integer>> iterator = this.tuneHits.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<BlockPos, Integer> entry = iterator.next();
         BlockPos pos = entry.getKey();
         int hitsNumber = entry.getValue();
         if (this.autoRotate.get()) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> this.tuneNoteblockWithPackets(pos));
         } else {
            this.tuneNoteblockWithPackets(pos);
         }

         this.clickedBlocks.add(pos);
         entry.setValue(--hitsNumber);
         if (hitsNumber == 0) {
            iterator.remove();
         }

         if (++iterations == this.concurrentTuneBlocks.get()) {
            return;
         }
      }
   }

   private void tuneNoteblockWithPackets(BlockPos pos) {
      this.mc
         .gameMode
         .startPrediction(
            this.mc.level,
            sequence -> new ServerboundUseItemOnPacket(
               InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false), sequence
            )
         );
      this.anyNoteblockTuned = true;
   }

   public void updateStatus() {
      if (this.status != null) {
         this.status.set(this.getStatus());
      }
   }

   private static int calcNumberOfHits(int from, int to) {
      return from > to ? 25 - from + to : to - from;
   }

   private void onTickPlay() {
      Collection<Note> notes = this.song.getNotesMap().get(this.currentTick);
      if (!notes.isEmpty()) {
         if (this.autoRotate.get()) {
            Optional<Note> firstNote = notes.stream().findFirst();
            if (firstNote.isPresent()) {
               BlockPos firstPos = this.noteBlockPositions.get(firstNote.get());
               if (firstPos != null) {
                  Rotations.rotate(Rotations.getYaw(firstPos), Rotations.getPitch(firstPos));
               }
            }
         }

         if (this.swingArm.get()) {
            this.mc.player.swing(InteractionHand.MAIN_HAND);
         }

         for (Note note : notes) {
            BlockPos pos = this.noteBlockPositions.get(note);
            if (pos == null) {
               return;
            }

            if (this.polyphonic.get()) {
               this.playRotate(pos);
            } else {
               this.playRotate(pos);
            }
         }
      }
   }

   private void playRotate(BlockPos pos) {
      if (this.mc.gameMode != null) {
         try {
            this.mc
               .gameMode
               .startPrediction(this.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, Direction.DOWN, sequence));
         } catch (NullPointerException var3) {
         }
      }
   }

   private boolean isValidScanSpot(BlockPos pos) {
      return this.mc.level.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK ? false : this.mc.level.getBlockState(pos.above()).isAir();
   }

   @Nullable
   public NoteBlockInstrument getMappedInstrument(@NotNull NoteBlockInstrument inst) {
      if (this.mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
         NotebotUtils.OptionalInstrument optionalInstrument = (NotebotUtils.OptionalInstrument)this.sgNoteMap.getByIndex(inst.ordinal()).get();
         return optionalInstrument.toMinecraftInstrument();
      } else {
         return inst;
      }
   }

   private String beautifyText(String text) {
      text = text.toLowerCase(Locale.ROOT);
      String[] arr = text.split("_");
      StringBuilder sb = new StringBuilder();

      for (String s : arr) {
         sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
      }

      return sb.toString().trim();
   }

   public enum PlayingMode {
      None,
      Preview,
      Noteblocks;
   }

   public enum Stage {
      None,
      LoadingSong,
      SetUp,
      Tune,
      WaitingToCheckNoteblocks,
      Playing;
   }
}
