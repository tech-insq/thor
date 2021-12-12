/**
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.czx.thor.limiter.netflix.executors;


import org.czx.thor.limiter.spi.Limiter;
import org.czx.thor.limiter.spi.MetricRegistry;
import org.czx.thor.limiter.netflix.internal.EmptyMetricRegistry;
import org.czx.thor.limiter.netflix.limit.AIMDLimit;
import org.czx.thor.limiter.netflix.limiter.BlockingLimiter;
import org.czx.thor.limiter.netflix.limiter.SimpleLimiter;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Executor} which uses a {@link Limiter} to determine the size of the thread pool.
 * Any {@link Runnable} executed once the limit has been reached will block the calling
 * thread until the limit is released.
 * 
 * Operations submitted to this executor should be homogeneous and have similar
 * long term latency characteristics.  RTT samples will only be taken from successful 
 * operations.  The {@link Runnable} should throw a {@link UncheckedTimeoutException} if
 * a request timed out or some external limit was reached.  All other exceptions will be
 * ignored.
 */
public final class BlockingAdaptiveExecutor implements Executor {
    public static class Builder {
        private static AtomicInteger idCounter = new AtomicInteger();

        private MetricRegistry metricRegistry = EmptyMetricRegistry.INSTANCE;
        private Executor executor;
        private Limiter<Void> limiter;
        private String name;

        public Builder metricRegistry(MetricRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder limiter(Limiter<Void> limiter) {
            this.limiter = limiter;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public BlockingAdaptiveExecutor build() {
            if (name == null) {
                name = "unnamed-" + idCounter.incrementAndGet();
            }

            if (executor == null) {
                executor = Executors.newCachedThreadPool(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setDaemon(true);
                        return thread;
                    }
                });
            }

            if (limiter == null) {
                limiter = SimpleLimiter.newBuilder()
                        .metricRegistry(metricRegistry)
                        .limit(AIMDLimit.newBuilder().build())
                        .build();
            }

            return new BlockingAdaptiveExecutor(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private final Limiter<Void> limiter;
    private final Executor executor;

    private BlockingAdaptiveExecutor(Builder builder) {
        this.limiter = builder.limiter;
        this.executor = builder.executor;
    }

    @Deprecated
    public BlockingAdaptiveExecutor(Limiter<Void> limiter) {
        this(limiter, Executors.newCachedThreadPool());
    }

    @Deprecated
    public BlockingAdaptiveExecutor(Limiter<Void> limiter, Executor executor) {
        this.limiter = BlockingLimiter.wrap(limiter);
        this.executor = executor;
    }

    @Override
    public void execute(Runnable command) {
        Limiter.Listener listener = limiter.acquire(null).orElseThrow(() -> new RejectedExecutionException());
        try {
            executor.execute(() -> {
                try {
                    command.run();
                    listener.onSuccess();
                } catch (UncheckedTimeoutException e) {
                    listener.onDropped();
                } catch (RejectedExecutionException e) {
                    // TODO: Remove support for RejectedExecutionException here.
                    listener.onDropped();
                } catch (Exception e) {
                    // We have no idea what caused the exception.  It could be an NPE thrown immediately on the client
                    // or some remote call failure.  The only sane thing to do here is just ignore this request
                    listener.onIgnore();
                }
            });
        } catch (Exception e) {
            listener.onIgnore();
            throw e;
        }
    }
}
