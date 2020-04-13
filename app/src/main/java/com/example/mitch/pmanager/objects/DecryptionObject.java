package com.example.mitch.pmanager.objects;

public class DecryptionObject {

    public String data, name;
    public boolean correctPassword;

    public DecryptionObject(String data, boolean correct, String name) {
        this.data = data;
        this.correctPassword = correct;
        this.name = name;
    }
}
