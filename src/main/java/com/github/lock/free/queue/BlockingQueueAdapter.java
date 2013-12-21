package com.github.lock.free.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 */
public class BlockingQueueAdapter<T> implements SimpleQueue<T> {

    private final BlockingQueue<T> queue;

    public BlockingQueueAdapter() {
        queue = new LinkedBlockingQueue<T>();
    }

    @Override
    public void add(T x) {
        queue.add(x);
    }

    @Override
    public T takeOrNull() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
