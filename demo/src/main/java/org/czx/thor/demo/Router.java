package org.czx.thor.demo;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.czx.thor.limiter.LimiterFactory;
import org.czx.thor.limiter.spi.Limiter;
import org.czx.thor.limiter.spi.Limiter.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class Router {
    private static final Logger traceLog = LoggerFactory.getLogger("trace");
    private static final Logger statLog = LoggerFactory.getLogger("stat");
    private class UpsHandler implements Runnable{
        private WorkExecutor executor;
        private Session session;
        public UpsHandler(WorkExecutor executor, Session session){
            this.executor = executor;
            this.session = session;
        }
        @Override
        public void run() {
            doing(executor, session);
        }
    }

    private class Context{
        private long tps;
        private long timeLong;
        private long failed = 0;
        private long current = 0;
        private long drop = 0;
        public Context(long tps, long tl){
            this.tps = tps;
            this.timeLong = tl;
        }
    }

    private Limiter<Object> limiter;
    private AtomicLong totalRrt;
    private AtomicLong totalDoTime;
    private AtomicInteger totalDone;
    private AtomicInteger doingWorkNum;

    public Router(String name){
        limiter = LimiterFactory.createLimiter(name);
        totalRrt = new AtomicLong(0);
        totalDoTime = new AtomicLong(0);
        totalDone = new AtomicInteger(0);
        doingWorkNum = new AtomicInteger(0);
        Thread.currentThread().setName(name);
    }

    public void upstream(int upsType, int pools, long oldTps, long tl){
        long startTime = System.currentTimeMillis();
        WorkExecutor executor = new WorkExecutor(pools);
        Context context;
        if(upsType == 0){
            context = new Context(oldTps, 0);
            randUps(context, executor);
        }else if(upsType == 1){
            context = new Context(0, tl);
            continueUps(context, executor);
        }else{
            context = new Context(0, tl);
            limitUps(context, executor);
        }
        long inQ = executor.getInQ();
        long rTps =  context.tps;
        log.info("InQ={}, Failed={}, Drop={}, Done={}", inQ, context.failed, context.drop,
                (rTps - inQ - context.failed - context.drop));
        executor.stop();
        long success = rTps - context.failed - context.drop;
        int timeUsed = (int)((System.currentTimeMillis() - startTime)/1000);
        statLog.info("Stat >>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        statLog.info("Counter: total={}, failed={}, drop={}, success={}:{}, timeUsed={}, rate={}",
                rTps, context.failed, context.drop, success, totalDone.get(), timeUsed, (success * 1.0)/rTps);
        statLog.info("totalRrt={}, avg(rrt)={}, totalDoTime={}, avg(doTime)={}, totalDone={}",
                 totalRrt.get(), totalRrt.get()/totalDone.get(), totalDoTime.get(), totalDoTime.get()/totalDone.get(),
                 totalDone.get());
    }

    private void limitUps(Context context, WorkExecutor executor){
        log.info("limitUps >>>>");
        int print = 0;
        int reject = 0;
        context.tps = 0;
        RateLimiter rateLimiter = RateLimiter.create(10000);
        long endTime = System.currentTimeMillis() + context.timeLong;
        while (System.currentTimeMillis() < endTime){
            rateLimiter.acquire();
            context.tps = context.tps + 1;
            context.current = context.current + 1;
            Optional<Listener> optional = limiter.acquire(null);
            if(optional.isPresent()){
                Runnable upsHandler = new UpsHandler(executor, new Session(optional));
                if(executor.execute(upsHandler) != 0){
                    context.drop = context.drop + 1;
                    optional.get().onDropped();
                }
            }else{
                context.failed = context.failed + 1;
                reject = reject + 1;
            }

            print++;
            if(print > 10000){
                int cLimit = limiter.getLimit();
                int cInF = limiter.getInflight();
                int doingQ = executor.getDoing();
                int doingN = doingWorkNum.get();
                traceLog.info("Reject={}, Drop={}, Limit={}, InFli={}, Doing={}:{}, InQ={}",reject, context.drop, cLimit,
                        cInF, doingQ, doingN, executor.getInQ());
                print = 1;
            }
        }
    }

    private void continueUps(Context context, WorkExecutor executor){
        log.info("continueUps >>>>");
        int print = 0;
        int reject = 0;
        int current = 0;
        context.tps = 0;
        long endTime = System.currentTimeMillis() + context.timeLong;
        while (System.currentTimeMillis() < endTime){
            context.tps = context.tps + 1;
            context.current = context.current + 1;
            current++;
            Optional<Listener> optional = limiter.acquire(null);
            if(optional.isPresent()){
                Runnable upsHandler = new UpsHandler(executor, new Session(optional));
                if(executor.execute(upsHandler) != 0){
                    context.drop = context.drop + 1;
                    optional.get().onDropped();
                }
            }else{
                context.failed = context.failed + 1;
                reject = reject + 1;
            }

            print++;
            if(print > 10000){
                int cLimit = limiter.getLimit();
                int cInF = limiter.getInflight();
                int doingQ = executor.getDoing();
                int doingN = doingWorkNum.get();
                traceLog.info("Reject={}, Drop={}, Limit={}, InFli={}, Doing={}:{}, InQ={}",reject, context.drop, cLimit,
                        cInF, doingQ, doingN, executor.getInQ());
                print = 1;
            }
            if(current >= 200){
                sleep(5);
                current = 0;
                reject = 0;
            }
        }
    }

    private void randUps(Context context, WorkExecutor executor){
        log.info("randUps >>>>");
        int print = 0;
        Random random = new Random();
        while ((context.current < context.tps)){
            int jobs = getJobNum(random);
            print ++;
            if((context.current + jobs) > context.tps){
                jobs = (int)(context.tps - context.current);
            }

            context.current = context.current + jobs;
            int reject = 0;
            int cur = 0;
            int putQ = 0;
            int cLimit = limiter.getLimit();
            int cInF = limiter.getInflight();
            int doingQ = executor.getDoing();
            int doingN = doingWorkNum.get();

            while (cur < jobs){
                cur++;
                Optional<Listener> optional = limiter.acquire(null);
                if(optional.isPresent()){
                    Runnable upsHandler = new UpsHandler(executor, new Session(optional));
                    if(executor.execute(upsHandler) != 0){
                        context.drop = context.drop + 1;
                        optional.get().onDropped();
                    }else{
                        putQ ++;
                    }
                }else{
                    reject ++;
                }
            }
            if(print > 500){
                traceLog.info("JobsNum={},Reject={},Drop={}, Limit={}, InFli={}, Doing={}:{}, InQ={},PutQ={}", jobs, reject,
                        context.drop, cLimit, cInF, doingQ, doingN, executor.getInQ(), putQ);
                print = 1;
            }
            context.failed = context.failed + reject;
            sleep(10);
        }
    }

    private void doing(WorkExecutor executor, Session session){
        int active = doingWorkNum.incrementAndGet();
        int time = countTime(active, (System.currentTimeMillis() - session.getStartTime()));
        sleep(time);
        session.done(200);
        totalDone.incrementAndGet();
        totalDoTime.addAndGet(time);
        totalRrt.addAndGet(session.getRrt());
        doingWorkNum.decrementAndGet();
    }

    private static int countTime(int c, long l){
        int base = 10 + timeFunc(l, 250) + timeFunc(c, 20);
        return base;
    }

    private static int timeFunc(long t, int b){
        int n = (int)(t/b);
        double sum = 0;
        for(int i = 1; i < n; i++){
            sum = sum + Math.log(i);
        }
        return (int)(sum * 1.2);
    }

    private static int getJobNum(Random rand){
        int r = rand.nextInt(1000);
        if(r < 20){
            return 20;
        }else{
            return r;
        }
    }

    private static void sleep(int mis){
        try{
            Thread.sleep(mis);
        }catch (Throwable t){

        }
    }
}
