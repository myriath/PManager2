package com.example.mitch.pmanager.objects;

public class DecryptionObject {

    public String data;
    public boolean correctPassword;

    public DecryptionObject(String data, boolean correct) {
        this.correctPassword = correct;
        this.data = data;
    }
}
