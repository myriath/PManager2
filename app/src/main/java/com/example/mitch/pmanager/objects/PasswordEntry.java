package com.example.mitch.pmanager.objects;

import java.io.Serializable;

public class PasswordEntry implements Serializable {

    public String domain;
    public String username;
    public String password;
    public int index;

    public PasswordEntry(String t, String a, String r, int i) {
        domain = t;
        username = a;
        password = r;
        index = i;
    }
}
