package org.czx.thor.demo;

import lombok.Data;
import org.czx.thor.limiter.spi.Limiter;
import java.util.Optional;

@Data
public class Session {
    private long startTime;
    private int rrt;
    private int status;
    private Optional<Limiter.Listener> listener;
    public Session(Optional<Limiter.Listener> listener){
        startTime = System.currentTimeMillis();
        status = 0;
        this.listener = listener;
    }

    public void done(int status){
        rrt = (int)(System.currentTimeMillis() - startTime);
        this.status = status;
        switch (status){
            case 0:
                listener.get().onIgnore();
                break;
            case 200:
                listener.get().onSuccess();
                break;
            case 500:
                listener.get().onDropped();
                break;
        }
    }
}
