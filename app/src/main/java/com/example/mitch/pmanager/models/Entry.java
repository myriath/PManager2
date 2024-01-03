package com.example.mitch.pmanager.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

/**
 * Data entry for an entry in a folder
 * Can have several types for future updates
 *
 * @author mitch
 */
public class Entry implements Parcelable {
    /**
     * Types interface for the different entry types
     */
    public interface Types {
        /**
         * Basic type consists of a label and a secret that is hidden by default to the user
         */
        int BASIC = 0;
    }

    /**
     * Type of the entry
     */
    @Expose
    private int type;
    /**
     * Label for BASIC entries
     */
    @Expose
    private char[] label;
    /**
     * Secret for BASIC entries
     */
    @Expose
    private char[] secret;

    public Entry(int type) {
        this.type = type;
        switch (type) {
            case Types.BASIC: {
                this.label = new char[0];
                this.secret = new char[0];
                break;
            }
        }
    }

    /**
     * Constructs a BASIC entry given a label and secret
     *
     * @param label  Label for the entry
     * @param secret Secret for the entry
     */
    public Entry(char[] label, char[] secret) {
        type = Types.BASIC;
        this.label = label;
        this.secret = secret;
    }

    protected Entry(Parcel in) {
        type = in.readInt();
        label = in.createCharArray();
        secret = in.createCharArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeCharArray(label);
        dest.writeCharArray(secret);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Entry> CREATOR = new Creator<Entry>() {
        @Override
        public Entry createFromParcel(Parcel in) {
            return new Entry(in);
        }

        @Override
        public Entry[] newArray(int size) {
            return new Entry[size];
        }
    };

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
