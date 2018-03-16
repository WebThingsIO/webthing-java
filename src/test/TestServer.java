package test;

import org.json.JSONObject;
import org.mozilla.iot.webthing.Action;
import org.mozilla.iot.webthing.Event;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.WebThingServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestServer {
    public static class RebootEvent extends Event {
        public RebootEvent(Thing thing) {
            super(thing, "reboot", "Going down for reboot");
        }
    }

    public static class RebootAction extends Action {
        public RebootAction(Thing thing, JSONObject params) {
            super(UUID.randomUUID().toString(), thing, "reboot", params);
        }

        @Override
        public void performAction() {
            this.thing.addEvent(new RebootEvent(this.thing));
        }
    }

    public static void main(String[] args) {
        Thing thing =
                new Thing("WoT Pi", "thing", "A WoT-connected Raspberry Pi");

        Map<String, Object> temperatureDescription = new HashMap<>();
        temperatureDescription.put("type", "number");
        temperatureDescription.put("unit", "celsius");
        temperatureDescription.put("description",
                                   "An ambient temperature sensor");
        thing.addProperty(new Property(thing,
                                       "temperature",
                                       temperatureDescription));

        Map<String, Object> humidityDescription = new HashMap<>();
        humidityDescription.put("type", "number");
        humidityDescription.put("unit", "percent");
        thing.addProperty(new Property(thing, "humidity", humidityDescription));

        Map<String, Object> ledDescription = new HashMap<>();
        ledDescription.put("type", "boolean");
        ledDescription.put("description", "A red LED");
        thing.addProperty(new Property(thing, "led", ledDescription));

        thing.addActionDescription("reboot", "Reboot the device", RebootAction.class);
        thing.addEventDescription("reboot", "Going down for reboot");

        WebThingServer server = new WebThingServer(thing, 8888);

        try {
            server.start();
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                server.stop();
                System.exit(0);
            }
        }
    }
}
