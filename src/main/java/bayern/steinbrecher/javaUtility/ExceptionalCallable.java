package bayern.steinbrecher.javaUtility;

import org.jetbrains.annotations.Nullable;

/**
 * @author Stefan Huber
 * @since 0.18
 */
@FunctionalInterface
public interface ExceptionalCallable<V, E extends Exception> {
    V callUnsafe() throws E;

    /**
     * This method is a result of Java being unable to catch {@link Exception}s whose class is defined by a generic
     * type. In contrast to {@link #callUnsafe()} this method catches all exception and throws an
     * {@link UnhandledException} in case the {@link ExceptionalCallable} throws an {@link Exception} which it is not
     * supposed to throw.
     *
     * @param exceptionTypeDummy If {@code null} is specified the {@link ExceptionalCallable} is not supposed to throw
     *                           any exception.
     */
    @SuppressWarnings("unchecked")
    default V call(@Nullable Class<E> exceptionTypeDummy) throws E {
        try {
            return callUnsafe();
        } catch (Exception ex) {
            if (exceptionTypeDummy != null
                    && exceptionTypeDummy.isAssignableFrom(ex.getClass())) {
                throw (E) ex;
            } else {
                throw new UnhandledException("The callable yielded an exception which it is not supposed to throw", ex);
            }
        }
    }
}
