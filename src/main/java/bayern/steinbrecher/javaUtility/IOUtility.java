package bayern.steinbrecher.javaUtility;

import bayern.steinbrecher.jsch.ChannelExec;
import javafx.concurrent.Task;
import javafx.util.Pair;

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

    /**
     * WARNING This method is neither geared to read continues output of interactive shells nor remote shells nor very
     * large outputs. In these cases consider using {@link #readChannelContinuously(ChannelExec)}. However, this method is
     * way faster in reading than {@link #readChannelContinuously(ChannelExec)}.
     */
    public static String readAll(InputStream inputStream, Charset charset) throws IOException {
        StringBuilder output = new StringBuilder();
        try (ReadableByteChannel rbc = Channels.newChannel(inputStream)) {
            boolean isSecondTimeoutInARow = false;
            boolean retrievingData = true;
            while (retrievingData) {
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
                    Optional<CharBuffer> inputChunk = readTask.get(3, TimeUnit.SECONDS);
                    if (inputChunk.isPresent()) {
                        output.append(inputChunk.get());
                        isSecondTimeoutInARow = false;
                    } else {
                        retrievingData = false;
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.WARNING, "Stopped reading the input stream", ex);
                    retrievingData = false;
                } catch (TimeoutException ex) {
                    if (isSecondTimeoutInARow) {
                        LOGGER.log(Level.WARNING, "Stopped reading the input stream. "
                                + "Input stream doesn't seem to yield further data. "
                                + "Returned data may be incomplete.", ex);
                        retrievingData = false;
                    } else {
                        LOGGER.log(Level.WARNING, "Input stream didn't yield further data yet. Retrying.", ex);
                        isSecondTimeoutInARow = true;
                    }
                }
            }
        }
        return output.toString();
    }

    /**
     * Based on https://stackoverflow.com/a/47554723/4863098
     */
    public static Pair<String, String> readChannelContinuously(ChannelExec channel) throws IOException {
        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder errorBuffer = new StringBuilder();
        InputStream inStream = channel.getInputStream();
        InputStream errStream = channel.getErrStream();
        byte[] tmp = new byte[DEFAULT_BUFFER_SIZE];
        while (true) {
            while (inStream.available() > 0) {
                int i = inStream.read(tmp, 0, DEFAULT_BUFFER_SIZE);
                if (i < 0) {
                    break;
                }
                outputBuffer.append(new String(tmp, 0, i));
            }
            while (errStream.available() > 0) {
                int i = errStream.read(tmp, 0, DEFAULT_BUFFER_SIZE);
                if (i < 0) {
                    break;
                }
                errorBuffer.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if ((inStream.available() > 0) || (errStream.available() > 0)) {
                    continue;
                }
                int exitStatus = channel.getExitStatus();
                if (exitStatus != 0) {
                    LOGGER.log(Level.WARNING, "The channel finished with a non-zero exit state");
                }
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.WARNING, "The delay before the next read iteration was interrupted.", ex);
            }
        }
        return new Pair<>(outputBuffer.toString(), errorBuffer.toString());
    }
}
