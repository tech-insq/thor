package org.czx.thor.limiter.spi;

/**
 * Common metric ids
 */
public final class MetricIds {
    public static final String LIMIT_NAME = "limit";
    public static final String CALL_NAME = "call";
    public static final String INFLIGHT_NAME = "inflight";
    public static final String PARTITION_LIMIT_NAME = "limit.partition";
    public static final String MIN_RTT_NAME = "min_rtt";
    public static final String WINDOW_MIN_RTT_NAME = "min_window_rtt";
    public static final String WINDOW_QUEUE_SIZE_NAME = "queue_size";
    private MetricIds() {}
}
