package com.example.mitch.pmanager.interfaces;

import android.os.Bundle;

/**
 * Simple interface to allow backwards communication between classes
 */
public interface CallbackListener {

    /**
     * Handles callbacks from other classes
     *
     * @param args Args for the callback. Must include a CALLBACK_CODE
     */
    void callback(Bundle args);
}
