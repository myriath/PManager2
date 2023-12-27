package com.example.mitch.pmanager.models;

import java.util.ArrayList;

public class Entry {
    public interface Types {
        int BASIC = 0;
    }

    private int type;
    private char[] label;
    private char[] secret;

    public Entry(char[] label, char[] secret) {
        type = Types.BASIC;
        this.label = label;
        this.secret = secret;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public char[] getLabel() {
        return label;
    }

    public void setLabel(char[] label) {
        this.label = label;
    }

    public char[] getSecret() {
        return secret;
    }

    public void setSecret(char[] secret) {
        this.secret = secret;
    }
}
