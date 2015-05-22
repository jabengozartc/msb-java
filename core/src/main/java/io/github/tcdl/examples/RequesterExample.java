package io.github.tcdl.examples;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.tcdl.ChannelManager;
import io.github.tcdl.Requester;
import io.github.tcdl.config.MsbConfigurations;
import io.github.tcdl.config.MsbMessageOptions;
import io.github.tcdl.events.Event;
import io.github.tcdl.messages.Acknowledge;
import io.github.tcdl.messages.MessageFactory;
import io.github.tcdl.messages.payload.Payload;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rdro on 5/18/2015.
 */
public class RequesterExample {

    public static void main(String... args) {
        Config config = ConfigFactory.load();
        MsbConfigurations msbConfig = new MsbConfigurations(config);
        ChannelManager channelManager = new ChannelManager(config);
        MessageFactory messageFactory = new MessageFactory(msbConfig.getServiceDetails());

        MsbMessageOptions options = new MsbMessageOptions();
        options.setNamespace("test:simple-requester");
        options.setWaitForResponses(1);
        options.setAckTimeout(100);
        options.setResponseTimeout(2000);

        Map<String, String> headers = new HashMap<>();
        headers.put("From", "user@example.com");
        Payload requestPayload = new Payload.PayloadBuilder().setHeaders(headers).build();

        Requester requester = new Requester(options, null, messageFactory, channelManager, msbConfig);

        requester
                .on(Event.ACKNOWLEDGE_EVENT, (Acknowledge acknowledge) -> {
                    System.out.println(">>> ACK timeout: " + acknowledge.getTimeoutMs());
                })
                .on(Event.RESPONSE_EVENT, (Payload payload) -> {
                    System.out.println(">>> RESPONSE body: " + payload.getBody());
                });

        requester.publish(requestPayload);
    }
}
