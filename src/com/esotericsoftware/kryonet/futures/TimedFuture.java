package com.esotericsoftware.kryonet.futures;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Evan on 3/25/17.
 */
public class TimedFuture<T> extends CompletableFuture<T> {

    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "KryoMs QueryTimeout");
                thread.setDaemon(true);
                return thread;
            });

    public static void addTimeout(CompletableFuture<?> future, Duration timeout) {
        timer.schedule(() -> future.cancel(false), timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

}
