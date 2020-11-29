package bayern.steinbrecher.javaUtility;

import javafx.concurrent.Task;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IOUtility {
    private static final Logger LOGGER = Logger.getLogger(IOUtility.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private IOUtility() {
        throw new UnsupportedOperationException("The construction of instances is prohibited");
    }

    public static String readAll(InputStream inputStream, Charset charset) throws IOException {
        StringBuilder output = new StringBuilder();
        try (ReadableByteChannel rbc = Channels.newChannel(inputStream)) {
           boolean retrievingData = true;
            while(retrievingData) {
                Task<Optional<CharBuffer>> readTask = new Task<>() {
                    @Override
                    protected Optional<CharBuffer> call() throws Exception {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
                        CharBuffer received;
                        int numReadBytes = rbc.read(byteBuffer);
                        if (numReadBytes > -1) {
                            byteBuffer.flip();
                            received = charset.decode(byteBuffer);
                            byteBuffer.clear();
                        } else {
                            received = null;
                        }
                        return Optional.ofNullable(received);
                    }
                };
                new Thread(readTask)
                        .start();
                try {
                    Optional<CharBuffer> inputChunk = readTask.get(1, TimeUnit.SECONDS);
                    if(inputChunk.isPresent()){
                        output.append(inputChunk.get());
                    } else {
                        retrievingData = false;
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                    LOGGER.log(Level.WARNING, "Stopped reading the input stream", ex);
                    retrievingData = false;
                }
            }
        }
        return output.toString();
    }
}
