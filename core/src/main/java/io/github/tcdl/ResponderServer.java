package io.github.tcdl;

import io.github.tcdl.config.MsbMessageOptions;
import io.github.tcdl.events.EventEmitter;
import io.github.tcdl.events.SingleArgEventHandler;
import io.github.tcdl.messages.payload.Payload;
import io.github.tcdl.middleware.Middleware;
import io.github.tcdl.middleware.MiddlewareChain;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by rdro on 4/29/2015.
 */
public class ResponderServer {

    public static final Logger LOG = LoggerFactory.getLogger(ResponderServer.class);

    private MsbMessageOptions messageOptions;
    private EventEmitter eventEmitter;
    private MiddlewareChain middlewareChain = new MiddlewareChain();

    public ResponderServer(MsbMessageOptions messageOptions) {
        this.messageOptions = messageOptions;
    }

    public ResponderServer use(Middleware... middleware) {
        middlewareChain.add(middleware);
        return this;
    }

    public ResponderServer listen() {
        if (eventEmitter != null) {
            throw new IllegalStateException("Already listening");
        }

        eventEmitter = Responder.createEmitter(this.messageOptions, onResponder);       

        return this;
    }

    private SingleArgEventHandler<Responder> onResponder = (Responder responder) -> {
            Payload request = responder.getOriginalMessage().getPayload();      
            Response response = new Response(responder);
            CompletableFuture.supplyAsync(() ->
                    middlewareChain
                            .withErrorHandler((req, resp, error) -> {
                                    if (error == null)
                                        return;
                                    errorHandler(request, response, error);
                            })
                            .invoke(request, response));
    };

    private void errorHandler(Payload request, Response response, Exception err) {
        LOG.error("Error processing request {}", request);
        Payload responsePayload = new Payload.PayloadBuilder()
                .setStatusCode(500)
                .setStatusMessage(err.getMessage()).build();
        response.getResponder().send(responsePayload, null);
    }
}
