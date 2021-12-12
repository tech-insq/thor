package org.czx.thor.limiter.measurement;

import org.czx.thor.limiter.spi.Measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TWMeasurement implements Measurement {
    private final int window;
    private final TimeUnit unit;
    private Number old;
    private long nextUpdateTime;
    private final Measurement measurement;
    private List<Measurement> listener;
    public TWMeasurement(int window, TimeUnit unit, Measurement measurement){
        this.window = window;
        this.unit = unit;
        this.old = 0;
        this.nextUpdateTime = getNextUpdateTime();
        this.measurement = measurement;
        this.listener = new ArrayList<>();
    }

    public void addListener(Measurement e){
        this.listener.add(e);
    }

    private long getNextUpdateTime(){
        return (System.currentTimeMillis() + unit.toMillis(window));
    }

    @Override
    public Number add(Number sample) {
        synchronized (this){
            checkAndUpdate();
            measurement.add(sample);
        }
        return old;
    }

    @Override
    public Number get() {
        checkAndUpdate();
        return old;
    }

    @Override
    public void reset() {
        measurement.reset();
        nextUpdateTime = getNextUpdateTime();
        old = 0;
    }

    @Override
    public void update(Function<Number, Number> operation) {
        old = operation.apply(old).doubleValue();
    }

    private void checkAndUpdate(){
        if(System.currentTimeMillis() >= nextUpdateTime){
            old = measurement.get();
            measurement.reset();
            nextUpdateTime = getNextUpdateTime();
            for(Measurement m:listener){
                m.add(old);
            }
        }
    }
}
