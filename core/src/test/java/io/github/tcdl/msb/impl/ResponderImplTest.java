package io.github.tcdl.msb.impl;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Clock;

import io.github.tcdl.msb.ChannelManager;
import io.github.tcdl.msb.Producer;
import io.github.tcdl.msb.api.MessageTemplate;
import io.github.tcdl.msb.api.Responder;
import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.api.message.payload.Payload;
import io.github.tcdl.msb.config.MsbConfig;
import io.github.tcdl.msb.message.MessageFactory;
import io.github.tcdl.msb.support.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ResponderImplTest {

    private MessageTemplate config;
    private MsbConfig msbConf;
    private static final String TOPIC = "test:responder";

    private ChannelManager mockChannelManager;
    private Producer mockProducer;
    private Payload payload;
    private Message originalMessage;
    private Responder responder;

    @Before
    public void setUp() {
        config = TestUtils.createSimpleMessageTemplate();

        msbConf = TestUtils.createMsbConfigurations();
        Clock clock = Clock.systemDefaultZone();

        MessageFactory messageFactory = new MessageFactory(msbConf.getServiceDetails(), clock);
        MessageFactory spyMessageFactory = spy(messageFactory);

        MsbContextImpl msbContext = TestUtils.createSimpleMsbContext();
        MsbContextImpl msbContextSpy = spy(msbContext);
        mockChannelManager = mock(ChannelManager.class);
        mockProducer = mock(Producer.class);
        payload = new Payload.Builder().build();
        originalMessage = TestUtils.createMsbRequestMessageWithSimplePayload(TOPIC);

        when(msbContextSpy.getChannelManager()).thenReturn(mockChannelManager);
        when(msbContextSpy.getMessageFactory()).thenReturn(spyMessageFactory);
        when(mockChannelManager.findOrCreateProducer(anyString())).thenReturn(mockProducer);

        responder = new ResponderImpl(config, originalMessage, msbContextSpy);
    }

    @Test
    public void testResponderConstructorOk() {
        MsbContextImpl context = TestUtils.createSimpleMsbContext();
        Message originalMessage = TestUtils.createMsbRequestMessageWithSimplePayload(TOPIC);
        new ResponderImpl(config, originalMessage, context);
    }

    @Test
    public void testProducerWasCreatedForProperTopic() {
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        responder.send(payload);

        verify(mockChannelManager).findOrCreateProducer(argument.capture());

        assertEquals(originalMessage.getTopics().getResponse(), argument.getValue());
    }

    @Test
    public void testProducerPublishMethodInvoked() {
        responder.send(payload);

        verify(mockProducer, times(1)).publish(anyObject());
    }

    @Test
    public void testProducerPublishUseCorrectPayload() {
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        responder.send(payload);

        verify(mockProducer).publish(argument.capture());

        assertEquals(payload, argument.getValue().getPayload());
    }

    @Test
    public void testAckBuilderContainsCorrectProperties() {
        Integer timeout = Integer.valueOf(222);
        Integer responsesRemaining = Integer.valueOf(2);
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        responder.sendAck(timeout, responsesRemaining);

        verify(mockProducer).publish(argument.capture());

        assertEquals(argument.getValue().getAck().getTimeoutMs(), timeout);
        assertEquals(argument.getValue().getAck().getResponsesRemaining(), responsesRemaining);
    }

    @Test
    public void testSendWithSameResponderId() {
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        responder.sendAck(1000, 1);

        verify(mockProducer).publish(messageCaptor.capture());
        String responderIdInAck = messageCaptor.getValue().getAck().getResponderId();

        reset(mockProducer);
        responder.send(TestUtils.createSimpleResponsePayload());
        verify(mockProducer).publish(messageCaptor.capture());

        String responderIdInAckPayload = messageCaptor.getValue().getAck().getResponderId();
        assertEquals(responderIdInAck, responderIdInAckPayload);
    }
}
