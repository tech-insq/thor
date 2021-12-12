package org.czx.thor.limiter.bbr;

import lombok.extern.slf4j.Slf4j;
import org.czx.thor.limiter.spi.Limit;

import java.util.function.Consumer;

@Slf4j
public class BBrLimit implements Limit {
    @Override
    public int getLimit() {
        return 0;
    }

    @Override
    public void notifyOnChange(Consumer<Integer> consumer) {

    }

    @Override
    public void onSample(long startTime, long rtt, int inflight, boolean didDrop) {

    }
}
