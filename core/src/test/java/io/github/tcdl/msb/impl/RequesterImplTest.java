package io.github.tcdl.msb.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.github.tcdl.msb.ChannelManager;
import io.github.tcdl.msb.Consumer;
import io.github.tcdl.msb.Producer;
import io.github.tcdl.msb.api.Callback;
import io.github.tcdl.msb.api.MessageTemplate;
import io.github.tcdl.msb.api.RequestOptions;
import io.github.tcdl.msb.api.Requester;
import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.api.message.payload.RestPayload;
import io.github.tcdl.msb.collector.Collector;
import io.github.tcdl.msb.support.TestUtils;

import java.time.Clock;
import java.util.function.BiConsumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Created by rdro on 4/27/2015.
 */
@RunWith(MockitoJUnitRunner.class)
public class RequesterImplTest {

    private static final String NAMESPACE = "test:requester";

    @Mock
    private ChannelManager channelManagerMock;

    @Mock
    private Producer producerMock;

    @Mock
    private Consumer consumerMock;

    @Mock
    private Collector collectorMock;

    @Test
    public void testPublishNoWaitForResponses() throws Exception {
        RequesterImpl<RestPayload> requester = initRequesterForResponsesWith(0, 0, 0, null);

        requester.publish(TestUtils.createSimpleRequestPayload());

        verify(collectorMock, never()).listenForResponses();
        verify(collectorMock, never()).waitForResponses();
    }

    @Test
    public void testPublishWaitForResponses() throws Exception {
        RequesterImpl<RestPayload> requester = initRequesterForResponsesWith(1, 0, 0, null);

        requester.publish(TestUtils.createSimpleRequestPayload());

        verify(collectorMock).listenForResponses();
        verify(collectorMock).waitForResponses();
    }

    @Test
    public void testPublishWaitForResponsesAck() throws Exception {
        RequesterImpl<RestPayload> requester = initRequesterForResponsesWith(1, 1000, 800, arg ->  fail());

        requester.publish(TestUtils.createSimpleRequestPayload());

        Message responseMessage = TestUtils.createMsbRequestMessage("some:topic", "body text");
        collectorMock.handleMessage(responseMessage, null);
    }

    @Test
    public void testProducerPublishWithPayload() throws Exception {
        String bodyText = "Body text";
        RequesterImpl<RestPayload> requester = initRequesterForResponsesWith(0, 0, 0, null);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        RestPayload payload = TestUtils.createPayloadWithTextBody(bodyText);

        requester.publish(payload);

        verify(producerMock).publish(messageCaptor.capture());
        TestUtils.assertRawPayloadContainsBodyText(bodyText, messageCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAcknowledgeEventHandlerIsAdded() throws Exception {
        BiConsumer onAckMock = mock(BiConsumer.class);
        RequesterImpl requester = initRequesterForResponsesWith(1, 0, 0, null);

        requester.onAcknowledge(onAckMock);

        assertThat(requester.eventHandlers.onAcknowledge(), is(onAckMock));
        assertThat(requester.eventHandlers.onResponse(), not(onAckMock));
        assertThat(requester.eventHandlers.onRawResponse(), not(onAckMock));
        assertThat(requester.eventHandlers.onEnd(), not(onAckMock));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResponseEventHandlerIsAdded() throws Exception {
        BiConsumer onResponseMock = mock(BiConsumer.class);
        RequesterImpl requester = initRequesterForResponsesWith(1, 0, 0, null);

        requester.onResponse(onResponseMock);

        assertThat(requester.eventHandlers.onAcknowledge(), not(onResponseMock));
        assertThat(requester.eventHandlers.onResponse(), is(onResponseMock));
        assertThat(requester.eventHandlers.onRawResponse(), not(onResponseMock));
        assertThat(requester.eventHandlers.onEnd(), not(onResponseMock));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testRawResponseEventHandlerIsAdded() throws Exception {
        BiConsumer onRawResponseMock = mock(BiConsumer.class);
        RequesterImpl requester = initRequesterForResponsesWith(1, 0, 0, null);

        requester.onRawResponse(onRawResponseMock);

        assertThat(requester.eventHandlers.onAcknowledge(), not(onRawResponseMock));
        assertThat(requester.eventHandlers.onRawResponse(), is(onRawResponseMock));
        assertThat(requester.eventHandlers.onResponse(), not(onRawResponseMock));
        assertThat(requester.eventHandlers.onEnd(), not(onRawResponseMock));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testEndEventHandlerIsAdded() throws Exception {
        Callback onEndMock = mock(Callback.class);
        RequesterImpl requester = initRequesterForResponsesWith(1, 0, 0, null);

        requester.onEnd(onEndMock);

        assertThat(requester.eventHandlers.onAcknowledge(), not(onEndMock));
        assertThat(requester.eventHandlers.onResponse(), not(onEndMock));
        assertThat(requester.eventHandlers.onRawResponse(), not(onEndMock));
        assertThat(requester.eventHandlers.onEnd(), is(onEndMock));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoEventHandlerAdded() throws Exception {
        Callback onEndMock = mock(Callback.class);
        RequesterImpl requester = initRequesterForResponsesWith(1, 0, 0, null);

        assertThat(requester.eventHandlers.onAcknowledge(), not(onEndMock));
        assertThat(requester.eventHandlers.onResponse(), not(onEndMock));
        assertThat(requester.eventHandlers.onRawResponse(), not(onEndMock));
        assertThat(requester.eventHandlers.onEnd(), not(onEndMock));
    }

    @Test
    public void testRequestMessage() throws Exception {
        ChannelManager channelManagerMock = mock(ChannelManager.class);
        Producer producerMock = mock(Producer.class);
        when(channelManagerMock.findOrCreateProducer(NAMESPACE)).thenReturn(producerMock);
        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

        MsbContextImpl msbContext = TestUtils.createMsbContextBuilder()
                .withChannelManager(channelManagerMock)
                .withClock(Clock.systemDefaultZone())
                .build();

        RestPayload requestPayload = TestUtils.createSimpleRequestPayload();
        Requester<RestPayload> requester = RequesterImpl.create(NAMESPACE, TestUtils.createSimpleRequestOptions(), msbContext, new TypeReference<RestPayload>() {});
        requester.publish(requestPayload);
        verify(producerMock).publish(messageArgumentCaptor.capture());

        Message requestMessage = messageArgumentCaptor.getValue();
        assertNotNull(requestMessage);
        assertNotNull(requestMessage.getMeta());
        assertNotNull(requestMessage.getRawPayload());
    }

    @Test
    public void testRequestMessageWithTags() throws Exception {
        ChannelManager channelManagerMock = mock(ChannelManager.class);
        Producer producerMock = mock(Producer.class);
        when(channelManagerMock.findOrCreateProducer(NAMESPACE)).thenReturn(producerMock);
        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

        MsbContextImpl msbContext = TestUtils.createMsbContextBuilder()
                .withChannelManager(channelManagerMock)
                .withClock(Clock.systemDefaultZone())
                .build();

        String tag = "requester-tag";
        String dynamicTag1 = "dynamic-tag1";
        String dynamicTag2 = "dynamic-tag2";
        String nullTag = null;
        RestPayload requestPayload = TestUtils.createSimpleRequestPayload();
        RequestOptions requestOptions = TestUtils.createSimpleRequestOptionsWithTags(tag);

        Requester<RestPayload> requester = RequesterImpl.create(NAMESPACE, requestOptions, msbContext, new TypeReference<RestPayload>() {});
        requester.publish(requestPayload, dynamicTag1, dynamicTag2, nullTag);
        verify(producerMock).publish(messageArgumentCaptor.capture());

        Message requestMessage = messageArgumentCaptor.getValue();
        assertArrayEquals(new String[]{tag, dynamicTag1, dynamicTag2}, requestMessage.getTags().toArray());
    }

    private RequesterImpl<RestPayload> initRequesterForResponsesWith(Integer numberOfResponses, Integer respTimeout,  Integer ackTimeout , Callback<Void> endHandler) throws Exception {

        MessageTemplate messageTemplateMock = mock(MessageTemplate.class);

        RequestOptions requestOptionsMock = new RequestOptions.Builder()
                .withMessageTemplate(messageTemplateMock)
                .withWaitForResponses(numberOfResponses)
                .withResponseTimeout(respTimeout)
                .withAckTimeout(ackTimeout)
                .build();

        when(channelManagerMock.findOrCreateProducer(anyString())).thenReturn(producerMock);

        MsbContextImpl msbContext = TestUtils.createMsbContextBuilder()
                .withChannelManager(channelManagerMock)
                .build();

        RequesterImpl<RestPayload> requester = spy(RequesterImpl.create(NAMESPACE, requestOptionsMock, msbContext, new TypeReference<RestPayload>() {}));
        requester.onEnd(endHandler);

        collectorMock = spy(new Collector<>(NAMESPACE, TestUtils.createMsbRequestMessageNoPayload(NAMESPACE), requestOptionsMock, msbContext, requester.eventHandlers,
                new TypeReference<RestPayload>() {}));

        doReturn(collectorMock)
                .when(requester)
                .createCollector(anyString(), any(Message.class), any(RequestOptions.class), any(MsbContextImpl.class), any());

        return requester;
    }

}
