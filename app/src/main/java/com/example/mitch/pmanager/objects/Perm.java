package com.example.mitch.pmanager.objects;

public class Perm {
    public String permission;
    public int code;

    public Perm(String perm, int request) {
        permission = perm;
        code = request;
    }
}
