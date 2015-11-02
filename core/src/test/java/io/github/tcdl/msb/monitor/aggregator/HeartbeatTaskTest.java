package io.github.tcdl.msb.monitor.aggregator;

import io.github.tcdl.msb.api.Callback;
import io.github.tcdl.msb.api.ObjectFactory;
import io.github.tcdl.msb.api.RequestOptions;
import io.github.tcdl.msb.api.Requester;
import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.api.message.payload.RestPayload;
import io.github.tcdl.msb.api.monitor.ChannelMonitorAggregator;
import io.github.tcdl.msb.support.TestUtils;
import io.github.tcdl.msb.support.Utils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeartbeatTaskTest {

    private ObjectFactory mockObjectFactory = mock(ObjectFactory.class);
    @SuppressWarnings("unchecked")
    private Requester<RestPayload> mockRequester = mock(Requester.class);
    @SuppressWarnings("unchecked")
    private Callback<List<Message>> mockMessageHandler = mock(Callback.class);
    private HeartbeatTask heartbeatTask = new HeartbeatTask(ChannelMonitorAggregator.DEFAULT_HEARTBEAT_TIMEOUT_MS, mockObjectFactory, mockMessageHandler);

    @Before
    public void setUp() {
        when(mockObjectFactory.createRequester(anyString(), any(RequestOptions.class))).thenReturn(mockRequester);
        @SuppressWarnings("unchecked")
        Callback<Message> responseHandler = any(Callback.class);
        when(mockRequester.onRawResponse(responseHandler)).thenReturn(mockRequester);
        @SuppressWarnings("unchecked")
        Callback<Void> endHandler = any(Callback.class);
        when(mockRequester.onEnd(endHandler)).thenReturn(mockRequester);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRun() {
        heartbeatTask.run();

        ArgumentCaptor<Callback> onResponseCaptor = ArgumentCaptor.forClass(Callback.class);
        ArgumentCaptor<Callback> onEndCaptor = ArgumentCaptor.forClass(Callback.class);
        ArgumentCaptor<List> messageCaptor = ArgumentCaptor.forClass(List.class);

        verify(mockObjectFactory).createRequester(eq(Utils.TOPIC_HEARTBEAT), any(RequestOptions.class));
        verify(mockRequester).onRawResponse(onResponseCaptor.capture());
        verify(mockRequester).onEnd(onEndCaptor.capture());
        verify(mockRequester).publish(any(RestPayload.class));

        // simulate incoming messages
        Message msg1 = TestUtils.createSimpleRequestMessage("from:responder");
        Message msg2 = TestUtils.createSimpleRequestMessage("from:responder");
        onResponseCaptor.getValue().call(msg1);
        onResponseCaptor.getValue().call(msg2);
        onEndCaptor.getValue().call(null);

        verify(mockMessageHandler).call(messageCaptor.capture());

        Assert.assertArrayEquals(new Message[] {msg1, msg2}, messageCaptor.getValue().toArray());
    }

    @Test
    public void testRunWithException() {
        try {
            when(mockObjectFactory.createRequester(anyString(), any(RequestOptions.class))).thenThrow(new RuntimeException());
            heartbeatTask.run();
        } catch (Exception e) {
            Assert.fail("Exception should not be thrown");
        }
    }

}