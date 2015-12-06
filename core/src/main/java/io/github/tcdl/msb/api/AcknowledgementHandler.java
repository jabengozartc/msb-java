package io.github.tcdl.msb.api;

/**
 * Callback interface for explicit message acknowledgement
 *
 */
public interface AcknowledgementHandler {
    /**
     * Inform server that a message was confirmed by consumer. 
     * Server should consider messages acknowledged once delivered
     */
    void confirmMessage();
    
    /**
     * Inform server that a message was reject by consumer. 
     * AMQP Server may requeue message or delete it from queue depending on the
     * requeueRejectedMessages configuration option   
     */
    void rejectMessage();
    
}
