package com.example.mitch.pmanager.exceptions;

public class IllegalThreadException extends IllegalStateException {
    public IllegalThreadException() {
        super("Executing on wrong thread...");
    }
}
