# webthing

[![Build Status](https://travis-ci.org/mozilla-iot/webthing-java.svg?branch=master)](https://travis-ci.org/mozilla-iot/webthing-java)
[![Maven](https://img.shields.io/maven-central/v/org.mozilla.iot/webthing.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.mozilla.iot%22%20AND%20a%3A%22webthing%22)
[![license](https://img.shields.io/badge/license-MPL--2.0-blue.svg)](LICENSE)

Implementation of an HTTP [Web Thing](https://iot.mozilla.org/wot/).

# Using

## Maven

Add the following dependency to your project:

```xml
<dependencies>
    <dependency>
        <groupId>org.mozilla.iot</groupId>
        <artifactId>webthing</artifactId>
        <version>LATEST</version>
    </dependency>
</dependencies>
```

## Gradle

Add the following dependency to your project:

```
dependencies {
    runtime(
        [group: 'org.mozilla.iot', name: 'webthing', version: 'LATEST'],
    )
}
```

# Example

In this example we will set up a dimmable light and a humidity sensor (both using fake data, of course). Both working examples can be found in [here](https://github.com/mozilla-iot/webthing-java/tree/master/src/main/java/org/mozilla/iot/webthing/example).

## Dimmable Light

Imagine you have a dimmable light that you want to expose via the web of things API. The light can be turned on/off and the brightness can be set from 0% to 100%. Besides the name, description, and type, a `Light` is required to expose two properties:
* `on`: the state of the light, whether it is turned on or off
    * Setting this property via a `PUT {"on": true/false}` call to the REST API toggles the light.
* `brightness`: the brightness level of the light from 0-100%
    * Setting this property via a PUT call to the REST API sets the brightness level of this light.

First we create a new Thing:

```java
Thing light = new Thing("My Lamp",
                        Arrays.asList("OnOffSwitch", "Light"),
                        "A web connected lamp");
```

Now we can add the required properties.

The **`on`** property reports and sets the on/off state of the light. For this, we need to have a `Value` object which holds the actual state and also a method to turn the light on/off. For our purposes, we just want to log the new state if the light is switched on/off.

```java
Map<String, Object> onDescription = new HashMap<>();
onDescription.put("@type", "OnOffProperty");
onDescription.put("label", "On/Off");
onDescription.put("type", "boolean");
onDescription.put("description", "Whether the lamp is turned on");

Value<Boolean> on = new Value<>(true,
                                // Here, you could send a signal to
                                // the GPIO that switches the lamp
                                // off
                                v -> System.out.printf(
                                        "On-State is now %s\n",
                                        v));

light.addProperty(new Property(light, "on", on, onDescription));
```

The **`brightness`** property reports the brightness level of the light and sets the level. Like before, instead of actually setting the level of a light, we just log the level.

```java
Map<String, Object> brightnessDescription = new HashMap<>();
brightnessDescription.put("@type", "BrightnessProperty");
brightnessDescription.put("label", "Brightness");
brightnessDescription.put("type", "number");
brightnessDescription.put("description",
                          "The level of light from 0-100");
brightnessDescription.put("minimum", 0);
brightnessDescription.put("maximum", 100);
brightnessDescription.put("unit", "percent");

Value<Double> level = new Value<>(0.0,
                                  // Here, you could send a signal
                                  // to the GPIO that controls the
                                  // brightness
                                  l -> System.out.printf(
                                          "Brightness is now %s\n",
                                          l));

light.addProperty(new Property(light, "level", level, brightnessDescription));
```

Now we can add our newly created thing to the server and start it:

```java
try {
    // If adding more than one thing, use MultipleThings() with a name.
    // In the single thing case, the thing's name will be broadcast.
    WebThingServer server = new WebThingServer(new SingleThing(light), 8888);

    Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
            server.stop();
        }
    });

    server.start(false);
} catch (IOException e) {
    System.out.println(e);
    System.exit(1);
}
```

This will start the server, making the light available via the WoT REST API and announcing it as a discoverable resource on your local network via mDNS.

## Sensor

Let's now also connect a humidity sensor to the server we set up for our light.

A `MultiLevelSensor` (a sensor that returns a level instead of just on/off) has one required property (besides the name, type, and optional description): **`level`**. We want to monitor this property and get notified if the value changes.

First we create a new Thing:

```java
Thing sensor = new Thing("My Humidity Sensor",
                         Arrays.asList("MultiLevelSensor"),
                         "A web connected humidity sensor");
```

Then we create and add the appropriate property:
* `level`: tells us what the sensor is actually reading
    * Contrary to the light, the value cannot be set via an API call, as it wouldn't make much sense, to SET what a sensor is reading. Therefore, we are creating a *readOnly* property.

    ```java
    Map<String, Object> levelDescription = new HashMap<>();
    levelDescription.put("@type", "LevelProperty");
    levelDescription.put("label", "Humidity");
    levelDescription.put("type", "number");
    levelDescription.put("description", "The current humidity in %");
    levelDescription.put("minimum", 0);
    levelDescription.put("maximum", 100);
    levelDescription.put("unit", "percent");
    levelDescription.put("readOnly", true);

    this.level = new Value<>(0.0);

    sensor.addProperty(new Property(sensor, "level", level, levelDescription));
    ```

Now we have a sensor that constantly reports 0%. To make it usable, we need a thread or some kind of input when the sensor has a new reading available. For this purpose we start a thread that queries the physical sensor every few seconds. For our purposes, it just calls a fake method.

```java
// Start a thread that polls the sensor reading every 3 seconds
new Thread(()->{
    while(true){
        try {
            Thread.sleep(3000);
            // Spdates the underlying value, which in turn notifies all
            // listeners
            this.level.notifyOfExternalUpdate(readFromGPIO());
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}).start();
```

This will update our `Value` object with the sensor readings via the `this.level.notifyOfExternalUpdate(readFromGPIO());` call. The `Value` object now notifies the property and the thing that the value has changed, which in turn notifies all websocket listeners.
