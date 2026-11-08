package meteordevelopment.meteorclient.utils.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.IntFunction;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ByteBufferUtils {
   private ByteBufferUtils() {
   }

   public static ByteBuffer readFully(Path path, IntFunction<ByteBuffer> allocator) throws IOException {
      try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
         long size = channel.size();
         if (size > 2147483647L) {
            throw new IOException("File too large to read into ByteBuffer: " + path);
         } else {
            return readFully(channel, (int)size, allocator);
         }
      }
   }

   public static ByteBuffer readFully(ReadableByteChannel channel, IntFunction<ByteBuffer> allocator) throws IOException {
      if (channel instanceof SeekableByteChannel seekableChannel) {
         long size = seekableChannel.size();
         if (size > 2147483647L) {
            throw new IOException("Channel content too large to read into ByteBuffer");
         } else {
            return readFully(seekableChannel, (int)size, allocator);
         }
      } else {
         ByteBuffer buffer = requireCapacity(allocator.apply(8192), 8192);

         while (true) {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
               break;
            }

            if (bytesRead == 0) {
               if (buffer.hasRemaining()) {
                  break;
               }

               buffer = grow(buffer, allocator);
            } else if (!buffer.hasRemaining()) {
               buffer = grow(buffer, allocator);
            }
         }

         buffer.flip();
         return buffer;
      }
   }

   private static ByteBuffer readFully(SeekableByteChannel channel, int size, IntFunction<ByteBuffer> allocator) throws IOException {
      ByteBuffer buffer = requireCapacity(allocator.apply(size), size);

      while (buffer.hasRemaining()) {
         int bytesRead = channel.read(buffer);
         if (bytesRead == -1) {
            break;
         }
      }

      buffer.flip();
      return buffer;
   }

   private static ByteBuffer grow(ByteBuffer buffer, IntFunction<ByteBuffer> allocator) {
      int oldCap = buffer.capacity();
      int newCap = oldCap << 1;
      if (newCap <= 0) {
         throw new OutOfMemoryError("Buffer too large (overflow): " + oldCap);
      }

      ByteBuffer newBuffer = requireCapacity(allocator.apply(newCap), newCap);
      buffer.flip();
      newBuffer.put(buffer);
      return newBuffer;
   }

   private static ByteBuffer requireCapacity(ByteBuffer buf, int minCap) {
      if (buf.capacity() < minCap) {
         throw new IllegalArgumentException("Allocator returned capacity " + buf.capacity() + " < " + minCap);
      } else {
         return buf;
      }
   }
}
