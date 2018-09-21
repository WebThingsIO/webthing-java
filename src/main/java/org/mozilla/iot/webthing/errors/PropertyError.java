package org.mozilla.iot.webthing.errors;

public class PropertyError extends Exception {
    public PropertyError() {
        super("General property error");
    }

    public PropertyError(String message) {
        super(message);
    }
}
