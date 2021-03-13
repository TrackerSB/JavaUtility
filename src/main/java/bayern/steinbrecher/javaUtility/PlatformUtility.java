package bayern.steinbrecher.javaUtility;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Stefan Huber
 * @since 0.18
 */
public final class PlatformUtility {
    private PlatformUtility() {
        throw new UnsupportedOperationException("The construction of instances is prohibited");
    }

    public static <R> R runLaterBlocking(Callable<R> task) throws Exception {
        AtomicReference<R> result = new AtomicReference<>();
        if (Platform.isFxApplicationThread()) {
            result.set(task.call());
        } else {
            CountDownLatch countDown = new CountDownLatch(1);
            AtomicReference<Exception> exception = new AtomicReference<>();
            Platform.runLater(() -> {
                try {
                    result.set(task.call());
                } catch (Exception ex) {
                    exception.set(ex);
                } finally {
                    countDown.countDown();
                }
            });
            countDown.await();
            if (exception.get() != null) {
                throw exception.get();
            }
        }
        return result.get();
    }

    /**
     * Ensure that the tasks {@code preNonFxTask} and {@code postNonFxTask} do not run on the JavaFXApplication thread
     * and ensure that the {@code fxTask} runs on the JavaFXApplication thread.
     * This function serves the purpose of running as view commands as possible on the JavaFXApplication thread to
     * ensure that all GUI components remain responsive.
     * This function can be run on any thread.
     */
    public static <R, S> void runLaterThriftily(
            Callable<R> preNonFxTask, Function<R, S> fxTask, Consumer<S> postNonFxTask) throws Exception {
        AtomicReference<Exception> exception = new AtomicReference<>();
        Runnable nonFxTask = () -> {
            try {
                R preTaskResult = preNonFxTask.call();
                S fxTaskResult = runLaterBlocking(() -> fxTask.apply(preTaskResult));
                postNonFxTask.accept(fxTaskResult);
            } catch (Exception ex) {
                exception.set(ex);
            }
        };
        if (Platform.isFxApplicationThread()) {
            new Thread(nonFxTask)
                    .start();
        } else {
            nonFxTask.run();
        }
        if (exception.get() != null) {
            throw exception.get();
        }
    }
}
