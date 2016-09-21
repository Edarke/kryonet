package com.esotericsoftware.kryonet.util;

import com.esotericsoftware.minlog.Log;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Created by Evan on 6/16/16.
 */
public class SameThreadListener<T> implements Consumer<T> {
    private T result = null;

    private final Object lock = new Object();

    /**Note: Null is not a valid response, although no explicit checking
     * is performed. May result in non-deterministic behavior.*/
    @Override
    public void accept(T response) {
        result = Objects.requireNonNull(response, "Query received null response");

        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**Calls wait() until a (Non-null) result is received. */
    public T waitForResult(Duration timeout) throws TimeoutException {
        try {
            synchronized (lock) {
                long curTime = System.currentTimeMillis();
                final long endTime = curTime + timeout.toMillis();
                while (result == null && curTime < endTime) {
                    lock.wait(endTime - curTime);
                    curTime = System.currentTimeMillis();
                }
                if (curTime >= endTime) {
                    throw new TimeoutException("Query did not receive response in time");
                }
            }
        } catch (InterruptedException e) {
            Log.error("Thread interrupted while waiting for response", e);
            Thread.currentThread().interrupt();
            throw new TimeoutException("Query did not receive response in time");
        }

        return Objects.requireNonNull(result, "Result is null");
    }

}
