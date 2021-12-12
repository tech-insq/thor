package org.czx.thor.demo;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WorkExecutor {
    private ThreadPoolExecutor executor;
    public WorkExecutor(int pools){
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(3000);
        AtomicInteger thSeq = new AtomicInteger(0);
        executor = new ThreadPoolExecutor(pools, pools, 5, TimeUnit.SECONDS, workQueue,
                r -> new Thread(r, String.format("ups.%d", thSeq.incrementAndGet())));
    }

    public int execute(Runnable command){
        try{
            executor.execute(command);
            return 0;
        }catch (Throwable t){
            return -1;
        }
    }

    public long getInQ(){
        return executor.getQueue().size();
    }

    public int getDoing(){
        return executor.getActiveCount();
    }

    private void await(){
       log.info("await >>>>>>>>:{}", getInQ());
       while (true){
           try {
               if (executor.awaitTermination(1, TimeUnit.SECONDS)) {
                   break;
               }
           }catch (Throwable t){

           }
       }
        log.info("await over:{}", getInQ());
    }

    public void stop(){
        log.info("executor.shutdown ......");
        executor.shutdown();
        log.info("executor.shutdown over");
        await();
    }
}
