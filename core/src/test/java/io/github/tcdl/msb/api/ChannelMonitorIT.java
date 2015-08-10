package io.github.tcdl.msb.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tcdl.msb.adapters.mock.MockAdapter;
import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.api.message.MetaMessage;
import io.github.tcdl.msb.api.message.Topics;
import io.github.tcdl.msb.api.message.payload.Payload;
import io.github.tcdl.msb.api.monitor.AggregatorStats;
import io.github.tcdl.msb.api.monitor.ChannelMonitorAggregator;
import io.github.tcdl.msb.config.MsbConfig;
import io.github.tcdl.msb.impl.MsbContextImpl;
import io.github.tcdl.msb.monitor.agent.AgentTopicStats;
import io.github.tcdl.msb.monitor.agent.ChannelMonitorAgent;
import io.github.tcdl.msb.monitor.agent.DefaultChannelMonitorAgent;
import io.github.tcdl.msb.support.TestUtils;
import io.github.tcdl.msb.support.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChannelMonitorIT {

    private static final Instant LAST_PRODUCED_TIME = Instant.parse("2007-12-03T15:15:30.00Z");
    private static final Instant LAST_CONSUMED_TIME = Instant.parse("2007-12-03T17:15:30.00Z");

    private static final int HEARTBEAT_TIMEOUT_MS = 2000;

    MsbContextImpl msbContext;
    ChannelMonitorAggregator channelMonitorAggregator;

    @Before
    public void setUp() {
        msbContext = TestUtils.createSimpleMsbContext();
    }

    @After
    public void tearDown() {
        channelMonitorAggregator.stop();
    }

    @Test
    public void testAnnouncement() throws InterruptedException {
        String TOPIC_NAME = "topic1";
        CountDownLatch announcementReceived = monitorPrepareAwaitOnAnnouncement(TOPIC_NAME);

        ChannelMonitorAgent channelMonitorAgent = new DefaultChannelMonitorAgent(msbContext);
        channelMonitorAgent.producerTopicCreated(TOPIC_NAME);

        assertTrue("Announcement was not received", announcementReceived.await(RequesterResponderIT.MESSAGE_TRANSMISSION_TIME, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAnnouncementUnexpectedMessage() throws InterruptedException {
        String TOPIC_NAME = "topic2";
        CountDownLatch announcementReceived = monitorPrepareAwaitOnAnnouncement(TOPIC_NAME);

        //simulate broken announcement in broker
        MockAdapter.pushRequestMessage(Utils.TOPIC_ANNOUNCE,
                Utils.toJson(TestUtils.createMsbRequestMessageWithSimplePayload(Utils.TOPIC_ANNOUNCE), msbContext.getPayloadMapper()));

        assertFalse("Broken announcement was handled", announcementReceived.await(RequesterResponderIT.MESSAGE_TRANSMISSION_TIME / 2, TimeUnit.MILLISECONDS));

        //verify next correct announcement was handled
        ChannelMonitorAgent channelMonitorAgent = new DefaultChannelMonitorAgent(msbContext);
        channelMonitorAgent.producerTopicCreated(TOPIC_NAME);

        assertTrue("Announcement was not received", announcementReceived.await(RequesterResponderIT.MESSAGE_TRANSMISSION_TIME, TimeUnit.MILLISECONDS));
    }

    private CountDownLatch monitorPrepareAwaitOnAnnouncement(String topicName) throws InterruptedException {
        CountDownLatch announcementReceived = new CountDownLatch(1);
        Callback<AggregatorStats> handler = stats -> {
            assertTrue(stats.getTopicInfoMap().containsKey(topicName));
            assertEquals(1, stats.getTopicInfoMap().get(topicName).getProducers().size());
            announcementReceived.countDown();
        };

        channelMonitorAggregator = msbContext.getObjectFactory().createChannelMonitorAggregator(handler);
        channelMonitorAggregator.start(false, ChannelMonitorAggregator.DEFAULT_HEARTBEAT_INTERVAL_MS, HEARTBEAT_TIMEOUT_MS);

        return announcementReceived;
    }

    @Test
    public void testHeartbeatMessage() throws InterruptedException {
        String TOPIC_NAME = "topic3";

        Map<String, AgentTopicStats> topicInfoMap = new HashMap<>();
        topicInfoMap.put(TOPIC_NAME, new AgentTopicStats(true, false, LAST_PRODUCED_TIME, LAST_CONSUMED_TIME));

        Payload payload = new Payload.Builder<Object, Object, Object, Map<String, AgentTopicStats>>()
                .withBody(topicInfoMap)
                .build();

        CountDownLatch heartBeatResponseReceived = new CountDownLatch(1);
        Callback<AggregatorStats> handler = stats -> {
            assertTrue(stats.getTopicInfoMap().containsKey(TOPIC_NAME));
            assertEquals(1, stats.getTopicInfoMap().get(TOPIC_NAME).getProducers().size());
            heartBeatResponseReceived.countDown();
        };

        channelMonitorAggregator = msbContext.getObjectFactory().createChannelMonitorAggregator(handler);
        channelMonitorAggregator.start(true, ChannelMonitorAggregator.DEFAULT_HEARTBEAT_INTERVAL_MS, HEARTBEAT_TIMEOUT_MS);

        //need to await for original request for heartbeat to be send to simulate response with same correlationId
        Message requestMessage = awaitHeartBeatRequestSent();

        Message responseMessage = createMsbRequestMessage(requestMessage.getTopics().getResponse(), requestMessage.getCorrelationId(),
                payload);
        MockAdapter.pushRequestMessage(requestMessage.getTopics().getResponse(), Utils.toJson(responseMessage, msbContext.getPayloadMapper()));

        assertTrue("Heartbeat response was not received",
                heartBeatResponseReceived.await(HEARTBEAT_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testHeartbeatUnexpectedMessage() throws InterruptedException {
        String TOPIC_NAME = "topic4";

        Map<String, AgentTopicStats> topicInfoMap = new HashMap<>();
        topicInfoMap.put(TOPIC_NAME, new AgentTopicStats(true, false, LAST_PRODUCED_TIME, LAST_CONSUMED_TIME));

        Payload payload = new Payload.Builder<Object, Object, Object, Map<String, AgentTopicStats>>()
                .withBody(topicInfoMap)
                .build();

        CountDownLatch heartBeatResponseReceived = new CountDownLatch(1);
        Callback<AggregatorStats> handler = stats -> {
            assertEquals(1, stats.getTopicInfoMap().size());
            heartBeatResponseReceived.countDown();
        };

        channelMonitorAggregator = msbContext.getObjectFactory().createChannelMonitorAggregator(handler);
        channelMonitorAggregator.start(true, ChannelMonitorAggregator.DEFAULT_HEARTBEAT_INTERVAL_MS, HEARTBEAT_TIMEOUT_MS);

        //need to await for original request for heartbeat to be send to simulate response with same correlationId
        Message requestMessage = awaitHeartBeatRequestSent();

        Message brokenResponseMessage = createMsbRequestMessage(requestMessage.getTopics().getResponse(), requestMessage.getCorrelationId(),
                " unexpected statistics format received");
        Message responseMessage = createMsbRequestMessage(requestMessage.getTopics().getResponse(), requestMessage.getCorrelationId(),
                payload);
        //simulate 3 heartbeatResponses: 1 valid and 2 broken
        MockAdapter.pushRequestMessage(requestMessage.getTopics().getResponse(), Utils.toJson(brokenResponseMessage, msbContext.getPayloadMapper()));
        MockAdapter.pushRequestMessage(requestMessage.getTopics().getResponse(), Utils.toJson(responseMessage, msbContext.getPayloadMapper()));
        MockAdapter.pushRequestMessage(requestMessage.getTopics().getResponse(), Utils.toJson(brokenResponseMessage, msbContext.getPayloadMapper()));

        assertTrue("Heartbeat response was not received",
                heartBeatResponseReceived.await(HEARTBEAT_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS));
    }

    private Message awaitHeartBeatRequestSent() throws InterruptedException {
        //need to await for original request for heartbeat to be send to simulate response with same correlationId
        CountDownLatch awaitRequestMessage = new CountDownLatch(1);
        List<Message> outgoingRequestMessages = new LinkedList<>();
        msbContext.getChannelManager().subscribe(Utils.TOPIC_HEARTBEAT, message -> {
            outgoingRequestMessages.add(message);
            awaitRequestMessage.countDown();
        });

        //fail the test if not able to get original heartbeat request
        assertTrue("Heartbeat original request not captured",
                awaitRequestMessage.await(HEARTBEAT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        //unsubscribe or else will consume messages from previous run
        msbContext.getChannelManager().unsubscribe(Utils.TOPIC_HEARTBEAT);
        return outgoingRequestMessages.get(0);
    }

    //TODO: move to TestUtils in scope of refactoring it (too much same looking methods are already there)
    private Message createMsbRequestMessage(String topicTo, String correlationId, String payloadString) {
        try {
            ObjectMapper payloadMapper = msbContext.getPayloadMapper();
            MsbConfig msbConf = msbContext.getMsbConfig();
            Clock clock = Clock.systemDefaultZone();
            JsonNode payload = payloadMapper.readValue(String.format("{\"body\": \"%s\" }", payloadString), JsonNode.class);

            Topics topic = new Topics(topicTo, topicTo + ":response:" + msbConf.getServiceDetails().getInstanceId());
            MetaMessage.Builder metaBuilder = TestUtils.createSimpleMetaBuilder(msbConf, clock);
            return new Message.Builder()
                    .withCorrelationId(correlationId)
                    .withId(Utils.generateId())
                    .withTopics(topic)
                    .withMetaBuilder(metaBuilder)
                    .withPayload(payload)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare request message", e);
        }
    }

    //TODO: move to TestUtils in scope of refactoring it (too much same looking methods are already there)
    private Message createMsbRequestMessage(String topicTo, String correlationId, Payload payload) {

        ObjectMapper payloadMapper = msbContext.getPayloadMapper();
        MsbConfig msbConf = msbContext.getMsbConfig();
        Clock clock = Clock.systemDefaultZone();

        Topics topic = new Topics(topicTo, topicTo + ":response:" + msbConf.getServiceDetails().getInstanceId());
        MetaMessage.Builder metaBuilder = TestUtils.createSimpleMetaBuilder(msbConf, clock);
        return new Message.Builder()
                .withCorrelationId(correlationId)
                .withId(Utils.generateId())
                .withTopics(topic)
                .withMetaBuilder(metaBuilder)
                .withPayload(Utils.convert(payload, JsonNode.class, payloadMapper))
                .build();
    }
}
