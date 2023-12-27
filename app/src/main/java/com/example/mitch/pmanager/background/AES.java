package com.example.mitch.pmanager.background;

import static com.example.mitch.pmanager.util.ByteCharStringUtil.splitByChar;

import android.util.Log;

import com.example.mitch.pmanager.models.Entry;
import com.example.mitch.pmanager.models.Folder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
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
     * Algorithm string for key type
     */
    private static final String ALG_STRING = "AES";
    /**
     * Algorithm string for encryption cipher
     */
    private static final String CIPHER_STRING = "AES/CBC/PKCS5Padding";

    /**
     * Constructor
     * @param keyString password to create a key from
     * @throws NoSuchPaddingException Thrown if padding errors occur
     * @throws NoSuchAlgorithmException Thrown if AES/CBC/PKCS5Padding is removed
     */
    public AES(String keyString) throws NoSuchPaddingException, NoSuchAlgorithmException {
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        key = new SecretKeySpec(keyBytes, ALG_STRING);
        cipher = Cipher.getInstance(CIPHER_STRING);
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
     *
     * @param out File to decrypt
     * @return String of the decrypted data
     * @throws InvalidKeyException Thrown if the key is invalid (incorrect password)
     */
    public String decrypt(File out) throws Exception {
        String content;
        try (FileInputStream fileIn = new FileInputStream(out)) {
            byte[] fileIv = new byte[16];
            //noinspection ResultOfMethodCallIgnored
            fileIn.read(fileIv);
            byte[] toDecrypt = new byte[(int) (out.length() - 16)];
            //noinspection ResultOfMethodCallIgnored
            fileIn.read(toDecrypt);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(fileIv));
            byte[] decrypted = cipher.doFinal(toDecrypt);
            StringBuilder sb = new StringBuilder();
            for (byte b : decrypted) {
                if (b == '\0') {
                    sb.append(System.lineSeparator());
                } else {
                    sb.append((char) b);
                }
            }
            content = sb.toString();
        }
        return content;
    }

    /**
     * Updated method for new translation
     */
    public String decrypt(InputStream in) throws Exception {
        byte[] fileIv = new byte[16];
        in.read(fileIv);

        int i;
        byte[] buf = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((i = in.read(buf, 0, buf.length)) != -1) {
            baos.write(buf, 0, i);
        }
        byte[] toDecrypt = baos.toByteArray();

        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(fileIv));
        byte[] decrypted = cipher.doFinal(toDecrypt);
        StringBuilder sb = new StringBuilder();
        for (byte b : decrypted) {
            if (b == '\0') {
                sb.append(System.lineSeparator());
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    /**
     * Parses an input stream into a string of decrypted data with filename check.
     * @param in encrypted data
     * @param filename filename to check
     * @param password password to decrypt with
     * @return Decrypted string
     * @throws Exception Thrown when decryption fails, usually wrong password
     */
    public static String parse(InputStream in, String filename, char[] password) throws Exception {
        String data;
        String[] splitFile;
        if (in == null) throw new IOException("In is null");
        AES decrypt = new AES(pad(String.valueOf(password)));
        data = decrypt.decrypt(in);
        Log.i("Decrypted", data);
        splitFile = data.split(System.lineSeparator());

        if (!splitFile[0].equals(filename)) {
            throw new Exception();
        }
        return data;
    }

    /**
     * Updates data string from V2 to a folder hashmap in V4
     * @param data Data to update
     * @param folders V4 folders hashmap
     */
    public static void modernize(String data, HashMap<String, Folder> folders) {
        char[][] splitData = splitByChar(data.toCharArray(), '\n');
        int entryCount = (splitData.length - 1) / 3;
        for (int j = 0; j < entryCount; j++) {
            int entryIndex = j * 3 + 1;
            String label = String.valueOf(splitData[entryIndex]);
            Entry entry = new Entry(splitData[entryIndex+1], splitData[entryIndex+2]);
            try {
                Folder folder = Objects.requireNonNull(folders.get(label));
                folder.getEntries().add(entry);
            } catch (Exception e) {
                Folder folder = new Folder(label);
                folder.getEntries().add(entry);
                folders.put(label, folder);
            }
        }
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
