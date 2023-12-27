package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.util.ByteCharStringUtil.bytesToChars;
import static com.example.mitch.pmanager.util.ByteCharStringUtil.splitByChar;
import static com.example.mitch.pmanager.util.Constants.BACKUP_EXTENSION;
import static com.example.mitch.pmanager.util.Constants.Version.V3;

import androidx.annotation.NonNull;

import com.example.mitch.pmanager.background.Encryptor;
import com.example.mitch.pmanager.interfaces.Writable;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.PasswordEntry;
import com.example.mitch.pmanager.objects.storage.PasswordBank;
import com.example.mitch.pmanager.objects.storage.UserEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

public class FileUtil {

    public static Comparator<File> FILE_COMPARATOR = Comparator.comparing(File::getName);

    /**
     * Copies the file at source to dest in 1MB chunks
     *
     * @param source Path to the source file
     * @param dest   Path to the destination file
     * @return True if the copy succeeded, false if it failed.
     */
    public static boolean copyFile(Path source, Path dest) {
        try (
                InputStream in = Files.newInputStream(source);
                OutputStream out = Files.newOutputStream(dest)
        ) {
            return copyFile(in, out);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Copies a file given an input stream and an output stream in 1MB chunks
     *
     * @param in  InputStream
     * @param out OutputStream
     * @return True if the transfer is successful
     * @throws IOException When error occurs
     */
    public static boolean copyFile(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        return true;
    }

    /**
     * Writes an encrypted file.
     *
     * @param writable       Object to write to the file
     * @param file           File to write to
     * @param associatedData Associated Data for the encryption
     * @param pwd            Password for the encryption
     * @return True if writing succeeds, false if it failed.
     */
    public static boolean writeFile(Writable writable, File file, byte[] associatedData, char[] pwd) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(writable);
            oos.flush();
            File backup = new File(file.getPath() + BACKUP_EXTENSION);
            if (file.exists() && !copyFile(file.toPath(), backup.toPath())) {
                throw new Exception("Couldn't make backup");
            }
            Encryptor.EncryptedData encrypted = Encryptor.encrypt(bos.toByteArray(), associatedData, pwd);
            Encryptor.writeEncrypted(encrypted, file);
            backup.delete();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean writeExternal(Writable writable, OutputStream out, byte[] ad, char[] pwd) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(writable);
            oos.flush();
            Encryptor.EncryptedData encrypted = Encryptor.encrypt(bos.toByteArray(), ad, pwd);
            Encryptor.writeEncryptedExternal(encrypted, out);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Decrypts and retrieves an object from a given file
     *
     * @param associatedData Associated data for the decryption
     * @param pwd            Password for the decryption
     * @param file           File to decrypt
     * @return Object of the data
     * @throws Exception Thrown if decryption fails.
     */
    public static PasswordBank readFile(byte[] associatedData, char[] pwd, File file) throws Exception {
        Encryptor.EncryptedData encrypted = Encryptor.readFromFile(file);
        byte[] data;
        try {
            data = Encryptor.decrypt(encrypted, associatedData, pwd);
        } catch (Exception e) {
            // Translates old v2 file to password bank
            String oldPath = file.getPath();
            int separator = oldPath.lastIndexOf('.');
//            if (!oldPath.substring(separator).equals(V2.ext))
            String newPath = oldPath.substring(0, oldPath.lastIndexOf('.')) + V3.ext;

            File oldFile = file;
            file = new File(newPath);

            PMFile pmFile = PMFile.translateV2toV3(oldFile, file, pwd);
            PasswordBank bank = PMFileToBank(pmFile);
            if (writeFile(bank, file, file.getName().getBytes(StandardCharsets.UTF_8), pwd)) {
                oldFile.delete();
            }
            return PMFileToBank(pmFile);
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object read = ois.readObject();
            if (read.getClass() == PMFile.class) {
                ((PMFile) read).setFile(file);
                return PMFileToBank((PMFile) read);
            }
            return (PasswordBank) read;
        } catch (Exception e) {
            return PMFileToBank(parseV2Data(data, file));
        }
    }

    /**
     * Parses a byte[] of data into a PMFile for use with the rest of the program.
     *
     * @param data Data to parse
     * @return Processed PMFile for easy use in the program
     */
    public static PMFile parseV2Data(byte[] data, File file) {
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
     * Converts a PMFile to a password bank
     *
     * @param file PMFile to convert
     * @return converted PasswordBank
     */
    public static PasswordBank PMFileToBank(PMFile file) {
        PasswordBank bank = new PasswordBank();
        for (PasswordEntry entry : file.getPasswordEntries()) {
            String domain = String.valueOf(entry.domain);
            bank.createDomain(domain).add(new UserEntry(entry.username, entry.password));
        }
        return bank;
    }

}
