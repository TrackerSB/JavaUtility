package bayern.steinbrecher.javaUtility;

/**
 * @author Stefan Huber
 * @since 0.17
 */
public final class CompareUtility {
    private CompareUtility() {
        throw new UnsupportedOperationException("Construction of instances is prohibited");
    }

    public static <T extends Comparable<T>> T clamp(T value, T min, T max) {
        T clamped;
        if (value.compareTo(min) < 0) {
            clamped = min;
        } else if (value.compareTo(max) > 0) {
            clamped = max;
        } else {
            clamped = value;
        }
        return clamped;
    }
}
