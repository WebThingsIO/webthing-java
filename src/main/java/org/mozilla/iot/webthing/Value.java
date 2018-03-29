package org.mozilla.iot.webthing;

import java.util.Observable;
import java.util.function.Consumer;

/**
 * A Value. Used for communicating between the Thing representation
 * and the actual physical thing implementation.
 *
 * Notifies all observers when the underlying value changes through an
 * external update (command to turn the light off)
 * or if the underlying sensor reports a new value
 * @author Tim Hinkes (timmeey@timmeey.de)
 */
public class Value<T> extends Observable {

    private T lastValue;
    private final Consumer<T> newValue;

    /**
     * Creates a writable value, that can be set to a new value
     * Example: A light that can be switched off by setting this to false
     * @param newValue The method that updates the actual value on the thing
     * @param initialValue the initial value
     */
    public Value(final Consumer<T> newValue, final T initialValue) {
        this.newValue = newValue;
        this.lastValue = initialValue;
    }

    /**
     * Creates a read only value, that can only be updated by a Things reading
     * Example: A sensor is updating its reading, but the reading cannot be set externally
     * @param initialValue
     */
    public Value(final T initialValue) {
        this.newValue = v -> {throw new IllegalArgumentException("Read only value");};
        this.lastValue = initialValue;
    }

    /**
     * Sets a new Value for this thing
     * Example: Switch a light off: set(false)
     * @param value for this
     */
    public final void set(T value) {
        newValue.accept(value);
        this.notifyOfExternalUpdate(value);

    }

    /**
     * Returns the last known value from the underlying thing
     * Example: Returns false, when a light is off
     * @return the last known value for the underlying thing
     */
    public final T get() {
        return this.lastValue;
    }

    /**
     * Called if the underlying thing reported a new value
     * This informs observers about the update
     * Example: A Sensor reports a new value
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

