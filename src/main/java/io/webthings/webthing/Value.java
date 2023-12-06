package io.webthings.webthing;

import java.util.Objects;
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

    static class BaseTypeHelper
    {
        static <BT> Class<BT> derive(final BT initialValue) {
            Objects.requireNonNull(initialValue, "Can not derive base type from null.");
            return (Class<BT>) initialValue.getClass();
        }
    }

    private final Consumer<T> valueForwarder;
    private final Class<T> baseType;
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
        this(BaseTypeHelper.derive(initialValue), initialValue, null);
    }
    
    /**
     * Create a read only value that can only be updated by a Thing's reading.
     * Initial value will be set to null.
     * <p>
     * Example: A sensor is updating its reading, but the reading cannot be set
     * externally.
     *
     * @param baseType The Class of the values base type
     */
    public Value(final Class<T> baseType) {
        this(baseType, null, null);
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
        this(BaseTypeHelper.derive(initialValue), initialValue, valueForwarder);
    }

    /**
     * Create a writable value that can be set to a new value.
     * Initial value will be set to null.
     * <p>
     * Example: A light that can be switched off by setting this to false.
     *
     * @param baseType       The Class of the values base type
     * @param valueForwarder The method that updates the actual value on the
     *                       thing
     */
    public Value(final Class<T> baseType, final Consumer<T> valueForwarder) {
        this(baseType, null, valueForwarder);
    }

    /**
     * Create a writable value that can be set to a new value.
     * <p>
     * Example: A light that can be switched off by setting this to false.
     *
     * @param baseType       The Class of the values base type
     * @param initialValue   The initial value
     * @param valueForwarder The method that updates the actual value on the
     *                       thing
     */
    public Value(final Class<T> baseType, final T initialValue, final Consumer<T> valueForwarder) {
        Objects.requireNonNull(baseType, "The base type of a value must not be null.");
        this.baseType = baseType;
        this.lastValue = initialValue;
        this.valueForwarder = valueForwarder;
    }

    /**
     * Get the base type of this value.
     *
     * @return The base type.
     */
    public Class<T> getBaseType()
    {
        return this.baseType;
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
