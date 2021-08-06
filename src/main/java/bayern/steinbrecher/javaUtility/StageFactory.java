package bayern.steinbrecher.javaUtility;

import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Stefan Huber
 * @since 0.18
 */
public final class StageFactory {
    private final Image defaultIcon;
    private final Modality defaultModality;
    private final StageStyle defaultStageStyle;
    private final Window defaultOwner;

    public StageFactory(@NotNull Modality defaultModality, @NotNull StageStyle defaultStageStyle,
                        @Nullable Image defaultIcon, @Nullable Window defaultOwner) {
        this.defaultIcon = defaultIcon;
        this.defaultModality = defaultModality;
        this.defaultStageStyle = defaultStageStyle;
        this.defaultOwner = defaultOwner;
    }

    @NotNull
    public Stage create() {
        Stage stage = PlatformUtility.runLaterBlocking(Stage::new, null);
        stage.initOwner(defaultOwner);
        stage.initModality(defaultModality);
        stage.initStyle(defaultStageStyle);
        if (defaultIcon != null) {
            stage.getIcons()
                    .add(defaultIcon);
        }
        return stage;
    }
}
