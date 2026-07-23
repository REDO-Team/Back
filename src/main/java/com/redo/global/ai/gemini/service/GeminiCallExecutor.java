package com.redo.global.ai.gemini.service;

import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class GeminiCallExecutor {

    private static final int THREAD_COUNT = 4;

    private final Duration timeout;
    private final ExecutorService executorService;

    public GeminiCallExecutor(Duration timeout) {
        this(timeout, Executors.newFixedThreadPool(THREAD_COUNT, daemonThreadFactory()));
    }

    GeminiCallExecutor(Duration timeout, ExecutorService executorService) {
        this.timeout = timeout;
        this.executorService = executorService;
    }

    public <T> T execute(Callable<T> task) {
        Future<T> future = executorService.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new GeminiException(GeminiErrorCode.TIMEOUT, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GeminiException(GeminiErrorCode.PROVIDER_ERROR, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new GeminiException(GeminiErrorCode.PROVIDER_ERROR, cause);
        }
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    private static ThreadFactory daemonThreadFactory() {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "gemini-call-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
