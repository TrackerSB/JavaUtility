package bayern.steinbrecher.javaUtility;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Stefan Huber
 */
public final class DialogUtility {

    /**
     * Width/height of the alert type identifying icon.
     */
    public static final int SIZE = 15;
    private static final Logger LOGGER = Logger.getLogger(DialogUtility.class.getName());
    private static final ResourceBundle RESOURCE_BUNDLE
            = ResourceBundle.getBundle("bayern.steinbrecher.javaUtility.dialog");
    private static final int NUMBER_USED_PARAMETERS = 3;
    private static final Map<AlertType, ImageView> ICONS = Map.of(
            AlertType.CONFIRMATION, loadIcon("checked.png"),
            AlertType.INFORMATION, loadIcon("info.png"),
            AlertType.WARNING, loadIcon("warning.png"),
            AlertType.ERROR, loadIcon("error.png")
    );

    private DialogUtility() {
        throw new UnsupportedOperationException("Construction of an instance is not allowed.");
    }

    @NotNull
    private static ImageView loadIcon(@NotNull String path) {
        Image image = new Image(DialogUtility.class.getResource(path).getPath(), SIZE, SIZE, true, true);
        ImageView imageView = new ImageView(image);
        imageView.setSmooth(true);
        return imageView;
    }

    /**
     * Returns a new {@link Alert}. This method may be called on any {@link Thread}. If it is not called on the FX
     * application thread it passes the creation to the FX application thread and waits for it.
     *
     * @return The newly created {@link Alert}.
     */
    @NotNull
    private static Alert getAlert(@NotNull Callable<Alert> alertCreation) throws DialogCreationException {
        Alert alert;
        if (Platform.isFxApplicationThread()) {
            try {
                alert = alertCreation.call();
            } catch (Exception ex) {
                throw new DialogCreationException(ex);
            }
        } else {
            FutureTask<Alert> alertCreationTask = new FutureTask<>(alertCreation);
            Platform.runLater(alertCreationTask);
            try {
                alert = alertCreationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new DialogCreationException(ex);
            }
        }

        Platform.runLater(() ->alert.setGraphic(ICONS.getOrDefault(alert.getAlertType(), null)));

        return alert;
    }

    /**
     * Creates an {@link Alert} with given settings.
     *
     * @param alertType The type of the alert.
     * @param args      The arguments containing the content, title and the header. NOTE: The order is important. If you
     *                  specify less elements or an element is {@code null} these elements will have the default
     *                  value according to {@link Alert}. If you specify more elements they will be ignored.
     * @return The created {@link Alert}.
     * @throws DialogCreationException Thrown if the creation of the dialog got interrupted on the JavaFX main
     * application thread or if the internal dialog creation process is erroneous.
     */
    @NotNull
    @SuppressWarnings({"fallthrough", "PMD.MissingBreakInSwitch"})
    public static Alert createConfirmationAlert(@NotNull Alert.AlertType alertType, @NotNull String... args)
            throws DialogCreationException {
        for (String arg : args) {
            Objects.requireNonNull(arg);
        }
        if (args.length > NUMBER_USED_PARAMETERS) {
            LOGGER.log(Level.WARNING, "You passed more than {0} parameters. Only the first {0} will be used.",
                    NUMBER_USED_PARAMETERS);
        }

        Alert alert = getAlert(() -> new Alert(alertType));
        int parameterCount = Math.min(args.length, NUMBER_USED_PARAMETERS);
        //CHECKSTYLE.OFF: MagicNumber - The JavaDoc explicitly describes these three possible parameters
        switch (parameterCount) {
        case 3:
            alert.setHeaderText(args[2]);
            //fall-through
        case 2:
            alert.setTitle(args[1]);
            //fall-through
        case 1:
            alert.setContentText(args[0]);
            //fall-through
        case 0:
            //No op
            break;
        default:
            throw new IllegalArgumentException("At most three parameters can be passed. (content, title, header)");
        }
        //CHECKSTYLE.ON: MagicNumber
        return alert;
    }

    @NotNull
    public static Alert createInteractiveAlert(
            @NotNull Alert.AlertType type, @Nullable String message, @NotNull ButtonType... buttons)
            throws DialogCreationException {
        return getAlert(() -> new Alert(type, message, buttons));
    }

    @NotNull
    public static Alert createStacktraceAlert(@NotNull Throwable cause, @NotNull String... args)
            throws DialogCreationException {
        Alert alert = createConfirmationAlert(Alert.AlertType.ERROR, args);

        Label stacktraceLabel = new Label(RESOURCE_BUNDLE.getString("stacktraceLabel"));

        StringWriter stacktrace = new StringWriter();
        PrintWriter stacktracePw = new PrintWriter(stacktrace);
        cause.printStackTrace(stacktracePw);
        TextArea stacktraceArea = new TextArea(stacktrace.toString());
        stacktraceArea.setEditable(false);

        GridPane grid = new GridPane();
        grid.addColumn(0, stacktraceLabel, stacktraceArea);
        GridPane.setHgrow(stacktraceArea, Priority.ALWAYS);
        GridPane.setVgrow(stacktraceArea, Priority.ALWAYS);

        alert.getDialogPane().setExpandableContent(grid);

        return alert;
    }

    @NotNull
    public static Alert createWarningAlert(@NotNull String... args) throws DialogCreationException {
        return createConfirmationAlert(Alert.AlertType.WARNING, args);
    }

    @NotNull
    public static Alert createErrorAlert(@NotNull String... args) throws DialogCreationException {
        return createConfirmationAlert(Alert.AlertType.ERROR, args);
    }

    @NotNull
    public static Alert createInfoAlert(@NotNull String... args) throws DialogCreationException {
        return createConfirmationAlert(Alert.AlertType.INFORMATION, args);
    }

    @NotNull
    public static Alert createMessageAlert(@Nullable String message, @NotNull String... args)
            throws DialogCreationException {
        Alert alert = createConfirmationAlert(Alert.AlertType.INFORMATION, args);

        TextArea messageArea = new TextArea(message);
        messageArea.setEditable(false);
        messageArea.setWrapText(true);

        GridPane grid = new GridPane();
        grid.addColumn(0, messageArea);
        GridPane.setHgrow(messageArea, Priority.ALWAYS);
        GridPane.setVgrow(messageArea, Priority.ALWAYS);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setExpandableContent(grid);
        dialogPane.setExpanded(true);

        return alert;
    }

    @NotNull
    public static Optional<ButtonType> showAndWait(@NotNull Alert alert) {
        Optional<ButtonType> result;
        if (Platform.isFxApplicationThread()) {
            result = alert.showAndWait();
        } else {
            FutureTask<Optional<ButtonType>> showAndWaitTask = new FutureTask<>(alert::showAndWait);
            Platform.runLater(showAndWaitTask);
            try {
                result = showAndWaitTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                result = Optional.empty();
            }
        }
        return result;
    }
}
