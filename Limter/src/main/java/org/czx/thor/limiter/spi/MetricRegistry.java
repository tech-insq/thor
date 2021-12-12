package org.czx.thor.limiter.spi;

import java.util.function.Supplier;

/**
 * Simple abstraction for tracking metrics in the limiters.
 *
 */
public interface MetricRegistry {
    /**
     * Listener to receive samples for a distribution
     */
    interface SampleListener {
        void addSample(Number value);
    }
    interface Counter {
        void increment();
    }

    /**
     * Register a sample distribution.  Samples are added to the distribution via the returned
     * {@link SampleListener}.  Will reuse an existing {@link SampleListener} if the distribution already
     * exists.
     *
     * @param id
     * @param tagNameValuePairs Pairs of tag name and tag value.  Number of parameters must be a multiple of 2.
     * @return SampleListener for the caller to add samples
     */
    SampleListener distribution(String id, String... tagNameValuePairs);
    /**
     * Register a gauge using the provided supplier.  The supplier will be polled whenever the guage
     * value is flushed by the registry.
     *
     * @param id
     * @param tagNameValuePairs Pairs of tag name and tag value.  Number of parameters must be a multiple of 2.
     * @param supplier
     */
    void gauge(String id, Supplier<Number> supplier, String... tagNameValuePairs);
    /**
     * Create a counter that will be increment when an event occurs.  Counters normally translate in an action
     * per second metric.
     *
     * @param id
     * @param tagNameValuePairs
     */
    default Counter counter(String id, String... tagNameValuePairs) {
        return () -> {};
    }
}
