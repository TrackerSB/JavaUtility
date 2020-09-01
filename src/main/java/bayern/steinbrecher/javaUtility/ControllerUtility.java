package bayern.steinbrecher.javaUtility;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for tasks typically required by JavaFX controller classes.
 *
 * @author Stefan Huber
 * @since 0.5
 */
public final class ControllerUtility {
    private static final Logger LOGGER = Logger.getLogger(ControllerUtility.class.getName());

    @NotNull
    public static Optional<Stage> determineStage(@NotNull Node element) {
        Optional<Stage> stage;
        Scene scene = element.getScene();
        if (scene == null) {
            LOGGER.log(
                    Level.FINE,
                    "Could not determine containing stage since the given element is not attached to any scene."
            );
            stage = Optional.empty();
        } else {
            Window window = scene.getWindow();
            if (window == null) {
                LOGGER.log(Level.FINE, "Could not determine containing stage since the scene to which the element is "
                        + "attached to is not embedded into a window.");
                stage = Optional.empty();
            } else {
                if (window instanceof Stage) {
                    stage = Optional.of((Stage) window);
                } else {
                    LOGGER.log(Level.FINE, "Could not determine containing stage since the given element is not "
                            + "attached to any window.");
                    stage = Optional.empty();
                }
            }
        }
        return stage;
    }
}
