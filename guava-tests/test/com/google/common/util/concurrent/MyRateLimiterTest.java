package com.google.common.util.concurrent;

import org.junit.Test;

import java.util.concurrent.*;

/**
 * https://zhuanlan.zhihu.com/p/439682111?utm_id=0
 */
public class MyRateLimiterTest {

    @Test
    public void testWarmUpRateLimiter() throws InterruptedException {
        // 每秒生成5个令牌，预热期设置为2s(即2s后令牌生成速度变为0.2s生成一个，在此之前，生成速度更慢) ，预热期令牌生成速度会低于稳定期
        RateLimiter rateLimiter = RateLimiter.create(5, 2, TimeUnit.SECONDS);
        Thread.sleep(2000);
        for (int i = 0; i < 20; i++) {
            //0.0
            //0.553163   开始时获取令牌耗时最长，随后逐渐减小
            //0.476724
            //0.396308
            //0.319991
            //0.237281
            //0.198168    2s后，获取令牌耗时 趋于稳定
            //0.195875
            //0.196701
            //...
            System.out.println(rateLimiter.acquire(1));
        }
    }
    @Test
    public void testWarmUpRateLimiter2() throws InterruptedException {
        // 每秒生成5个令牌，预热期设置为5s，预热期令牌生成速度会低于稳定期
        RateLimiter rateLimiter = RateLimiter.create(5, 5, TimeUnit.SECONDS);
        Thread.sleep(2000);
        int requests = 40;
        CountDownLatch countDownLatch = new CountDownLatch(requests);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    countDownLatch.countDown();
                    countDownLatch.await();
                    System.out.println(Thread.currentThread().getName() + ":" + rateLimiter.tryAcquire(1, 2, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        ExecutorService service = Executors.newFixedThreadPool(requests);
        for (int i = 0; i < requests; i++) {
            service.submit(runnable);
        }
        service.awaitTermination(60, TimeUnit.SECONDS);

    }

    /**
     * 业务上常规使用，1s内两个请求，qps=2
     */
    @Test
    public void testSmoothBurstyUse() throws InterruptedException {
        // 每秒可以通过5个请求，也即每秒放入5个令牌
        RateLimiter rateLimiter = RateLimiter.create(5);
        // 这里睡眠2s，令牌桶里也不会有10个令牌；
        // 因为框架中 maxBurstSeconds 强制设置为1，表示令牌桶只会保留1s的令牌数即5个
        // 令牌桶容量就是5
        Thread.sleep(2000);

        int requests = 20;
        CountDownLatch countDownLatch = new CountDownLatch(requests);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    countDownLatch.countDown();
                    countDownLatch.await();
                    // 前6个请求耗时都为0
                    // 前5个请求耗时为0，是因为桶里有5个令牌
                    // 第6个请求耗时为0，是因为"预支"原理，先将令牌预先取到，不耗时；
                    // 由于平均0.2s才生产1个令牌，所以下次请求获取令牌需要等待0.2s
                    System.out.println(Thread.currentThread().getName() + ":" + rateLimiter.acquire(1));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        ExecutorService service = Executors.newFixedThreadPool(requests);
        for (int i = 0; i < requests; i++) {
            service.submit(runnable);
        }
        service.awaitTermination(60, TimeUnit.SECONDS);
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
