package org.mozilla.iot.webthing.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.WebThingServer;

public class ExampleServer {
    public static void main(String[] args) {
        //Create a thing that represents a Dimmable Light
        Thing light = new ExampleDimmableLight().getThing();
        //Create a thing that represents a Sensor
        Thing sensor = new FakeGpioHumiditySensor().getThing();

        try {
            List<Thing> things = new ArrayList<>();
            things.add(light);
            things.add(sensor);

            // If adding more than one thing here, be sure to set the second
            // parameter to some string, which will be broadcast via mDNS.
            // In the single thing case, the thing's name will be broadcast.
            WebThingServer server = new WebThingServer(things, "LightAndTempDevice", 8888);

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
    }


}
