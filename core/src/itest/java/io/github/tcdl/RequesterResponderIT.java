package io.github.tcdl;

import static org.junit.Assert.assertTrue;
import io.github.tcdl.config.MsbConfigurations;
import io.github.tcdl.config.MsbMessageOptions;
import io.github.tcdl.messages.payload.Payload;
import io.github.tcdl.support.TestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequesterResponderIT {

    public static final Logger LOG = LoggerFactory.getLogger(RequesterResponderIT.class);

    private MsbMessageOptions messageOptions;
    private MsbConfigurations msbConf;

    @Before
    public void setUp() throws Exception {
        this.messageOptions = TestUtils.createSimpleConfigSetNamespace("test:requester-responder");
        this.msbConf = MsbConfigurations.msbConfiguration();
    }

    @Test
    public void testResponderServerRecievedMessage() throws Exception {
        CountDownLatch requestRecieved = new CountDownLatch(1);
        
        //Create and send request message
        Requester requester = new Requester(messageOptions, null);
        Payload requestPayload = TestUtils.createSimpleRequestPayload();
        requester.publish(requestPayload);
        
        Responder.createServer(messageOptions)
        .use(((request, response) -> {
            requestRecieved.countDown();
        }))
        .listen();

        assertTrue("Message was not recieved", requestRecieved.await(500, TimeUnit.MILLISECONDS));

    }

}
