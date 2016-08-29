package io.github.tcdl.msb.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import io.github.tcdl.msb.ChannelManager;
import io.github.tcdl.msb.MessageHandler;
import io.github.tcdl.msb.Producer;
import io.github.tcdl.msb.api.AcknowledgementHandler;
import io.github.tcdl.msb.api.MessageTemplate;
import io.github.tcdl.msb.api.RequestOptions;
import io.github.tcdl.msb.api.Responder;
import io.github.tcdl.msb.api.ResponderContext;
import io.github.tcdl.msb.api.ResponderServer;
import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.api.message.payload.RestPayload;
import io.github.tcdl.msb.support.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ResponderServerImplTest {

    private static final String TOPIC = "test:responder-server";

    private RequestOptions requestOptions;
    private MessageTemplate messageTemplate;

    private MsbContextImpl msbContext = TestUtils.createSimpleMsbContext();

    @Before
    public void setUp() {
        requestOptions = TestUtils.createSimpleRequestOptions();
        messageTemplate = TestUtils.createSimpleMessageTemplate();
        msbContext = TestUtils.createSimpleMsbContext();
    }

    @Test
    public void testResponderServerProcessPayloadSuccess() throws Exception {
        Message originalMessage = TestUtils.createSimpleRequestMessage(TOPIC);

        ResponderServer.RequestHandler<RestPayload<Object, Map<String, String>, Object, Map<String, String>>> handler =
                (request, responderContext) -> {
                    assertEquals("MessageContext must contain original message during message handler execution",
                            originalMessage, MsbThreadContext.getMessageContext().getOriginalMessage());
                    assertEquals("MsbThreadContext must contain a Request during message handler execution",
                            request, MsbThreadContext.getRequest());
                };

        ArgumentCaptor<MessageHandler> subscriberCaptor = ArgumentCaptor.forClass(MessageHandler.class);
        ChannelManager spyChannelManager = spy(msbContext.getChannelManager());
        MsbContextImpl spyMsbContext = spy(msbContext);

        when(spyMsbContext.getChannelManager()).thenReturn(spyChannelManager);

        ResponderServerImpl<RestPayload<Object, Map<String, String>, Object, Map<String, String>>> responderServer = ResponderServerImpl
                .create(TOPIC, Collections.emptySet(), requestOptions.getMessageTemplate(), spyMsbContext, handler, null,
                        new TypeReference<RestPayload<Object, Map<String, String>, Object, Map<String, String>>>() {});

        ResponderServerImpl spyResponderServer = (ResponderServerImpl) spy(responderServer).listen();

        verify(spyChannelManager).subscribe(anyString(), subscriberCaptor.capture());

        assertNull("MessageContext must be absent outside message handler execution", MsbThreadContext.getMessageContext());
        assertNull("Request must be absent outside message handler execution", MsbThreadContext.getRequest());
        subscriberCaptor.getValue().handleMessage(originalMessage, null);
        assertNull("MessageContext must be absent outside message handler execution", MsbThreadContext.getMessageContext());
        assertNull("Request must be absent outside message handler execution", MsbThreadContext.getRequest());

        verify(spyResponderServer).onResponder(anyObject());
    }

    @Test(expected = NullPointerException.class)
    public void testResponderServerProcessErrorNoHandler() throws Exception {
        msbContext.getObjectFactory().createResponderServer(TOPIC, messageTemplate, null);
    }

    @Test
    public void testResponderServerProcessUnexpectedPayload() throws Exception {
        ResponderServer.RequestHandler<Integer> handler = (request, responderContext) -> {};

        String bodyText = "some body";
        Message incomingMessage = TestUtils.createMsbRequestMessage(TOPIC, bodyText);

        ResponderServerImpl<Integer> responderServer = ResponderServerImpl
                .create(TOPIC, null, messageTemplate, msbContext, handler, null, new TypeReference<Integer>() {});
        responderServer.listen();

        // simulate incoming request
        ResponderImpl responder = spy(new ResponderImpl(messageTemplate, incomingMessage, msbContext));

        AcknowledgementHandler acknowledgeHandler = mock(AcknowledgementHandler.class);
        ResponderContext responderContext = responderServer.createResponderContext(responder, acknowledgeHandler, incomingMessage);
        
        responderServer.onResponder(responderContext);
        verify(responder).sendAck(0, 0);
        verify(acknowledgeHandler).confirmMessage();
    }

    @Test
    public void testResponderServerProcessHandlerThrowException() throws Exception {
        String exceptionMessage = "Test exception message";
        Exception error = new Exception(exceptionMessage);
        ResponderServer.RequestHandler<String> handler = (request, responderContext) -> { throw error; };

        ResponderServerImpl<String> responderServer = ResponderServerImpl
                .create(TOPIC, null, messageTemplate, msbContext, handler, null, new TypeReference<String>() {});
        responderServer.listen();

        // simulate incoming request
        Message originalMessage = TestUtils.createMsbRequestMessageNoPayload(TOPIC);
        ResponderImpl responder = spy(
                new ResponderImpl(messageTemplate, originalMessage, msbContext));
        AcknowledgementHandler acknowledgeHandler = mock(AcknowledgementHandler.class);
        ResponderContext responderContext = responderServer.createResponderContext(responder, acknowledgeHandler, originalMessage);

        responderServer.onResponder(responderContext);
        verify(responder).sendAck(0, 0);
        verify(acknowledgeHandler).confirmMessage();
    }

    @Test
    public void testResponderServerProcessCustomHandlerThrowException() throws Exception {
        String exceptionMessage = "Test exception message";
        Exception error = new Exception(exceptionMessage);
        ResponderServer.RequestHandler<String> handler = (request, responderContext) -> {
            throw error;
        };

        ResponderServer.ErrorHandler errorHandlerMock = mock(ResponderServer.ErrorHandler.class);
        ResponderServerImpl<String> responderServer = ResponderServerImpl
                .create(TOPIC, null, messageTemplate, msbContext, handler, errorHandlerMock, new TypeReference<String>() {});
        responderServer.listen();

        // simulate incoming request
        Message originalMessage = TestUtils.createMsbRequestMessageNoPayload(TOPIC);
        Responder responder = mock(Responder.class);
        AcknowledgementHandler acknowledgeHandler = mock(AcknowledgementHandler.class);
        ResponderContext responderContext = responderServer.createResponderContext(responder, acknowledgeHandler, originalMessage);

        responderServer.onResponder(responderContext);

        verify(errorHandlerMock).handle(eq(error), eq(originalMessage));
    }

    @Test
    public void testCreateResponderWithResponseTopic() {
        ResponderServer.RequestHandler<String> handler = (request, responderContext) -> {
        };

        ChannelManager mockChannelManager = mock(ChannelManager.class);
        Producer mockProducer = mock(Producer.class);
        when(mockChannelManager.findOrCreateProducer(anyString())).thenReturn(mockProducer);
        MsbContextImpl msbContext1 = new TestUtils.TestMsbContextBuilder()
                .withChannelManager(mockChannelManager)
                .build();

        ResponderServerImpl<String> responderServer = ResponderServerImpl
                .create(TOPIC, null, messageTemplate, msbContext1, handler, null, new TypeReference<String>() {});

        Message incomingMessage = TestUtils.createMsbRequestMessageNoPayload(TOPIC);
        Responder responder = responderServer.createResponder(incomingMessage);
        ResponderContext responderContext = responderServer.createResponderContext(responder, null, incomingMessage);
        assertEquals(incomingMessage, responderContext.getOriginalMessage());

        responder.sendAck(1, 1);
        responder.send("response");

        // Verify that 2 messages were published
        verify(mockProducer, times(2)).publish(any(Message.class));
    }

    @Test
    public void testCreateResponderWithRoutingKeys() throws Exception {

        ChannelManager mockChannelManager = mock(ChannelManager.class);
        MsbContextImpl msbContext = new TestUtils.TestMsbContextBuilder()
                .withChannelManager(mockChannelManager)
                .build();

        Set<String> routingKeys = Sets.newHashSet("routing.key.one", "routing.key.two");

        ResponderServer.RequestHandler<String> requestHandler = (request, responderContext) -> {};
        ResponderServerImpl<String> responderServer = ResponderServerImpl
                .create(TOPIC, routingKeys, messageTemplate, msbContext, requestHandler, null, new TypeReference<String>() {});

        responderServer.listen();
        verify(mockChannelManager).subscribe(eq(TOPIC), eq(routingKeys), any(MessageHandler.class));
    }

    @Test
    public void testCreateResponderWithoutRoutingKeys() throws Exception {

        ChannelManager mockChannelManager = mock(ChannelManager.class);
        MsbContextImpl msbContext = new TestUtils.TestMsbContextBuilder()
                .withChannelManager(mockChannelManager)
                .build();

        ResponderServer.RequestHandler<String> requestHandler = (request, responderContext) -> {};
        ResponderServerImpl<String> responderServer = ResponderServerImpl
                .create(TOPIC, Collections.emptySet(), messageTemplate, msbContext, requestHandler, null, new TypeReference<String>() {});

        responderServer.listen();
        verify(mockChannelManager).subscribe(eq(TOPIC), any(MessageHandler.class));
    }

    @Test
    public void testCreateResponderNoResponseTopic() {
        ResponderServer.RequestHandler<String> handler = (request, responderContext) -> {
        };

        ChannelManager mockChannelManager = mock(ChannelManager.class);
        MsbContextImpl msbContext = new TestUtils.TestMsbContextBuilder()
                .withChannelManager(mockChannelManager)
                .build();

        ResponderServerImpl<String> responderServer = ResponderServerImpl
                .create(TOPIC, null, messageTemplate, msbContext, handler, null, new TypeReference<String>() {});

        Message incomingMessage = TestUtils.createMsbBroadcastMessageNoPayload(TOPIC);
        Responder responder = responderServer.createResponder(incomingMessage);

        responder.sendAck(1, 1);
        responder.send("response");

        // Verify that no messages were published
        verifyZeroInteractions(mockChannelManager);
    }

    @Test
    public void testStop() throws Exception {
        ResponderServer.RequestHandler<String> doNothingHandler = (request, responderContext) -> {};

        ChannelManager mockChannelManager = mock(ChannelManager.class);
        MsbContextImpl msbContext = new TestUtils.TestMsbContextBuilder()
                .withChannelManager(mockChannelManager)
                .build();

        ResponderServerImpl<String> responderServer = ResponderServerImpl.create(
                TOPIC, null, messageTemplate, msbContext, doNothingHandler, null, new TypeReference<String>() {}
        );

        responderServer.stop();
        verify(mockChannelManager).unsubscribe(TOPIC);
    }
}
