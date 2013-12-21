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
public class LockFreeQueueTest {

    private static final Logger logger = LoggerFactory.getLogger(LockFreeQueueTest.class);

    public static final int THREADS = 41;
    public static final long MESSAGES = 99999L;
    public static final long SPACE    = MESSAGES + 1L;
    class Worker extends Thread {
        final SimpleQueue<Long> queue;
        final CountDownLatch startLock;

        Worker(SimpleQueue<Long> queue,
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
        final long i;
        public Sender(SimpleQueue<Long> queue,
                      String name,
                      CountDownLatch startLock,
                      long i)
        {
            super(queue, name, startLock);
            this.i = i;
        }

        @Override
        public void run() {
            super.run();
            for (long j = 0; j < MESSAGES; ++j) {
                queue.add(j + i * SPACE); // + ( i == 1 && j == 0 ? 1 : 0));
//                logger.info("put {}", (j + i * SPACE));
            }
        }
    }

    class Receiver extends Worker {
        final CountDownLatch endLock;
        final AtomicLong checkSum;
        public Receiver(SimpleQueue<Long> q, CountDownLatch endLock,
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
            List<Long> collector = new ArrayList<Long>();
            while (collector.size() < MESSAGES) {
                Long n = queue.takeOrNull();
                if (n == null) {
                    Thread.yield();
                } else {
                    collector.add(n);
//                    logger.info("get {}", n);
                }
            }
            for (Long n : collector) {
                checkSum.addAndGet(n);
            }
            endLock.countDown();
        }
    }

    @Test
    public void testLockFreeQueue() throws InterruptedException {
        SimpleQueue<Long> q = new LockFreeQueue<Long>();
        template(q);
    }

    @Test
    public void testLinkedBlockingQueue() throws InterruptedException {
        SimpleQueue<Long> q = new BlockingQueueAdapter<Long>();
        template(q);
    }

    public void template(SimpleQueue<Long> q) throws InterruptedException {
        logger.info("init; queue {}; threads = {}; messages per thread = {}",
                q.getClass().getSimpleName(), THREADS, MESSAGES);

        long started = System.currentTimeMillis();
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
        logger.info("end {} sec", (System.currentTimeMillis() - started) / 1000.0);
    }
}
