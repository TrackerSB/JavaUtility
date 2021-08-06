package bayern.steinbrecher.javaUtility;

import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

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

    @SuppressWarnings("unchecked")
    public static <R, E extends Exception> R runLaterBlocking(
            @NotNull ExceptionalCallable<R, E> task, @NotNull Class<E> exceptionTypeDummy) throws E {
        AtomicReference<R> result = new AtomicReference<>();
        if (Platform.isFxApplicationThread()) {
            result.set(task.call(exceptionTypeDummy));
        } else {
            CountDownLatch countDown = new CountDownLatch(1);
            AtomicReference<E> exception = new AtomicReference<>();
            Platform.runLater(() -> {
                try {
                    result.set(task.call(exceptionTypeDummy));
                } catch (Exception ex) {
                    exception.set((E) ex);
                } finally {
                    countDown.countDown();
                }
            });
            try {
                countDown.await();
            } catch (InterruptedException ex) {
                throw new UnhandledException(ex);
            }
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
    @SuppressWarnings("unchecked")
    public static <R, S, E extends Exception> void runLaterThriftily(
            @NotNull ExceptionalCallable<R, E> preNonFxTask, @NotNull Function<R, S> fxTask,
            @NotNull Consumer<S> postNonFxTask, @NotNull Class<E> exceptionTypeDummy) throws E {
        AtomicReference<E> exception = new AtomicReference<>();
        ExceptionalCallable<Void, E> nonFxTask = () -> {
            R preTaskResult = preNonFxTask.call(exceptionTypeDummy);
            S fxTaskResult = runLaterBlocking(() -> fxTask.apply(preTaskResult), exceptionTypeDummy);
            postNonFxTask.accept(fxTaskResult);
            return null;
        };
        if (Platform.isFxApplicationThread()) {
            new Thread(() -> {
                try {
                    nonFxTask.call(exceptionTypeDummy);
                } catch (Exception ex) {
                    exception.set((E) ex); // Per design of ExceptionalCallable#call() this cannot be another exception
                }
            }).start();
        } else {
            nonFxTask.call(exceptionTypeDummy);
        }
        if (exception.get() != null) {
            throw exception.get();
        }
    }
}
