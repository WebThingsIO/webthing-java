package io.webthings.webthing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class ValueTest {

    @Test
    public void itKnowsItsBaseTypeAtRuntime()
    {
        Value<Double> doubleNull = new Value<>(Double.class);
        assertEquals(Double.class, doubleNull.getBaseType());
        assertNull(doubleNull.get());

        Value<Double> doubleByValue = new Value<>(42.0123);
        assertEquals(Double.class, doubleNull.getBaseType());
        assertEquals(42.0123, doubleByValue.get(), 0.00001);

        Value<String> stringByValue = new Value<>("my-string-value");
        assertEquals(String.class, stringByValue.getBaseType());
        assertEquals("my-string-value", stringByValue.get());

        Value<String> stringNull = new Value<>(String.class, (String str) -> {});
        assertEquals(String.class, stringByValue.getBaseType());
        assertNull(stringNull.get());

        Value<JSONArray> listByValue = new Value<>(new JSONArray("[1,2,3]"), list -> {});
        assertEquals(JSONArray.class, listByValue.getBaseType());
        assertEquals(Arrays.asList(1, 2, 3), listByValue.get().toList());

        Value<JSONObject> objectExplicit;
        objectExplicit = new Value<>(JSONObject.class, new JSONObject().put("key", "value"), obj -> {});
        assertEquals(JSONObject.class, objectExplicit.getBaseType());
        assertEquals(Collections.singletonMap("key", "value"), objectExplicit.get().toMap());
    }

    @Test
    public void itsBaseTypeIsRequiredAtConstruction()
    {
        NullPointerException ex;
        ex = assertThrows(NullPointerException.class, () -> new Value<Boolean>(null, true, bool -> {}));
        assertEquals("The base type of a value must not be null.", ex.getMessage());
    }
}
