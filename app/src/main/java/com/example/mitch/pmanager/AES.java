package com.example.mitch.pmanager;

import android.os.Environment;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

class AES {

    private Cipher cipher;
    private SecretKeySpec key;

    AES(String keyString) throws NoSuchPaddingException, NoSuchAlgorithmException {
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        key = new SecretKeySpec(keyBytes, "AES");
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    }

    void encryptString(String toEncrypt, File out) throws InvalidKeyException {
        byte[] bytes = toEncrypt.getBytes(StandardCharsets.UTF_8);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        try (
                FileOutputStream fileOut = new FileOutputStream(out);
                CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)
        ) {
            fileOut.write(iv);
            cipherOut.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String decrypt(File out) throws InvalidKeyException {
        String content;
        try (FileInputStream fileIn = new FileInputStream(out)) {
            byte[] fileIv = new byte[16];
            //noinspection ResultOfMethodCallIgnored
            fileIn.read(fileIv);
            byte[] toDecrypt = new byte[(int)(out.length()-16)];
            //noinspection ResultOfMethodCallIgnored
            fileIn.read(toDecrypt);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(fileIv));
            byte[] decrypted = cipher.doFinal(toDecrypt);
            StringBuilder sb = new StringBuilder();
            for (byte b : decrypted) {
                if (b == '\0') {
                    sb.append(System.lineSeparator());
                } else {
                    sb.append((char)b);
                }
            }
            content = sb.toString();
        } catch (IOException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return "\0";
        }
        return content;
    }

    static String pad(String toPad) {
        StringBuilder padded = new StringBuilder(toPad);
        if (toPad.length() < 16) {
            for (int i = 0; i < 16 - toPad.length(); i++) {
                padded.append(0);
            }
        } else {
            for (int i = 0; i < toPad.length() % 16; i++) {
                System.out.println(toPad.length() % 16);
                padded.append(0);
            }
        }
        return padded.toString();
    }

//    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
//        AES aes = new AES(pad("gamer"));
//        String string = "gamer epic style\0hello";
//        aes.encryptString(string, "enc.txt");
//        AES decrypt = new AES(pad("gamer"));
//        String decrypted = decrypt.decrypt("enc.txt");
//        System.out.println(decrypted);
//    }
}
