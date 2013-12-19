package com.github.lock.free.queue;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Daneel Yaitskov
 */
@Ignore
public class LockFreeQueueTest {

    private static final Logger logger = LoggerFactory.getLogger(LockFreeQueueTest.class);

    public static final int THREADS = 129;
    public static final int MESSAGES = 39;
    public static final int SPACE = 100;

    class Worker extends Thread {
        final LockFreeQueue<Integer> queue;
        final CountDownLatch startLock;

        Worker(LockFreeQueue<Integer> queue,
               String name,
               CountDownLatch startLock)
        {
            super(name);
            this.queue = queue;
            this.startLock = startLock;
        }

        @Override
        public void run() {
            try {
                startLock.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class Sender extends Worker {
        final int i;
        public Sender(LockFreeQueue<Integer> queue,
                      String name,
                      CountDownLatch startLock,
                      int i)
        {
            super(queue, name, startLock);
            this.i = i;
        }

        @Override
        public void run() {
            super.run();
            for (int j = 0; j < MESSAGES; ++j) {
                queue.add(j + i * SPACE);
//                logger.info("put {}", (j + i * SPACE));
            }
        }
    }

    class Receiver extends Worker {
        final CountDownLatch endLock;
        final AtomicLong checkSum;
        public Receiver(LockFreeQueue<Integer> q, CountDownLatch endLock,
                        String name,
                        CountDownLatch startLatch, AtomicLong checkSum)
        {
            super(q, name, startLatch);
            this.checkSum = checkSum;
            this.endLock = endLock;
        }

        @Override
        public void run() {
            super.run();
            List<Integer> collector = new ArrayList<Integer>();
            while (collector.size() < MESSAGES) {
                Integer n = queue.takeOrNull();
                if (n == null) {
                    Thread.yield();
                } else {
                    collector.add(n);
//                    logger.info("get {}", n);
                }
            }
            for (Integer n : collector) {
                checkSum.addAndGet(n);
            }
            endLock.countDown();
        }
    }

    @Test
    public void test() throws InterruptedException {
        logger.info("init threads = {}; messages per thread = {}",
                THREADS, MESSAGES);
        LockFreeQueue<Integer> q = new LockFreeQueue<Integer>();

        final AtomicLong checkSum = new AtomicLong();
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch waitLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; ++i) {
            new Sender(q, "s" + i, startLatch, i).start();
            new Receiver(q, waitLatch, "r" + i, startLatch, checkSum).start();
        }

        logger.info("launching");
        startLatch.countDown();
        logger.info("await finish");
        waitLatch.await();
        logger.info("checking");
        long sum = 0;
        for (int i = 0; i < THREADS; ++i) {
            for (int j = 0; j < MESSAGES; ++j) {
                sum += j + i * SPACE;
            }
        }
        Assert.assertEquals(sum, checkSum.get());
        logger.info("end");
    }
}
