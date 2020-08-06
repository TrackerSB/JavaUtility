package bayern.steinbrecher.javaUtility;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a {@link HashMap} which creates an empty entry fro a key whenever it is accessed over
 * {@link HashMap#get(java.lang.Object)}.
 *
 * @author Stefan Huber
 * @since 0.1
 * @param <K> The type of the key values.
 * @param <V> The type of values.
 */
public class SupplyingMap<K, V> extends HashMap<K, V> {

    private final Function<K, V> entrySupplier;

    /**
     * Creates a {@link SupplyingMap} which generates empty entries when accessing them using the passed
     * {@link Function}.
     *
     * @param entrySupplier The supplier for generating new empty entries. Its input is the key to generate an empty
     * entry for.
     */
    public SupplyingMap(Function<K, V> entrySupplier) {
        super();
        this.entrySupplier
                = Objects.requireNonNull(entrySupplier, "The function generating empty entries must not be null.");
    }

    /**
     * Returns the value associated with the given key or generates an empty entry, associates it with the key and
     * returns it. This method returns {@code null} if and only if the {@link Function} for generating empty entries
     * returns {@code null}.
     *
     * @param key The key to get an associated value for.
     * @return The value assocated with the given key.
     * @throws ClassCastException Thrown only if no value is associated with the key and the key is not of type
     * {@link K}.
     * @see #getEntrySupplier()
     * @see HashMap#get(java.lang.Object)
     * @see HashMap#containsKey(java.lang.Object)
     */
    @Override
    @SuppressWarnings({"element-type-mismatch", "unchecked"})
    public V get(Object key) {
        if (!containsKey(key)) {
            K keyK = (K) key;
            put(keyK, entrySupplier.apply(keyK));
        }
        return super.get(key);
    }

    /**
     * Returns the {@link Function} used for generating new empty entries.
     *
     * @return The {@link Function} used for generating new empty entries. Its input is the key to generate an empty
     * entry for.I
     */
    public Function<K, V> getEntrySupplier() {
        return entrySupplier;
    }
}
