package org.czx.thor.limiter.measurement;

import org.czx.thor.limiter.spi.Measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CWMeasurement implements Measurement {
    private Number old;
    private int cwd;
    private final int window;
    private final Measurement measurement;
    private final List<Measurement> listener;
    public CWMeasurement(int window, Measurement measurement){
        this.cwd = 0;
        this.old = 0;
        this.window = window;
        this.measurement = measurement;
        this.listener = new ArrayList<>();
    }

    public void addListener(Measurement e){
        this.listener.add(e);
    }

    @Override
    public Number add(Number sample) {
        synchronized (this){
            checkAndUpdate();
            measurement.add(sample);
            cwd++;
        }
        return old;
    }

    @Override
    public Number get() {
        return old;
    }

    @Override
    public void reset() {
        measurement.reset();
        cwd = 0;
        old = 0;
    }

    @Override
    public void update(Function<Number, Number> operation) {
        old = operation.apply(old).doubleValue();
    }

    private void checkAndUpdate(){
        if(cwd >= window){
            old = measurement.get();
            measurement.reset();
            cwd = 1;
            for(Measurement m:listener){
                m.add(old);
            }
        }
    }
}
