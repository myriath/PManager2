package com.example.mitch.pmanager.objects;

import static com.example.mitch.pmanager.util.Constants.STRING_ENCODING;
import static com.example.mitch.pmanager.util.FileUtil.parseV2Data;

import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.exceptions.DecryptionException;
import com.example.mitch.pmanager.interfaces.Writable;
import com.example.mitch.pmanager.util.Constants;

import java.io.File;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

/**
 * Data class represents a saved file
 */
public class PMFile implements Serializable, Writable {
    /**
     * File to write the encrypted data to
     */
    private File file;
    /**
     * File version for future-proofing
     *
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
     * Getter for the output file
     * @return output file
     */
    public File getFile() {
        return file;
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
     *
     * @return password entries
     */
    public ArrayList<PasswordEntry> getPasswordEntries() {
        return passwordEntries;
    }

    /**
     * Decrypts a v2 file, then re-encrypts it as a v3 file
     *
     * @param in  v2 file for input
     * @param out v3 file to output to
     * @param pwd password char[]
     * @return True if the translation succeeded, false if not.
     * @throws Exception Exception thrown by v3 encryption errors.
     */
    public static PMFile translateV2toV3(File in, File out, char[] pwd) throws Exception {
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
        }

        String filename = out.getName();
        data = filename + data.substring(data.indexOf(System.lineSeparator()));
        return parseV2Data(data.getBytes(STRING_ENCODING), out);
    }


    private static final long serialVersionUID = 1L;
}
