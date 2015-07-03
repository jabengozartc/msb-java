package io.github.tcdl.examples;

import io.github.tcdl.MsbContextImpl;
import io.github.tcdl.RequesterImpl;
import io.github.tcdl.api.MsbContext;
import io.github.tcdl.api.RequestOptions;
import io.github.tcdl.api.message.payload.Payload;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by rdrozdov-tc on 6/8/15.
 */
public class MultipleRequester {

    public static void main(String... args) throws Exception {
        MsbContext msbContext = new MsbContextImpl.MsbContextBuilder().
                withShutdownHook(true).
                build();

        int numberOfRequesters = 5;
        CountDownLatch countDownLatch = new CountDownLatch(numberOfRequesters);

        ConcurrentLinkedDeque<String> requests = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < numberOfRequesters; i++) {
            String requestId = "request#" + (i+1);
            requests.add(requestId);
            runRequester("test:aggregator", requestId, "Holidays in Spain", msbContext,  body -> {
                String confirmedRequestId = (String)body.get("requestId");
                System.out.println(">>> RECEIVED response for " + confirmedRequestId);
                requests.remove(requestId);
                countDownLatch.countDown();
            });
        }

        countDownLatch.await(30000, TimeUnit.MILLISECONDS);
        System.out.println(">>> UN-RESPONDED requests: " + requests.size());
    }

    public static void runRequester(String namespace, String requestId, String queryString, MsbContext msbContext, Consumer<Map> callback) {

        RequestOptions options = new RequestOptions.Builder()
            .withWaitForResponses(1)
            .withAckTimeout(10000)
            .withResponseTimeout(10000)
            .build();

        SearchRequest request = new SearchRequest(requestId, queryString);
        Payload requestPayload = new Payload.PayloadBuilder().withBody(request).build();

        CompletableFuture.supplyAsync(() -> {
            msbContext.getObjectFactory().createRequester(namespace, options)
                .onAcknowledge(acknowledge ->
                    System.out.println(">>> ACK timeout: " + acknowledge.getTimeoutMs())
                )
                .onResponse(payload -> {
                    System.out.println(">>> RESPONSE body: " + payload.getBody());
                    callback.accept(payload.getBodyAs(Map.class));
                })
                .publish(requestPayload);
            return null;
        });
    }

    public static class SearchRequest {

        private String requestId;
        private String query;

        public SearchRequest() {
        }

        public SearchRequest(String requestId, String query) {
            this.requestId = requestId;
            this.query = query;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}
