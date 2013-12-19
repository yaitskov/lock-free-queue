package com.github.lock.free.queue;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Daneel Yaitskov
 */
public class LockFreeQueue<T> {

    private static final Logger logger = LoggerFactory.getLogger(LockFreeQueue.class);

    // never empty
    private final AtomicLong length = new AtomicLong(1L);
    private final Node stub = new Node(null);
    private final AtomicReference<Node<T>> head = new AtomicReference<Node<T>>(stub);
    private final AtomicReference<Node<T>> tail = new AtomicReference<Node<T>>(stub);

    public void add(T x) {
        logger.info("put {}", x);
        addNode(new Node<T>(x));
        length.incrementAndGet();
    }

    public T takeOrNull() {
        while (true) {
            long l = length.get();
            if (l == 1) {
                logger.info("null cause length is 1");
                return null;
            }
            if (length.compareAndSet(l, l - 1)) {
                logger.info("changed length is {}", l);
                break;
            } else {
                logger.info("failed change length");
            }
        }
        while (true) {
            Node<T> r = head.get();
            logger.info("head {}, next", r);
            if (head.compareAndSet(r, r.next.get())) {
                if (r == stub) {
                    logger.info("took stub retry");
                    stub.next.set(null);
                    addNode(stub);
                } else {
                    logger.info("took {}", r.ref);
                    return r.ref;
                }
            } else {
                logger.info("failed take head");
            }
        }
    }

    private void addNode(Node<T> n) {
        Node<T> t;
        while (true) {
            t = tail.get();
            if (tail.compareAndSet(t, n)) {
                logger.info("put");
                break;
            } else {
                logger.info("failed put");
            }
        }
        if (t.next.compareAndSet(null, n)) {
            return;
        }
        throw new IllegalStateException("bad tail next");
    }
}