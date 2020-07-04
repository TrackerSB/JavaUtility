package bayern.steinbrecher.utility;

public class DialogCreationException extends Exception {
    public DialogCreationException() {
        super();
    }

    public DialogCreationException(String message) {
        super(message);
    }

    public DialogCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DialogCreationException(Throwable cause) {
        super(cause);
    }
}
