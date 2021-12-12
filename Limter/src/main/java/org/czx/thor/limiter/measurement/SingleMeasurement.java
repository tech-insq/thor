package org.czx.thor.limiter.measurement;

import org.czx.thor.limiter.spi.Measurement;

import java.util.function.Function;

public class SingleMeasurement implements Measurement {
    private Number value = null;

    @Override
    public Number add(Number sample) {
        return value = sample;
    }

    @Override
    public Number get() {
        return value;
    }

    @Override
    public void reset() {
        value = null;
    }

    @Override
    public void update(Function<Number, Number> operation) {
        value = operation.apply(value);
    }
}
