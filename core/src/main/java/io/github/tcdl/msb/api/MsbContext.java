package io.github.tcdl.msb.api;


/**
 * Specifies the context of the MSB message processing.
 */
public interface MsbContext {
    
    /**
    *
    * @return object of class  {@link ObjectFactory} which provides access to communication objects
    */
    public ObjectFactory getObjectFactory();

    /**
     * Gracefully shuts down the current context.
     * This methods is not guaranteed to be THREAD-SAFE and is not intended to be executed in parallel from different threads
     */
    public void shutdown();

}
