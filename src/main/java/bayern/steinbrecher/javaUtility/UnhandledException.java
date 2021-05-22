package bayern.steinbrecher.javaUtility;

/**
 * @author Stefan Huber
 * @since 0.18
 */
public class UnhandledException extends RuntimeException {
    public UnhandledException() {
    }

    public UnhandledException(String message) {
        super(message);
    }

    public UnhandledException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnhandledException(Throwable cause) {
        super(cause);
    }
}
