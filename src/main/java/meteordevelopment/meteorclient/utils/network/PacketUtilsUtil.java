package meteordevelopment.meteorclient.utils.network;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.minecraft.network.protocol.Packet;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;

public class PacketUtilsUtil {
   private PacketUtilsUtil() {
   }

   static void main() {
      try {
         init();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public static void init() throws IOException {
      Comparator<Class<?>> cmp = Comparator.comparing((Class<?> type) -> type.getSimpleName()).thenComparing(type -> type.getName());
      Reflections reflections = new Reflections("net.minecraft.network.protocol", new Scanner[]{Scanners.SubTypes});
      Set<Class<? extends Packet>> all = reflections.getSubTypesOf(Packet.class);
      SortedSet<Class<? extends Packet>> s2c = new TreeSet<>(cmp);
      SortedSet<Class<? extends Packet>> c2s = new TreeSet<>(cmp);

      for (Class<? extends Packet> packet : all) {
         if (!packet.isInterface() && !Modifier.isAbstract(packet.getModifiers())) {
            Class<?> namingClass = packet.getEnclosingClass() != null ? packet.getEnclosingClass() : packet;
            String simpleName = namingClass.getSimpleName();
            if (simpleName.startsWith("Clientbound")) {
               s2c.add(packet);
            } else if (simpleName.startsWith("Serverbound")) {
               c2s.add(packet);
            } else if (simpleName.startsWith("Client")) {
               c2s.add(packet);
            } else if (simpleName.startsWith("Server")) {
               s2c.add(packet);
            } else {
               System.err.printf("WARNING: Skipping unclassified packet: %s%n", packet.getName());
            }
         }
      }

      File file = new File("src/main/java/%s/PacketUtils.java".formatted(PacketUtilsUtil.class.getPackageName().replace('.', '/')));
      if (!file.exists()) {
         file.getParentFile().mkdirs();
         file.createNewFile();
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
         writer.write("/*\n");
         writer.write(" * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).\n");
         writer.write(" * Copyright (c) Meteor Development.\n");
         writer.write(" */\n\n");
         writer.write("package meteordevelopment.meteorclient.utils.network;\n\n");
         writer.write("import com.google.common.collect.Sets;\n");
         writer.write("import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;\n");
         writer.write("import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;\n");
         writer.write("import net.minecraft.network.protocol.Packet;\n\n");
         writer.write("import java.util.Map;\n");
         writer.write("import java.util.Set;\n");
         writer.write("\npublic class PacketUtils {\n");
         writer.write("    private static final Map<Class<? extends Packet<?>>, String> S2C_PACKETS = new Reference2ObjectOpenHashMap<>();\n");
         writer.write("    private static final Map<Class<? extends Packet<?>>, String> C2S_PACKETS = new Reference2ObjectOpenHashMap<>();\n\n");
         writer.write("    private static final Map<String, Class<? extends Packet<?>>> S2C_PACKETS_R = new Object2ReferenceOpenHashMap<>();\n");
         writer.write("    private static final Map<String, Class<? extends Packet<?>>> C2S_PACKETS_R = new Object2ReferenceOpenHashMap<>();\n\n");
         writer.write("    public static final Set<Class<? extends Packet<?>>> PACKETS = Sets.union(getC2SPackets(), getS2CPackets());\n\n");
         writer.write("    static {\n");
         writePacketEntries(writer, c2s, "C2S_PACKETS", "C2S_PACKETS_R");
         writer.newLine();
         writePacketEntries(writer, s2c, "S2C_PACKETS", "S2C_PACKETS_R");
         writer.write("    }\n\n");
         writer.write("    private PacketUtils() {\n");
         writer.write("    }\n\n");
         writer.write("    public static String getName(Class<? extends Packet<?>> packetClass) {\n");
         writer.write("        String name = S2C_PACKETS.get(packetClass);\n");
         writer.write("        if (name != null) return name;\n");
         writer.write("        return C2S_PACKETS.get(packetClass);\n");
         writer.write("    }\n\n");
         writer.write("    public static Class<? extends Packet<?>> getPacket(String name) {\n");
         writer.write("        Class<? extends Packet<?>> packet = S2C_PACKETS_R.get(name);\n");
         writer.write("        if (packet != null) return packet;\n");
         writer.write("        return C2S_PACKETS_R.get(name);\n");
         writer.write("    }\n\n");
         writer.write("    public static Set<Class<? extends Packet<?>>> getS2CPackets() {\n");
         writer.write("        return S2C_PACKETS.keySet();\n");
         writer.write("    }\n\n");
         writer.write("    public static Set<Class<? extends Packet<?>>> getC2SPackets() {\n");
         writer.write("        return C2S_PACKETS.keySet();\n");
         writer.write("    }\n");
         writer.write("}\n");
      }
   }

   private static void writePacketEntries(BufferedWriter writer, SortedSet<Class<? extends Packet>> packets, String mapName, String reverseMapName) throws IOException {
      for (Class<?> packet : packets) {
         String fullName = packet.getName().replace('$', '.');
         String simpleName = packet.getEnclosingClass() != null
            ? packet.getEnclosingClass().getSimpleName() + "." + packet.getSimpleName()
            : packet.getSimpleName();
         writer.write("        %s.put(%s.class, \"%s\");%n".formatted(mapName, fullName, simpleName));
         writer.write("        %s.put(\"%s\", %s.class);%n".formatted(reverseMapName, simpleName, fullName));
      }
   }
}
