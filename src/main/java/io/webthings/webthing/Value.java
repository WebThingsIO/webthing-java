package io.webthings.webthing;

import java.util.Observable;
import java.util.function.Consumer;

/**
 * A property value.
 * <p>
 * This is used for communicating between the Thing representation and the
 * actual physical thing implementation.
 * <p>
 * Notifies all observers when the underlying value changes through an external
 * update (command to turn the light off) or if the underlying sensor reports a
 * new value.
 *
 * @author Tim Hinkes (timmeey@timmeey.de)
 */
public class Value<T> extends Observable {
    private final Consumer<T> valueForwarder;
    private T lastValue;

    /**
     * Create a read only value that can only be updated by a Thing's reading.
     * <p>
     * Example: A sensor is updating its reading, but the reading cannot be set
     * externally.
     *
     * @param initialValue The initial value
     */
    public Value(final T initialValue) {
        this(initialValue, null);
    }

    /**
     * Create a writable value that can be set to a new value.
     * <p>
     * Example: A light that can be switched off by setting this to false.
     *
     * @param initialValue   The initial value
     * @param valueForwarder The method that updates the actual value on the
     *                       thing
     */
    public Value(final T initialValue, final Consumer<T> valueForwarder) {
        this.lastValue = initialValue;
        this.valueForwarder = valueForwarder;
    }

    /**
     * Set a new Value for this thing.
     * <p>
     * Example: Switch a light off: set(false)
     *
     * @param value Value to set
     */
    public final void set(T value) {
        if (valueForwarder != null) {
            valueForwarder.accept(value);
        }

        this.notifyOfExternalUpdate(value);
    }

    /**
     * Returns the last known value from the underlying thing.
     * <p>
     * Example: Returns false, when a light is off.
     *
     * @return The value.
     */
    public final T get() {
        return this.lastValue;
    }

    /**
     * Called if the underlying thing reported a new value. This informs
     * observers about the update.
     * <p>
     * Example: A sensor reports a new value.
     *
     * @param value the newly reported value
     */
    public final void notifyOfExternalUpdate(T value) {
        if (value != null && !value.equals(this.lastValue)) {
            this.setChanged();
            this.lastValue = value;
            notifyObservers(value);
        }
    }
}
