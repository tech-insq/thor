package org.czx.thor.limiter;

import org.czx.thor.limiter.bbr.VAIMDLimit;
import org.czx.thor.limiter.netflix.internal.EmptyMetricRegistry;
import org.czx.thor.limiter.netflix.limit.*;
import org.czx.thor.limiter.netflix.limiter.BlockingLimiter;
import org.czx.thor.limiter.netflix.limiter.SimpleLimiter;
import org.czx.thor.limiter.spi.Limit;
import org.czx.thor.limiter.spi.Limiter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class LimiterFactory {
    private static final Map<String, Function<Void, Limit>> mapInstance = getMap();
    private static final boolean isWindowWrap = true;
    public static <Context> Limiter<Context> createLimiter(String name){
        Function<Void, Limit> func = mapInstance.get(name);
        if(func != null) {
            SimpleLimiter<Context> limiter = SimpleLimiter.newBuilder().metricRegistry(EmptyMetricRegistry.INSTANCE).limit(func.apply(null)).build();
            return limiter;
        }else{
            throw new UnsupportedOperationException(String.format("Alg %s is not supported", name));
        }
    }

    public static <Context> Limiter<Context> createBlockLimiter(String name){
       return BlockingLimiter.wrap(createLimiter(name), Duration.ofMillis(5));
    }

    private static Map<String, Function<Void, Limit>> getMap(){
        Map<String, Function<Void, Limit>> map = new HashMap<>();
        map.put("aimd", LimiterFactory::createAIMDLimit);
        map.put("vegas", LimiterFactory::createVegasLimit);
        map.put("window", LimiterFactory::createWindowedLimit);
        map.put("gradient", LimiterFactory::createGradientLimit);
        map.put("gradient2", LimiterFactory::createGradient2Limit);
        map.put("vaimd", LimiterFactory::createVAMIDLimit);
        map.put("fix", LimiterFactory::createFixLimit);
        return map;
    }

    private static Limit createAIMDLimit(Void av){
        Limit limit = AIMDLimit.newBuilder().backoffRatio(0.85).timeout(30, TimeUnit.MILLISECONDS).maxLimit(5000).build();
        if(isWindowWrap){
            return WindowedLimit.newBuilder().windowSize(20).build(limit);
        }else{
            return limit;
        }
    }

    private static Limit createVAMIDLimit(Void av){
        Limit limit = VAIMDLimit.newBuilder().initialLimit(20).backoffRatio(0.85).build();
        return limit;
    }

    private static Limit createVegasLimit(Void av){
        Limit limit = VegasLimit.newBuilder().alpha(32).beta(64).initialLimit(100).maxConcurrency(5000).build();
        if(isWindowWrap){
            return WindowedLimit.newBuilder().windowSize(20).build(limit);
        }else{
            return limit;
        }
    }

    private static Limit createGradientLimit(Void av){
        Limit limit = GradientLimit.newBuilder().rttTolerance(1.20).maxConcurrency(5000).backoffRatio(0.85).build();
        if(isWindowWrap){
            return WindowedLimit.newBuilder().windowSize(20).build(limit);
        }else{
            return limit;
        }
    }

    private static Limit createGradient2Limit(Void av){
        Limit limit = Gradient2Limit.newBuilder().rttTolerance(1.20).maxConcurrency(5000).build();
        if(isWindowWrap){
            return WindowedLimit.newBuilder().windowSize(20).build(limit);
        }else{
            return limit;
        }
    }

    private static Limit createWindowedLimit(Void av){
        return WindowedLimit.newBuilder().windowSize(30).build(createAIMDLimit(null));
    }

    private static Limit createFixLimit(Void av){
        return FixedLimit.of(150);
    }
}
