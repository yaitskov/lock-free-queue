package com.github.lock.free.queue;

import java.util.concurrent.atomic.AtomicReference;

/**
*/
class Node<T> {
    final AtomicReference<Node<T>> next = new AtomicReference<Node<T>>();
    final T ref;
    Node(T ref) {
        this.ref = ref;
    }
}
