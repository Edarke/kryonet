package com.esotericsoftware.kryonet.adapters;

import com.esotericsoftware.kryonet.network.Connection;
import com.esotericsoftware.minlog.Log;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is the abstract class version of Listener, similar to the
 * relationship between MouseListener and MouseAdapter, for example.
 * <p>
 * This class allows you to implement the only the methods you need and leave
 * the rest as nops.
 * <p>
 * Created by Evan on 6/9/16.
 */
public class ConnectionAdapter<C extends Connection> implements Listener<C> {


    @Override
    public void onConnected(C connection) {
        Log.debug("kryonet", "Connected with " + connection);
    }

    @Override
    public void onDisconnected(C connection) {
        Log.debug("kryonet", "Disconnected with " + connection);
    }


    @Override
    public void onIdle(C connection) {

    }

    @Override
    public void received(C connection, Object msg) {
    }


    /**
     * Wraps a listener and queues notifications as {@link Runnable runnables}. This allows the runnables to be processed on a
     * different thread, preventing the connection's update thread from being blocked.
     */
    public static abstract class QueuedListener<T extends Connection> extends ConnectionAdapter<T> {
        final Listener<T> listener;

        public QueuedListener(Listener<T> listener) {
            if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
            this.listener = listener;
        }

        @Override
        public void onConnected(final T connection) {
            queue(new Runnable() {
                public void run() {
                    listener.onConnected(connection);
                }
            });
        }

        @Override
        public void onDisconnected(final T connection) {
            queue(new Runnable() {
                public void run() {
                    listener.onDisconnected(connection);
                }
            });
        }

        @Override
        public void onIdle(final T connection) {
            queue(new Runnable() {
                public void run() {
                    listener.onIdle(connection);
                }
            });
        }

        @Override
        public void received(T connection, Object msg) {
            queue(new Runnable() {
                public void run() {
                    listener.received(connection, msg);
                }
            });
        }

        abstract protected void queue(Runnable runnable);
    }

    /**
     * This Listener delegates to another listener but (potentially) executes
     * {@link #onConnected(T)}, {@link #onDisconnected(T)}, {@link #onIdle(T)}, and {@link #received(T, Object)}
     * on a different thread.
     */
    public static class ThreadedListener<T extends Connection> extends QueuedListener<T> {
        protected final Executor threadPool;

        /**
         * Creates a single thread to process notification events.
         */
        public ThreadedListener(Listener<T> listener) {
            this(listener, Executors.newFixedThreadPool(1));
        }

        /**
         * Uses the specified executor to process notification events.
         */
        public ThreadedListener(Listener<T> listener, Executor threadPool) {
            super(listener);
            if (threadPool == null) throw new IllegalArgumentException("threadPool cannot be null.");
            this.threadPool = threadPool;
        }

        @Override
        public void queue(Runnable runnable) {
            threadPool.execute(runnable);
        }
    }

    /**
     * Delays the notification of the wrapped listener to simulate lag on incoming objects. Notification events are processed on a
     * separate thread after a delay. Note that only incoming objects are delayed. To delay outgoing objects, use a LagListener at
     * the other end of the connection.
     */
    public static class LagListener<T extends Connection> extends QueuedListener<T> {
        final LinkedList<Runnable> runnables = new LinkedList<>();
        private final ScheduledExecutorService threadPool;
        private final int lagMillisMin, lagMillisMax;

        public LagListener(int lagMillisMin, int lagMillisMax, Listener<T> listener) {
            super(listener);
            this.lagMillisMin = lagMillisMin;
            this.lagMillisMax = lagMillisMax;
            threadPool = Executors.newScheduledThreadPool(1);
        }

        @Override
        public void queue(Runnable runnable) {
            synchronized (runnables) {
                runnables.addFirst(runnable);
            }
            int lag = lagMillisMin + (int) (Math.random() * (lagMillisMax - lagMillisMin));
            threadPool.schedule(new Runnable() {
                public void run() {
                    Runnable runnable;
                    synchronized (runnables) {
                        runnable = runnables.removeLast();
                    }
                    runnable.run();
                }
            }, lag, TimeUnit.MILLISECONDS);
        }
    }
}
