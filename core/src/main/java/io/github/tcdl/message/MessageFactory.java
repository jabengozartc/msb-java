package io.github.tcdl.message;

import io.github.tcdl.api.MessageTemplate;
import io.github.tcdl.api.message.Acknowledge;
import io.github.tcdl.api.message.Message;
import io.github.tcdl.api.message.MetaMessage;
import io.github.tcdl.api.message.MetaMessage.Builder;
import io.github.tcdl.api.message.Topics;
import io.github.tcdl.api.message.payload.Payload;
import io.github.tcdl.config.ServiceDetails;
import io.github.tcdl.support.Utils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.time.Clock;


/**
 * Created by rdro on 4/22/2015.
 */
public class MessageFactory {

    private ServiceDetails serviceDetails;
    private Clock clock;

    public MessageFactory(ServiceDetails serviceDetails, Clock clock) {
        Validate.notNull(serviceDetails, "'serviceDetails' must not be null");
        Validate.notNull(clock, "'clock' must not be null");
        this.serviceDetails = serviceDetails;
        this.clock = clock;
    }

    public Message.Builder createRequestMessageBuilder(String namespace, MessageTemplate messageTemplate, Message originalMessage) {
        Message.Builder messageBuilder = createMessageBuilderWithMeta(messageTemplate, originalMessage);
        Topics topic = new Topics(namespace, namespace + ":response:" +
                this.serviceDetails.getInstanceId());
        return messageBuilder.withTopics(topic);
    }

    public Message.Builder createResponseMessageBuilder(MessageTemplate messageTemplate, Message originalMessage) {
        Message.Builder messageBuilder = createMessageBuilderWithMeta(messageTemplate, originalMessage);
        Topics topic = new Topics(originalMessage.getTopics().getResponse(), null);
        return messageBuilder.withTopics(topic);
    }

    private Message.Builder createMessageBuilderWithMeta(MessageTemplate config, Message originalMessage) {
        Message.Builder messageBuilder = createBaseMessage(originalMessage);
        MetaMessage.Builder metaBuilder = createMetaBuilder(config);
        return messageBuilder.withMetaBuilder(metaBuilder);
    }

    public Message createRequestMessage(Message.Builder messageBuilder, Payload payload) {
        if (payload != null) {
            messageBuilder.withPayload(payload);
        }
        return messageBuilder.build();
    }

    public Message createResponseMessage(Message.Builder messageBuilder, Acknowledge ack, Payload payload) {
        messageBuilder.withPayload(payload);
        messageBuilder.withAck(ack);
        return messageBuilder.build();
    }

    public Acknowledge.Builder createAckBuilder() {
        return new Acknowledge.Builder().withResponderId(Utils.generateId());
    }

    public Builder createMetaBuilder(MessageTemplate config) {
        Integer ttl = config == null ? null : config.getTtl();
        return new MetaMessage.Builder(ttl, clock.instant(), this.serviceDetails, clock);
    }

    public Message.Builder createBroadcastMessageBuilder(MessageTemplate config, String topicTo, Payload payload) {
        Message.Builder messageBuilder = createBaseMessage(null);
        Builder metaBuilder = createMetaBuilder(config);
        messageBuilder.withMetaBuilder(metaBuilder);

        Topics topics = new Topics(topicTo, null);
        messageBuilder.withTopics(topics);
        messageBuilder.withPayload(payload);

        return messageBuilder;
    }

    private Message.Builder createBaseMessage(@Nullable Message originalMessage) {
        Message.Builder baseMessage = new Message.Builder()
                .setId(Utils.generateId())
                .withCorrelationId(
                        originalMessage != null && originalMessage.getCorrelationId() != null ? originalMessage
                                .getCorrelationId() : Utils.generateId());

        return baseMessage;
    }
}
