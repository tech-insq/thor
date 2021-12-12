package org.czx.thor.limiter.measurement;

import org.czx.thor.limiter.spi.Measurement;

import java.util.function.Function;

public class PercentMeasurement implements Measurement {
    private int count;
    private int value;
    public PercentMeasurement(){
        count = 0;
        value = 0;
    }

    @Override
    public Number add(Number sample) {
        synchronized (this) {
            count++;
            value = value + ((sample.intValue() > 0)? 1:0);
            return getPercent();
        }
    }

    @Override
    public Number get() {
        return getPercent();
    }

    @Override
    public void reset() {
        count = 0;
        value = 0;
    }

    @Override
    public void update(Function<Number, Number> operation) {
    }

    private double getPercent(){
        return ((count == 0)? 0:((value *1.0)/count));
    }
}
