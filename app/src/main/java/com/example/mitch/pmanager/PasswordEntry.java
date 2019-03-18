package com.example.mitch.pmanager;

import java.io.Serializable;

class PasswordEntry implements Serializable {

    String domain;
    String username;
    String password;
    int index;

    PasswordEntry(String t, String a, String r, int i) {
        domain = t;
        username = a;
        password = r;
        index = i;
    }
}
