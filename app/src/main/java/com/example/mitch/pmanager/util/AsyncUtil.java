package com.example.mitch.pmanager.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncUtil {
    public static final Object LOCK = new Object();
    private static AsyncUtil instance;
    private final Executor diskIO;
    private final Executor uiThread;

    private AsyncUtil(Executor diskIO, Executor uiThread) {
        this.diskIO = diskIO;
        this.uiThread = uiThread;
    }

    public static AsyncUtil async() {
        if (instance != null) return instance;
        synchronized (LOCK) {
            instance = new AsyncUtil(
                    Executors.newSingleThreadExecutor(),
                    new UIThreadExecutor()
            );
        }
        return instance;
    }

    public static Executor diskIO() {
        return async().diskIO;
    }

    public static Executor uiThread() {
        return async().uiThread;
    }

    private static class UIThreadExecutor implements Executor {
        private final Handler uiHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable runnable) {
            uiHandler.post(runnable);
        }
    }
}
