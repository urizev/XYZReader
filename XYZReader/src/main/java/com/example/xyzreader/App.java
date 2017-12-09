package com.example.xyzreader;

import android.app.Application;

import timber.log.Timber;

/**
 * Creado por jcvallejo en 9/12/17.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
