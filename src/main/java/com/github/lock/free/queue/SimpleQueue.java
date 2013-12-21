package com.github.lock.free.queue;

/**
 */
public interface SimpleQueue<T> {
    void add(T x);

    T takeOrNull();
}
