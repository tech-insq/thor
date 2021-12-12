package org.czx.thor.limiter.measurement;

import org.czx.thor.limiter.spi.Measurement;

import java.util.function.Function;

public class ExpMovAvgMeasurement implements Measurement {
    private Double shadowVariable;
    private Double decay;

    public ExpMovAvgMeasurement(Double decay){
        shadowVariable = Double.valueOf("0.0");
        this.decay = decay;
    }

    @Override
    public Number add(Number sample) {
        shadowVariable = shadowVariable * decay + (1 - decay) * sample.doubleValue();
        return shadowVariable;
    }

    @Override
    public Number get() {
        return shadowVariable;
    }

    @Override
    public void reset() {
        shadowVariable = Double.valueOf("0.0");
    }

    @Override
    public void update(Function<Number, Number> operation) {
        this.shadowVariable = operation.apply(this.shadowVariable).doubleValue();
    }
}
