package io.github.tcdl.msb.acceptance.bdd.steps;

import io.github.tcdl.msb.api.Requester;
import io.github.tcdl.msb.api.message.payload.RestPayload;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Steps to send multiple requests
 */
public class AsyncRequesterSteps extends MsbSteps {

    private CountDownLatch await;

    @Given("$numberOfRequesters requesters send a request to namespace $namespace with query '$query'")
    public void sendRequests(int numberOfRequesters, String namespace, String query) throws Exception {
        await = new CountDownLatch(numberOfRequesters);

        for (int i = 0; i < numberOfRequesters; i++) {
            CompletableFuture.supplyAsync(() -> {
                Requester<RestPayload> requester = helper.createRequester(namespace, 1, RestPayload.class);
                try {
                    RestPayload<?, ?, ?, ?> requestPayload = helper.createFacetParserPayload(query, null);
                    helper.sendRequest(requester, requestPayload, true, 1, null, this::onResponse);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
                return null;
             });
        }
    }

    @Then("wait responses in $timeout ms")
    public void waitForResponse(long timeout) throws Exception {
        await.await(timeout, TimeUnit.MILLISECONDS);
        assertEquals("Some requests were not responded", 0, await.getCount());
    }

    private void onResponse(RestPayload<?, ?, ?, ?> payload) {
        await.countDown();
    }
}
