package com.example.mitch.pmanager.objects;

import static com.example.mitch.pmanager.Constants.Version.V3;
import static com.example.mitch.pmanager.Util.bytesToChars;
import static com.example.mitch.pmanager.Util.splitByChar;

import com.example.mitch.pmanager.Constants;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.background.Encryptor;
import com.example.mitch.pmanager.exceptions.DecryptionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

/**
 * Data class represents a saved file
 */
public class PMFile implements Serializable {
    /**
     * File to write the encrypted data to
     */
    private transient File file;
    /**
     * File version for future-proofing
     * @serial
     */
    private final Constants.Version version;
    /**
     * Password entries arraylist for the stored data
     * @serial
     */
    private final ArrayList<PasswordEntry> passwordEntries;

    /**
     * Constructor
     * @param version Version for this file
     * @param entries Password entries
     */
    public PMFile(Constants.Version version, ArrayList<PasswordEntry> entries, File file) {
        this.version = version;
        this.passwordEntries = entries;
        this.file = file;
    }

    /**
     * Setter for the output file
     * @param file File to write to
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Getter for the version
     * @return version number
     */
    public Constants.Version getVersion() {
        return version;
    }

    /**
     * Getter for the password entries
     * @return password entries
     */
    public ArrayList<PasswordEntry> getPasswordEntries() {
        return passwordEntries;
    }

    /**
     * Decrypts and fills out a PMFile object from a given file
     * @param associatedData Associated data for the decryption
     * @param pwd Password for the decryption
     * @param file File to decrypt
     * @return PMFile of the file's contents
     * @throws Exception Thrown if decryption fails.
     */
    public static PMFile readFile(byte[] associatedData, char[] pwd, File file) throws Exception {
        Encryptor.EncryptedData encrypted = Encryptor.readFromFile(file);
        byte[] data = Encryptor.decrypt(encrypted, associatedData, pwd);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bis)) {
            PMFile pmFile = (PMFile) ois.readObject();
            pmFile.setFile(file);
            return pmFile;
        } catch (Exception e) {
            return parseV2Data(data, file);
        }
    }

    /**
     * Writes an encrypted file.
     * @param associatedData Associated Data for the encryption
     * @param pwd Password for the encryption
     * @return True if writing succeeds, false if it failed.
     */
    public boolean writeFile(byte[] associatedData, char[] pwd) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            oos.flush();
            Encryptor.EncryptedData encrypted = Encryptor.encrypt(bos.toByteArray(), associatedData, pwd);
            Encryptor.writeEncrypted(encrypted, file);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Parses a byte[] of data into a PMFile for use with the rest of the program.
     * @param data Data to parse
     * @return Processed PMFile for easy use in the program
     */
    private static PMFile parseV2Data(byte[] data, File file) {
        ArrayList<PasswordEntry> entries = new ArrayList<>();
        char[][] dataList = splitByChar(bytesToChars(data), '\n');
        int entryCount = (dataList.length - 1) / 3;
        for (int i = 0; i < entryCount; i++) {
            int entryIndex = i * 3 + 1;
            entries.add(new PasswordEntry(dataList[entryIndex], dataList[entryIndex + 1], dataList[entryIndex + 2], i + 1));
        }
        return new PMFile(V3, entries, file);
    }

    /**
     * Decrypts a v2 file, then re-encrypts it as a v3 file
     * @param in v2 file for input
     * @param out v3 file to output to
     * @param pwd password char[]
     * @throws Exception Exception thrown by v3 encryption errors.
     * @return True if the translation succeeded, false if not.
     */
    public static boolean translateV2toV3(File in, File out, char[] pwd) throws Exception {
        String data;
        try {
            String[] splitFile;
            AES decrypt = new AES(AES.pad(String.valueOf(pwd)));
            data = decrypt.decrypt(in);
            splitFile = data.split(System.lineSeparator());

            if (!splitFile[0].equals(in.getName())) {
                translateV2toV3(in, out, pwd);
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DecryptionException("");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String filename = out.getName();
        data = filename + data.substring(data.indexOf(System.lineSeparator()));
        PMFile pmFile = parseV2Data(data.getBytes(StandardCharsets.UTF_8), out);
        byte[] ad = filename.getBytes(StandardCharsets.UTF_8);

        return pmFile.writeFile(ad, pwd);
    }

    private static final long serialVersionUID = 1L;
}
