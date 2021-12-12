package org.czx.thor.limiter.bbr;

import lombok.extern.slf4j.Slf4j;
import org.czx.thor.limiter.measurement.AvgMeasurement;
import org.czx.thor.limiter.measurement.CWMeasurement;
import org.czx.thor.limiter.measurement.ExpMovAvgMeasurement;
import org.czx.thor.limiter.measurement.PercentMeasurement;
import org.czx.thor.limiter.netflix.internal.Preconditions;
import org.czx.thor.limiter.netflix.limit.AbstractLimit;
import org.czx.thor.limiter.spi.Measurement;

import java.util.concurrent.TimeUnit;

@Slf4j
public class VAIMDLimit extends AbstractLimit {
    private static final int NTM = (int)TimeUnit.MILLISECONDS.toNanos(1);
    public static class Builder {
        private double decay = 0.995;
        private int minLimit = 20;
        private int initialLimit = 20;
        private double backoffRatio = 0.9;
        private int windows = 500;

        public Builder decay(double decay){
            this.decay = decay;
            return this;
        }

        public Builder minLimit(int minLimit) {
            this.minLimit = minLimit;
            return this;
        }

        public Builder initialLimit(int initialLimit) {
            this.initialLimit = initialLimit;
            return this;
        }

        public Builder backoffRatio(double backoffRatio) {
            Preconditions.checkArgument(backoffRatio < 1.0 && backoffRatio >= 0.5, "Backoff ratio must be in the range [0.5, 1.0)");
            this.backoffRatio = backoffRatio;
            return this;
        }

        public Builder windows(int windows){
            this.windows = windows;
            return this;
        }

        public VAIMDLimit build() {
            return new VAIMDLimit(this);
        }
    }

    public static VAIMDLimit.Builder newBuilder() {
        return new VAIMDLimit.Builder();
    }

    private final CWMeasurement avgRtt;
    private final CWMeasurement dropMeasurement;
    private final ExpMovAvgMeasurement oldBestRtt;
    private final double backoffRatio;
    private final int minLimit;

    private VAIMDLimit(Builder builder) {
        super(builder.initialLimit);
        AvgMeasurement avgMeasurement = new AvgMeasurement();
        Measurement pMeasurement = new PercentMeasurement();
        this.avgRtt = new CWMeasurement(builder.windows, avgMeasurement);
        this.dropMeasurement = new CWMeasurement(builder.windows, pMeasurement);
        this.oldBestRtt = new ExpMovAvgMeasurement(0.9);
        this.backoffRatio = builder.backoffRatio;
        this.minLimit = builder.minLimit;
        this.avgRtt.addListener(this.oldBestRtt);
    }

    @Override
    protected int _update(long startTime, long rtt, int inflight, boolean didDrop) {
        long rttMil = (rtt)/NTM;
        int currentLimit = getLimit();

        if(!didDrop){
            avgRtt.add(rttMil);
        }
        long curRtt = avgRtt.get().longValue();
        long bestRtt = oldBestRtt.get().longValue();
        double dropPercent = dropMeasurement.add(((didDrop)? 1:0)).doubleValue();

        if(isBad(dropPercent, curRtt, bestRtt)){
            /**没有最大现在，通过前面的降速**/
            int oldLimit = currentLimit;
            currentLimit = currentLimit / 2;
            log.debug("isBad: drop={} curRtt={}, oldRtt={}, rtt={},limit={}->{},didDrop={}", dropPercent, curRtt, bestRtt,
                    rttMil, oldLimit, currentLimit, didDrop);
        }else if((dropPercent >= 0.1) || isSlow(curRtt, bestRtt)){
            int oldLimit = currentLimit;
            currentLimit = (int) (currentLimit * backoffRatio);
            log.debug("isSlow: drop={} curRtt={}, oldRtt={}, rtt={},limit={}->{},didDrop={}", dropPercent, curRtt, bestRtt,
                    rttMil, oldLimit, currentLimit, didDrop);
        }else{
            currentLimit =  currentLimit + 1;
        }

        return Math.max(minLimit, currentLimit);
    }

    private boolean isSlow(long curRtt, long oldAvgRtt){
        if(curRtt == oldAvgRtt){
            return false;
        }
        return (curRtt >= (120 * oldAvgRtt)/100);
    }

    private boolean isBad(double drop, long curRtt, long oldAvgRtt){
        if(drop >= 0.3){
            return true;
        }

        if(curRtt == oldAvgRtt){
            return false;
        }
        return (curRtt >= (2 * oldAvgRtt));
    }
}
