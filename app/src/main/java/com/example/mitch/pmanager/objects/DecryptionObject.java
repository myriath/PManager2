package com.example.mitch.pmanager.objects;

public class DecryptionObject {

    public String data, name;
    public boolean correctPassword;

    public DecryptionObject(String data, boolean correct, String name) {
        this.correctPassword = correct;
        this.data = data;
        this.name = name;
    }

    public String getGeneric() {
        return data.substring(name.length() + System.lineSeparator().length());
    }
}
