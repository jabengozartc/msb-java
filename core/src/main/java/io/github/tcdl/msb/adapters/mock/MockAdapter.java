package io.github.tcdl.msb.adapters.mock;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.tcdl.msb.adapters.ConsumerAdapter;
import io.github.tcdl.msb.adapters.ProducerAdapter;
import io.github.tcdl.msb.api.exception.JsonConversionException;
import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.support.Utils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MockAdapter class represents implementation of {@link ProducerAdapter} and {@link ConsumerAdapter}
 * for test purposes.
 */
public class MockAdapter implements ProducerAdapter, ConsumerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MockAdapter.class);
    private static final int CONSUMING_INTERVAL = 20;

    static Map<String, Queue<String>> messageMap = new ConcurrentHashMap<>();

    private String topic;
    private ExecutorService executorService;
    private Queue<ExecutorService> activeConsumerExecutors;

    public MockAdapter(String topic) {
        LOG.debug("Created Mock Adapter for publishing to topic: " + topic);
        this.topic = topic;
    }

    public MockAdapter(String topic, Queue<ExecutorService> activeConsumerExecutors) {
        LOG.debug("Created Mock Adapter for consuming form topic: " + topic);
        this.topic = topic;
        this.activeConsumerExecutors = activeConsumerExecutors;
        this.executorService = activateConsumerThreadPool(topic);
    }

    @Override
    /**
     * @throws ChannelException if an error is encountered during publishing to broker
     */
    public void publish(String jsonMessage) {
        LOG.debug("Received request {}", jsonMessage);
        try {
            Message incomingMessage = Utils.fromJson(jsonMessage, Message.class);
            pushRequestMessage(incomingMessage);
        } catch (JsonConversionException e) {
            LOG.error("Received message can not be parsed");
        }
    }

    @Override
    public void subscribe(RawMessageHandler messageHandler) {
        if (executorService == null) {
            LOG.warn("Mock Adapter not initialized for consuming");
        } else {
            executorService.execute(() -> {
                {
                    String jsonMessage = null;
                    while (!executorService.isShutdown()) {
                        jsonMessage = pollJsonMessageForTopic(topic);

                        if (messageHandler != null && jsonMessage != null) {
                            LOG.debug("Process message for topic {} [{}]", topic, jsonMessage);
                            messageHandler.onMessage(jsonMessage);
                        } else {
                            try {
                                Thread.sleep(CONSUMING_INTERVAL);
                            } catch (Exception e) {
                                LOG.debug("Finish listen for subscribed topic");
                            }
                        }
                    }
                }

            });
        }
    }

    @Override
    public void unsubscribe() {
        LOG.debug("Unsubscribe");
        if (executorService == null) {
            LOG.warn("Mock Adapter not initialized for consuming");
        }
        activeConsumerExecutors.remove(executorService);
        executorService.shutdown();
    }

    public static String pollJsonMessageForTopic(String topic) {
        String jsonMessage = null;
        if (messageMap.get(topic) != null) {
            jsonMessage = messageMap.get(topic).poll();
        }

        if (jsonMessage == null) {
            LOG.debug("No message found for topic {}", topic);
        }
        return jsonMessage;
    }

    public static void pushRequestMessage(Message message) {
        String topicTo = message.getTopics().getTo();
        Queue<String> messagesQueue = messageMap.get(topicTo);
        if (messagesQueue == null) {
            messagesQueue = new ConcurrentLinkedQueue<>();
            Queue<String> curQ = messageMap.putIfAbsent(topicTo, messagesQueue);
            if (curQ != null) {
                messagesQueue = curQ;
            }
        }
        try {
            String jsonMessage = Utils.toJson(message);
            messagesQueue.add(jsonMessage);
            LOG.debug("Message for topic {} published: [{}]", topicTo, jsonMessage);
        } catch (JsonConversionException e) {
            LOG.error("Pushed message can not be parsed");
        }
    }

    private ExecutorService activateConsumerThreadPool(String topic) {
        BasicThreadFactory threadFactory = new BasicThreadFactory.Builder()
                .namingPattern("mock-consumer-" + topic + "-thread-%d")
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(1, threadFactory);
        activeConsumerExecutors.add(executorService);
        return executorService;
    }

}
