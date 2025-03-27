package io.hhplus.tdd.concurrent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class LockExecutor {

    private static final ConcurrentHashMap<Long, Lock> userIdLocks = new ConcurrentHashMap<>();

    private Lock getUserLock(long id) {
        return userIdLocks.computeIfAbsent(id, key -> new ReentrantLock(false));
    }

    public <T> T executeWithUserLock(long userId, Supplier<T> task) {
        Lock lock = getUserLock(userId);
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }
}
