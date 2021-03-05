package bayern.steinbrecher.javaUtility;

import bayern.steinbrecher.jsch.ChannelExec;
import javafx.concurrent.Task;
import javafx.util.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public final class IOUtility {
    private static final Logger LOGGER = Logger.getLogger(IOUtility.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private IOUtility() {
        throw new UnsupportedOperationException("The construction of instances is prohibited");
    }

    /**
     * WARNING This method is neither geared to read continues output of interactive shells nor remote shells nor very
     * large outputs. In these cases consider using {@link #readChannelContinuously(ChannelExec, Charset)}. However,
     * this method is way faster in reading than {@link #readChannelContinuously(ChannelExec, Charset)}.
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
     *
     * @return A pair of output and error stream content
     */
    public static Pair<String, String> readChannelContinuously(ChannelExec channel, Charset charset)
            throws IOException {
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
                outputBuffer.append(new String(tmp, 0, i, charset));
            }
            while (errStream.available() > 0) {
                int i = errStream.read(tmp, 0, DEFAULT_BUFFER_SIZE);
                if (i < 0) {
                    break;
                }
                errorBuffer.append(new String(tmp, 0, i, charset));
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

    /**
     * @since 0.18
     */
    public static void writeCSV(Path outputPath, Iterable<? extends Iterable<String>> rowMajorContent, CSVFormat format)
            throws IOException {
        try (Writer writer = new FileWriter(outputPath.toFile())) {
            if (format.isWithBOM()) {
                /* NOTE 2021-03-02
                 * FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF)
                 * See https://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
                 */
                writer.write('\uFEFF');
            }

            for (Iterable<String> row : rowMajorContent) {
                boolean isFirstCell = true;
                for (String cell : row) {
                    if (!isFirstCell) {
                        writer.write(format.getSeparator());
                    }
                    writer.write(cell);
                    isFirstCell = false;
                }
                writer.write('\n');
            }
        }
    }
}
