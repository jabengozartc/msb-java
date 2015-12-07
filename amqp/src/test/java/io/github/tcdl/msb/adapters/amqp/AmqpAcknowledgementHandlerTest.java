package io.github.tcdl.msb.adapters.amqp;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rabbitmq.client.Channel;

@RunWith(MockitoJUnitRunner.class)
public class AmqpAcknowledgementHandlerTest {

    private AmqpAcknowledgementHandler handler;

    @Mock
    private Channel mockChannel;

    private long deliveryTag = 123123123;

    private boolean isRequeueByDefault = true;

    @Before
    public void setUp() {
        handler = getHandler(isRequeueByDefault);
    }

    @Test
    public void testMessageConfirmed() throws Exception {
        handler.confirmMessage();
        verifyConfirmedOnce();
    }

    @Test
    public void testMessageRejectedWithRequeue() throws Exception {
        handler.retryMessage();
        verifyRejectedOnce(true);
    }

    @Test
    public void testMessageRejectedWithoutRequeue() throws Exception {
        handler = getHandler(false);
        handler.retryMessage();
        verifyRejectedOnce(false);
    }

    @Test
    public void testOnlyFirstRejectInvoked() throws Exception {
        handler.retryMessage();
        verifyRejectedOnce();
        submitMultipleConfirmRejectRequests();
        verifyRejectedOnce();
    }

    @Test
    public void testOnlyFirstConfirmInvoked() throws Exception {
        handler.confirmMessage();
        verifyConfirmedOnce();
        submitMultipleConfirmRejectRequests();
        verifyConfirmedOnce();
    }

    @Test
    public void testAutoConfirmConfirmsMessageOnce() throws Exception {
        handler.autoConfirm();
        verifyConfirmedOnce();
        submitMultipleAutoConfirmAutoRejectRequests();
        verifyConfirmedOnce();
    }

    @Test
    public void testAutoRejectRejectsMessageOnce() throws Exception {
        handler.autoReject();
        verifyRejectedOnce();
        submitMultipleAutoConfirmAutoRejectRequests();
        verifyRejectedOnce();
    }

    @Test
    public void testAutoConfirmIgnoredWhenConfirmedByClient() throws Exception {
        handler.confirmMessage();
        verifyConfirmedOnce();
        handler.autoConfirm();
        verifyConfirmedOnce();
    }

    @Test
    public void testAutoRejectIgnoredWhenConfirmedByClient() throws Exception {
        handler.confirmMessage();
        verifyConfirmedOnce();
        handler.autoReject();
        verifyConfirmedOnce();
    }

    @Test
    public void testAutoConfirmIgnoredWhenRejectedByClient() throws Exception {
        handler.retryMessage();
        verifyRejectedOnce();
        handler.autoConfirm();
        verifyRejectedOnce();
    }

    @Test
    public void testAutoRejectIgnoredWhenRejectedByClient() throws Exception {
        handler.retryMessage();
        verifyRejectedOnce();
        handler.autoReject();
        verifyRejectedOnce();
    }

    private void verifyConfirmedOnce() throws Exception {
        verify(mockChannel, times(1)).basicAck(deliveryTag, false);
        verifyNoMoreInteractions(mockChannel);
    }

    private void verifyRejectedOnce() throws Exception {
        verifyRejectedOnce(isRequeueByDefault);
    }

    private void verifyRejectedOnce(boolean isRequeue) throws Exception {
        verify(mockChannel, times(1)).basicReject(deliveryTag, isRequeue);
        verifyNoMoreInteractions(mockChannel);
    }

    private void submitMultipleConfirmRejectRequests() {
        IntStream.range(0, 5).forEach((i) -> {
                handler.confirmMessage();
                handler.retryMessage();
        });
    }

    private void submitMultipleAutoConfirmAutoRejectRequests() {
        IntStream.range(0, 5).forEach((i) -> {
            handler.autoReject();
            handler.autoConfirm();
        });
    }

    private AmqpAcknowledgementHandler getHandler(boolean isRequeue) {
        return new AmqpAcknowledgementHandler(mockChannel, "any", deliveryTag, isRequeue);
    }

}
