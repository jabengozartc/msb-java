package io.github.tcdl.msb.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.tcdl.msb.api.*;
import io.github.tcdl.msb.api.monitor.AggregatorStats;
import io.github.tcdl.msb.api.monitor.ChannelMonitorAggregator;
import io.github.tcdl.msb.config.MsbConfig;
import io.github.tcdl.msb.monitor.aggregator.DefaultChannelMonitorAggregator;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Provides methods for creation {@link Requester} and {@link ResponderServer}.
 */
public class ObjectFactoryImpl implements ObjectFactory {
    private static Logger LOG = LoggerFactory.getLogger(ObjectFactoryImpl.class);

    private MsbContextImpl msbContext;
    private PayloadConverter payloadConverter;
    private ChannelMonitorAggregator channelMonitorAggregator;

    public ObjectFactoryImpl(MsbContextImpl msbContext) {
        super();
        this.msbContext = msbContext;
        payloadConverter = new PayloadConverterImpl(msbContext.getPayloadMapper());
    }

    @Override
    public <T> Requester<T> createRequester(String namespace, RequestOptions requestOptions, TypeReference<T> payloadTypeReference) {
        return RequesterImpl.create(namespace, requestOptions, msbContext, payloadTypeReference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Requester<T> createRequesterForSingleResponse(String namespace, Class<T> payloadClass) {
        MsbConfig msbConfig = msbContext.getMsbConfig();

        RequestOptions requestOptions = new RequestOptions.Builder()
                .withMessageTemplate(new MessageTemplate())
                .withResponseTimeout(msbConfig.getDefaultResponseTimeout())
                .build();

        return createRequesterForSingleResponse(namespace, payloadClass, requestOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Requester<T> createRequesterForSingleResponse(String namespace, Class<T> payloadClass, RequestOptions baseRequestOptions) {

        RequestOptions requestOptions = new RequestOptions.Builder().from(baseRequestOptions)
                .withWaitForResponses(1)
                .withAckTimeout(0)
                .build();
        return RequesterImpl.create(namespace, requestOptions, msbContext, toTypeReference(payloadClass));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ResponderServer createResponderServer(String namespace, Set<String> routingKeys, MessageTemplate messageTemplate,
                                                     ResponderServer.RequestHandler<T> requestHandler, ResponderServer.ErrorHandler errorHandler, TypeReference<T> payloadTypeReference) {
        return ResponderServerImpl.create(namespace, routingKeys, messageTemplate, msbContext, requestHandler, errorHandler, payloadTypeReference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Requester<T> createRequesterForFireAndForget(String namespace) {
        return createRequesterForFireAndForget(namespace, null);
    }

    @Override
    public <T> Requester<T> createRequesterForFireAndForget(String namespace, MessageTemplate messageTemplate){
        RequestOptions.Builder optionsBuilder = new RequestOptions.Builder()
                .withMessageTemplate(messageTemplate)
                .withWaitForResponses(0);

        return RequesterImpl.create(namespace, optionsBuilder.build(), msbContext, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Requester<T> createRequesterForFireAndForget(MessageDestination destination, MessageTemplate messageTemplate) {
        RequestOptions.Builder optionsBuilder = new RequestOptions.Builder()
                .withMessageTemplate(messageTemplate)
                .withRoutingKey(destination.getRoutingKey())
                .withWaitForResponses(0);

        return RequesterImpl.create(destination.getTopic(), optionsBuilder.build(), msbContext, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Requester<T> createRequesterForFireAndForget(String namespace, MessageDestination forwardTo, MessageTemplate messageTemplate) {
        RequestOptions.Builder optionsBuilder = new RequestOptions.Builder()
                .withMessageTemplate(messageTemplate)
                .withForwardNamespace(forwardTo.getTopic())
                .withRoutingKey(forwardTo.getRoutingKey())
                .withWaitForResponses(0);

        return RequesterImpl.create(namespace, optionsBuilder.build(), msbContext, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ResponderServer createResponderServer(String namespace, MessageTemplate messageTemplate,
            ResponderServer.RequestHandler<T> requestHandler, TypeReference<T> payloadTypeReference) {
        return createResponderServer(namespace, messageTemplate, requestHandler, null, payloadTypeReference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ResponderServer createResponderServer(String namespace, MessageTemplate messageTemplate,
            ResponderServer.RequestHandler<T> requestHandler, ResponderServer.ErrorHandler errorHandler, TypeReference<T> payloadTypeReference) {
        return ResponderServerImpl.create(namespace, Collections.emptySet(), messageTemplate, msbContext, requestHandler, errorHandler, payloadTypeReference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PayloadConverter getPayloadConverter() {
        return payloadConverter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ChannelMonitorAggregator createChannelMonitorAggregator(Callback<AggregatorStats> aggregatorStatsHandler) {
        ThreadFactory threadFactory = new BasicThreadFactory.Builder()
                .namingPattern("monitor-aggregator-heartbeat-thread-%d")
                .daemon(true)
                .build();
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, threadFactory);

        channelMonitorAggregator = createDefaultChannelMonitorAggregator(aggregatorStatsHandler, scheduledExecutorService);
        return channelMonitorAggregator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void shutdown() {
        LOG.info("Shutting down...");
        if (channelMonitorAggregator != null) {
            channelMonitorAggregator.stop();
        }
        LOG.info("Shutdown complete");
    }

    DefaultChannelMonitorAggregator createDefaultChannelMonitorAggregator(Callback<AggregatorStats> aggregatorStatsHandler, ScheduledExecutorService scheduledExecutorService) {
        return new DefaultChannelMonitorAggregator(msbContext, scheduledExecutorService, aggregatorStatsHandler);
    }

    private static <U> TypeReference<U> toTypeReference(Class<U> clazz) {
        return new TypeReference<U>() {
            @Override
            public Type getType() {
                return clazz;
            }
        };
    }
}
