package bayern.steinbrecher.javaUtility;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Stefan Huber
 * @since 0.18
 */
public final class CSVFormat {
    public static final CSVFormat EXCEL = new CSVFormat(';', StandardCharsets.UTF_8, true);
    public static final CSVFormat LIBRE_OFFICE = new CSVFormat(',', StandardCharsets.UTF_8, false);

    private final char separator;
    private final Charset encoding;
    private final boolean withBOM;

    public CSVFormat(char separator, Charset encoding, boolean withBOM) {
        assert encoding == StandardCharsets.UTF_8 || !withBOM : "BOM is only an optional character in case of UTF-8";
        this.separator = separator;
        this.encoding = encoding;
        this.withBOM = withBOM;
    }

    public char getSeparator() {
        return separator;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public boolean isWithBOM() {
        return withBOM;
    }
}
