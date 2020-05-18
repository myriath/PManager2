package com.example.mitch.pmanager.background;

import com.example.mitch.pmanager.objects.PasswordEntry;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

public class DecryptionObject {
    public ArrayList<PasswordEntry> read(String key, File out) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        ArrayList<PasswordEntry> entries = new ArrayList<>();
        AES aes = new AES(AES.pad(key));
        String decrypted = aes.decrypt(out);
        String[] dataList = decrypted.split(System.lineSeparator());
        String[] entry = new String[3];
        int i2 = 0;
        int check0 = i2 * 3 + 1;
        int check1 = (dataList.length-1) / 3;
        while(i2 < check1) {
            System.arraycopy(dataList, check0, entry, 0, 3);
            i2++;
            entries.add(new PasswordEntry(entry[0], entry[1], entry[2], i2));
        }
        return entries;
    }
}
