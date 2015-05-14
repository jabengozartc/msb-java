package tcdl.msb;

import static tcdl.msb.support.Utils.ifNull;

import org.apache.commons.lang3.Validate;

import tcdl.msb.config.MsbMessageOptions;
import tcdl.msb.events.Event;
import tcdl.msb.events.EventEmitter;
import tcdl.msb.events.EventHandler;
import tcdl.msb.events.TwoArgumentsAdapter;
import tcdl.msb.messages.Acknowledge;
import tcdl.msb.messages.Message;
import tcdl.msb.messages.MessageFactory;
import tcdl.msb.messages.MetaMessage;
import tcdl.msb.messages.payload.ResponsePayload;

/**
 * Created by rdro on 4/29/2015.
 */
public class Responder {

    public static Event RESPONDER_EVENT = new Event("responder");

    MsbMessageOptions msgOptions;
    private MetaMessage meta;
    private Acknowledge ack;
    private Message originalMessage;
    private ChannelManager channelManager;
    private MessageFactory messageFactory;
    private Message responseMessage;

    public Responder(MsbMessageOptions msgOptions, Message originalMessage) {
        Validate.notNull(msgOptions);
        Validate.notNull(originalMessage);

        this.msgOptions = msgOptions;
        this.messageFactory = MessageFactory.getInstance();
        this.meta = this.messageFactory.createMeta(this.msgOptions);
        this.ack = this.messageFactory.createAck(this.msgOptions);
        this.originalMessage = originalMessage;

        channelManager = ChannelManager.getInstance();
    }

    public Message getOriginalMessage() {
        return this.originalMessage;
    }

    public void sendAck(Integer timeoutMs, Integer responsesRemaining, EventHandler callback) { 
        this.ack.withTimeoutMs(timeoutMs != null && timeoutMs > -1 ? timeoutMs : null);
        this.ack.withResponsesRemaining(ifNull(responsesRemaining, 1));

        Message ackMessage = this.messageFactory.createAckMessage(originalMessage, ack);
        sendMessage(ackMessage, callback);
    }

    public void send(ResponsePayload payload, EventHandler callback) {
        this.ack.withResponsesRemaining(-1);
        Message message = this.messageFactory.createResponseMessage(originalMessage, ack, payload);
        sendMessage(message, callback);
    };

    private void sendMessage(Message message, EventHandler callback) {
        this.responseMessage = this.messageFactory.completeMeta(message, meta);

        Producer producer = channelManager.findOrCreateProducer(message.getTopics().getTo());
        producer.publish(message);

        if (callback != null) {
            producer.withMessageHandler(callback);
        }
    }

    public static EventEmitter createEmitter(final MsbMessageOptions msgOptions) {
        String topic = msgOptions.getNamespace();
        ChannelManager channelManager = ChannelManager.getInstance();
        channelManager.findOrCreateConsumer(topic, null)
                .withMessageHandler(new TwoArgumentsAdapter<Message, Exception>() {
                    @Override
                    public void onEvent(Message message, Exception exception) {
                        channelManager.emit(RESPONDER_EVENT, new Responder(msgOptions, message));
                    }
                }).subscribe();

        return channelManager;
    }

    public static ResponderServer createServer(MsbMessageOptions msgOptions) {
        return new ResponderServer(msgOptions);
    }

    Message getResponseMessage() {
        return responseMessage;
    }
}
