package com.example.mitch.pmanager.background;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Outdated encryption class for v2 encryption
 * INSECURE AND DEPRECATED! DO NOT USE FOR NEW FILES!
 */
@Deprecated
public class AES {
    /**
     * Cipher for this encryption
     */
    private final Cipher cipher;
    /**
     * Key for this encryption
     */
    private final SecretKeySpec key;

    /**
     * Constructor
     * @param keyString password to create a key from
     * @throws NoSuchPaddingException Thrown if padding errors occur
     * @throws NoSuchAlgorithmException Thrown if AES/CBC/PKCS5Padding is removed
     */
    public AES(String keyString) throws NoSuchPaddingException, NoSuchAlgorithmException {
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        key = new SecretKeySpec(keyBytes, "AES");
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    }

    /**
     * Encrypts the given string into the `out` file
     * @param toEncrypt String to encrypt
     * @param out Output file
     * @throws InvalidKeyException Thrown if the key is invalid (incorrect password)
     */
    public void encryptString(String toEncrypt, File out) throws InvalidKeyException {
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

    /**
     * Decrypts a given file
     * @param out File to decrypt
     * @return String of the decrypted data
     * @throws InvalidKeyException Thrown if the key is invalid (incorrect password)
     */
    public String decrypt(File out) throws InvalidKeyException {
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

    /**
     * Pads a given string to a proper key length
     * @param toPad String to pad
     * @return Key string padded with 0s
     */
    public static String pad(String toPad) {
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
}
