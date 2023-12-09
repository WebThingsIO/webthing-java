package io.webthings.webthing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import io.webthings.webthing.errors.PropertyError;

public class ThingTest {

    Object simulateHttpPutProperty(String key, String jsonBody) {
        JSONObject json = new JSONObject(jsonBody);
        return json.get(key);
    }

    @Test
    public void itSupportsIntegerValues() throws PropertyError
    {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<Integer> value = new Value<>(42, v -> System.out.println("value: " + v));
        thing.addProperty(new Property<>(thing, "p", value, new JSONObject().put("type", "integer")));
        
        // when updating property, then
        assertEquals(42, value.get().intValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":"+Integer.MIN_VALUE+"}"));
        assertEquals(Integer.MIN_VALUE, value.get().intValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":"+Integer.MAX_VALUE+"}"));
        assertEquals(Integer.MAX_VALUE, value.get().intValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":4.2}"));
        assertEquals(4, value.get().intValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":4}"));
        assertEquals(4, value.get().intValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":0}"));
        assertEquals(0, value.get().intValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":-123}"));
        assertEquals(-123, value.get().intValue());
    }

    @Test
    public void itSupportsLongValues() throws PropertyError
    {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<Long> value = new Value<>(42l, v -> System.out.println("value: " + v));
        thing.addProperty(new Property<>(thing, "p", value, new JSONObject().put("type", "integer")));
        
        // when updating property, then
        assertEquals(42, value.get().longValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":"+Long.MIN_VALUE+"}"));
        assertEquals(Long.MIN_VALUE, value.get().longValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":"+Long.MAX_VALUE+"}"));
        assertEquals(Long.MAX_VALUE, value.get().longValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":4.2}"));
        assertEquals(4, value.get().longValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":4}"));
        assertEquals(4, value.get().longValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":0}"));
        assertEquals(0, value.get().longValue());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":-123}"));
        assertEquals(-123, value.get().longValue());
    }

    @Test
    public void itSupportsFloatValues() throws PropertyError
    {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<Float> value = new Value<>(42.0123f, v -> System.out.println("value: " + v));
        thing.addProperty(new Property<>(thing, "p", value,
            new JSONObject().put("type", "number")));
        
        // when updating property, then
        assertEquals(42.0123f, value.get().floatValue(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":"+Float.MIN_VALUE+"}"));
        assertEquals(Float.MIN_VALUE, value.get().floatValue(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":"+Float.MAX_VALUE+"}"));
        assertEquals(Float.MAX_VALUE, value.get().floatValue(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":4.2}"));
        assertEquals(4.2f, value.get().floatValue(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":4}"));
        assertEquals(4f, value.get().floatValue(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":0}"));
        assertEquals(0f, value.get().floatValue(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":-123.456}"));
        assertEquals(-123.456f, value.get().floatValue(), 0.00001);
    }

    @Test
    public void itSupportsDoubleValues() throws PropertyError
    {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<Double> value = new Value<>(42.0123, v -> System.out.println("value: " + v));
        thing.addProperty(new Property<>(thing, "p", value, new JSONObject().put("type", "number")));
        
        // when updating property, then
        assertEquals(42.0123, value.get(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":"+Double.MIN_VALUE+"}"));
        assertEquals(Double.MIN_VALUE, value.get(), 0.0000000001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":"+Double.MAX_VALUE+"}"));
        assertEquals(Double.MAX_VALUE, value.get(), 0.0000000001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":4.2}"));
        assertEquals(4.2, value.get(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":4}"));
        assertEquals(4, value.get(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":0}"));
        assertEquals(0, value.get(), 0.00001);

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":-123.456}"));
        assertEquals(-123.456, value.get(), 0.00001);
    }

    @Test
    public void itSupportsObjectValues() throws PropertyError
    {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<JSONObject> value = new Value<>(new JSONObject().put("key1", "val1").put("key2", "val2"),
            v -> System.out.println("value: " + v));
        thing.addProperty(new Property<>(thing, "p", value, new JSONObject().put("type", "object")));
        
        // when updating property, then
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("key1", "val1");
        expectedMap.put("key2", "val2");
        assertEquals(expectedMap, value.get().toMap());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":{\"key3\":\"val3\"}}"));
        assertEquals(Collections.singletonMap("key3", "val3"), value.get().toMap());
    }

    @Test
    public void itSupportsArrayValues() throws PropertyError
    {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<JSONArray> value = new Value<>(new JSONArray("[1,2,3]"), v -> System.out.println("value: " + v));
        thing.addProperty(new Property<>(thing, "p", value, new JSONObject().put("type", "array")));
        
        // when updating property, then
        assertEquals(Arrays.asList(1,2,3), value.get().toList());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":[]}"));
        assertEquals(Arrays.asList(), value.get().toList());
    }

    @Test
    public void itSupportsStringValues() throws PropertyError
    {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<String> value = new Value<>("the-initial-string", v -> System.out.println("value: " + v));
        thing.addProperty(new Property<>(thing, "p", value, new JSONObject().put("type", "string")));
        
        // when updating property, then
        assertEquals("the-initial-string", value.get());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":\"the-updated-string\"}"));
        assertEquals("the-updated-string", value.get());
    }

    @Test
    public void itSupportsBooleanValues() throws PropertyError
    {
        // given
        Thing thing = new Thing("urn:dev:test-123", "My TestThing");

        Value<Boolean> value = new Value<>(false, v -> System.out.println("value: " + v));
        thing.addProperty(new Property<>(thing, "p", value, new JSONObject().put("type", "boolean")));
        
        // when updating property, then
        assertFalse(value.get());

        thing.setProperty("p", simulateHttpPutProperty("p", "{\"p\":true}"));
        assertTrue(value.get());
    }
}
