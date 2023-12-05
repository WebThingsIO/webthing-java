package io.webthings.webthing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.json.JSONObject;
import org.junit.Test;

import io.webthings.webthing.errors.PropertyError;

public class ThingTest {

    Object simulateHttpPutProperty(String key, String jsonBody) {
        JSONObject json = new JSONObject(jsonBody);
        return json.get(key);
    }

    @Test
    public void ItSupportsIntegralInputForFractionalProperties() throws PropertyError {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<Integer> intValue = new Value<>(456,
            iv -> System.out.println("integer value: " + iv));
        thing.addProperty(new Property<>(thing, "intProp", intValue,
            new JSONObject().put("type", "integer")));

        Value<Double> doubleValue = new Value<>(12.34,
            dv -> System.out.println("doubel value: " + dv));
        thing.addProperty(new Property<>(thing, "doubleProp", doubleValue,
            new JSONObject().put("type", "number")));

        Value<Float> floatValue = new Value<>(4.2f,
            fv -> System.out.println("float value: " + fv));
        thing.addProperty(new Property<>(thing, "floatProp", floatValue,
            new JSONObject().put("type", "number")));

        // when updating integer property, then
        assertEquals(Integer.valueOf(456), intValue.get());

        Exception ex = assertThrows(PropertyError.class, () -> thing.setProperty("intProp", 
            simulateHttpPutProperty("intProp", "{\"intProp\":42.0}")));
        assertEquals(ex.getMessage(), "Invalid property value");
        assertEquals(Integer.valueOf(456), intValue.get());

        thing.setProperty("intProp", 
            simulateHttpPutProperty("intProp", "{\"intProp\":24}"));
        assertEquals(Integer.valueOf(24), intValue.get());

        // when updating double property, then
        assertEquals(12.34, doubleValue.get(), 0.00001);

        thing.setProperty("doubleProp", 
            simulateHttpPutProperty("doubleProp", "{\"doubleProp\":42.0}"));
        assertEquals(42.0, doubleValue.get(), 0.00001);

        thing.setProperty("doubleProp", 
            simulateHttpPutProperty("doubleProp", "{\"doubleProp\":24}"));
        assertEquals(24.0, doubleValue.get(), 0.00001);

        // when updating float property, then
        assertEquals(4.2f, floatValue.get(), 0.00001);

        thing.setProperty("floatProp", 
            simulateHttpPutProperty("floatProp", "{\"floatProp\":2.4}"));
        assertEquals(42.0f, floatValue.get(), 0.00001);

        thing.setProperty("floatProp", 
            simulateHttpPutProperty("floatProp", "{\"floatProp\":4}"));
        assertEquals(4.0f, floatValue.get(), 0.00001);
    }
}
