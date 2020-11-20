package bayern.steinbrecher.javaUtility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

public final class IOUtility {
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private IOUtility() {
        throw new UnsupportedOperationException("The construction of instances is prohibited");
    }

    public static String readAll(InputStream inputStream, Charset charset) throws IOException {
        StringBuilder output = new StringBuilder();
        try (ReadableByteChannel rbc = Channels.newChannel(inputStream)) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
            while (rbc.read(byteBuffer) != -1) {
                byteBuffer.flip();
                output.append(charset.decode(byteBuffer));
                byteBuffer.clear();
            }
        }
        return output.toString();
    }
}
