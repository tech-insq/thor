package org.czx.thor.demo;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class Application {
    public static void main(String []args){
        log.info("Testing >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        long WAN = 10000;
        long TEN_MIN = TimeUnit.MINUTES.toMillis(3);
        String[] algths = new String[]{"aimd","vaimd","vegas", "window", "gradient", "gradient2", "fix"};
        for(int type = 0; type < 3; type++) {
            for (String alg : algths) {
                log.info("Run alg:{} ......", alg);
                Router router = new Router(alg);
                router.upstream(type, 100, 500 * WAN, TEN_MIN);
                log.info("Run alg:{} over", alg);
            }
        }
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }
}
