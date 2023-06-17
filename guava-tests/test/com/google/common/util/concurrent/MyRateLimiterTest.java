package com.google.common.util.concurrent;

import com.google.monitoring.runtime.instrumentation.common.util.concurrent.RateLimiter;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * https://zhuanlan.zhihu.com/p/439682111?utm_id=0
 */
public class MyRateLimiterTest {

    @Test
    public void testWarmUpRateLimiter(){
        // 每秒生成5个令牌，预热期设置为2s，预热期令牌生成速度会低于稳定期
        RateLimiter rateLimiter = RateLimiter.create(5, 3, TimeUnit.SECONDS);
        for (int i = 0; i < 20; i++) {
            System.out.println(rateLimiter.acquire(1));
        }

    }

    /**
     * 业务上常规使用，1s内两个请求，qps=2
     */
    @Test
    public void testNormalUse(){
        RateLimiter rateLimiter = RateLimiter.create(2);
        for (int i = 0; i < 20; i++) {
            System.out.println(rateLimiter.acquire(1));
        }
    }
    @Test
    public void test1(){
        RateLimiter rateLimiter = RateLimiter.create(2);
        double timeSpend1 = rateLimiter.acquire(100);
        System.out.println(timeSpend1);
        double timeSpend2 = rateLimiter.acquire(10);
        System.out.println(timeSpend2);
        double timeSpend3 = rateLimiter.acquire(2);
        System.out.println(timeSpend3);
    }
    @Test
    public void test2() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(5);
        CountDownLatch countDownLatch = new CountDownLatch(10);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000); // 积攒1s的令牌
                    countDownLatch.countDown();
                    countDownLatch.await();
                    System.out.println(Thread.currentThread().getName() + ":" + rateLimiter.acquire(1));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        ExecutorService service = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 10; i++) {
            service.submit(runnable);
        }
        service.awaitTermination(60, TimeUnit.SECONDS);


    }
}
