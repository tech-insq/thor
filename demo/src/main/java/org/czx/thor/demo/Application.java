package org.czx.thor.demo;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class Application {
    public static void main(String []args){
        log.info("Testing >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        long WAN = 10000;
        long TEN_MIN = TimeUnit.MINUTES.toMillis(10);
        String[] algths = new String[]{"aimd","vaimd","vegas", "window", "gradient", "gradient2", "fix"};
        //String[] algths = new String[]{"aimd","vaimd"};
        for(String alg:algths){
            log.info("Run alg:{} ......", alg);
            Router router = new Router(alg);
            router.upstream(false,100, 500 * WAN, TEN_MIN);
            log.info("Run alg:{} over", alg);
        }
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }
}
