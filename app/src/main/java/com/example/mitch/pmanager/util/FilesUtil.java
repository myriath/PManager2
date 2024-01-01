package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.activities.LoginActivity.toast;
import static com.example.mitch.pmanager.models.FileKey.generateSalt;
import static com.example.mitch.pmanager.util.AsyncUtil.diskIO;
import static com.example.mitch.pmanager.util.AsyncUtil.uiThread;
import static com.example.mitch.pmanager.util.Constants.Encryption.GCM_IV_LENGTH;
import static com.example.mitch.pmanager.util.Constants.Encryption.GCM_IV_LENGTH_OLD;
import static com.example.mitch.pmanager.util.Constants.Encryption.SALT_LENGTH;
import static com.example.mitch.pmanager.util.Constants.Extensions.SHM;
import static com.example.mitch.pmanager.util.Constants.Extensions.V2;
import static com.example.mitch.pmanager.util.Constants.Extensions.V3;
import static com.example.mitch.pmanager.util.Constants.Extensions.V4;
import static com.example.mitch.pmanager.util.Constants.Extensions.WAL;
import static com.example.mitch.pmanager.util.Constants.STRING_ENCODING;
import static com.example.mitch.pmanager.util.GsonUtil.gson;
import static com.example.mitch.pmanager.util.HashUtil.SHA512;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldChars;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.activities.LoginActivity;
import com.example.mitch.pmanager.deprecated.AES;
import com.example.mitch.pmanager.database.database.FileDatabase;
import com.example.mitch.pmanager.database.database.FolderDatabase;
import com.example.mitch.pmanager.database.entity.FileEntity;
import com.example.mitch.pmanager.database.entity.FolderEntity;
import com.example.mitch.pmanager.database.entity.MetadataEntity;
import com.example.mitch.pmanager.dialogs.CustomDialog;
import com.example.mitch.pmanager.models.EncryptedArchive;
import com.example.mitch.pmanager.models.EncryptedValue;
import com.example.mitch.pmanager.models.FileKey;
import com.example.mitch.pmanager.models.Folder;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.PasswordEntry;
import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.PasswordBank;
import com.example.mitch.pmanager.objects.storage.UserEntry;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.security.auth.DestroyFailedException;

/**
 * FilesUtil utility class for upgrading old file versions, importing and exporting files
 *
 * @author mitch
 */
public class FilesUtil {
    /**
     * Util class, private constructor
     */
    private FilesUtil() {
    }

    public static int getFolderCount(Context context, FileEntity entity) {
        return FolderDatabase.singleton(context, entity.getFilename()).folderDAO().getFolders().size();
    }

    public static List<Folder> getFolders(List<FolderEntity> entities, FileKey key) throws Exception {
        List<Folder> folders = new ArrayList<>(entities.size());
        for (FolderEntity entity : entities) {
            folders.add(getFolder(entity, key));
        }
        return folders;
    }

    public static Folder getFolder(FolderEntity entity, FileKey key) throws Exception {
        EncryptedValue value = new EncryptedValue(entity.getEncryptedJson(), entity.getIv());
        byte[] plaintext = key.decrypt(value, entity.getAssociatedData());

        Folder folder = gson().fromJson(new String(plaintext, STRING_ENCODING), Folder.class);
        folder.setEntity(entity);
        return folder;
    }

    public static void updateOrInsertFolder(Folder folder, @Nullable FolderEntity entity, FolderDatabase database, FileKey key) {
        long time = System.nanoTime();

        EncryptedValue folderKey = key.generateFolderKey();

        long id;
        if (entity == null) {
            entity = new FolderEntity(time, folderKey);
            id = database.folderDAO().insert(entity);
            entity.setId(id);
        } else {
            entity.setLastAccessed(time);
            entity.setKey(folderKey);
            id = entity.getId();
        }
        if (id == -1) {
            throw new RuntimeException("Error inserting/updating folderEntity");
            // TODO: maybe not the right error type?
        }

        byte[] labelHash = SHA512(folder.getLabel());
        entity.setLabel(labelHash);

        byte[] associatedData = entity.getAssociatedData();

        byte[] plaintext = gson().toJson(folder).getBytes(STRING_ENCODING);
        EncryptedValue encrypted = key.encryptWithFolderKey(folderKey, plaintext, associatedData);

        entity.setIv(encrypted.getIv());
        entity.setEncryptedJson(encrypted.getCiphertext());
        database.folderDAO().update(entity);
    }

    public static MetadataEntity updateOrInsertMetadata(@Nullable MetadataEntity metadata, FolderDatabase folderDB, long folderCount, FileKey key) {
        long time = System.nanoTime();

        long id;
        if (metadata == null) {
            metadata = new MetadataEntity(time, folderCount, key);
            id = folderDB.metadataDAO().insert(metadata);
            metadata.setId(id);
        } else {
            metadata.update(time, folderCount, key);
            id = metadata.getId();
            folderDB.metadataDAO().update(metadata);
        }
        if (id == -1) {
            throw new RuntimeException("Error inserting/updating folderEntity");
            // TODO: maybe not the right error type?
        }
        return metadata;
    }

    public static void checkpoint(Context context) {
        SupportSQLiteDatabase db = FileDatabase.singleton(context).getOpenHelper().getWritableDatabase();
        db.query("PRAGMA wal_checkpoint(FULL);");
        db.query("PRAGMA wal_checkpoint(TRUNCATE);");
    }

    public static final String TAR_DB = "DB";
    public static final String TAR_WAL = "WAL";
    public static final String TAR_SHM = "SHM";
    public static final String TAR_SALT = "SALT";

    public static void exportTar(EncryptedValue db, EncryptedValue wal, EncryptedValue shm, byte[] salt, OutputStream out) {
        try (
                BufferedOutputStream bos = new BufferedOutputStream(out);
                GZIPOutputStream gzip = new GZIPOutputStream(bos);
                TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)
        ) {
            writeTar(tar, TAR_DB, db);
            writeTar(tar, TAR_WAL, wal);
            writeTar(tar, TAR_SHM, shm);

            TarArchiveEntry entry = new TarArchiveEntry(TAR_SALT);
            entry.setSize(SALT_LENGTH);
            tar.putArchiveEntry(entry);
            tar.write(salt);
            tar.closeArchiveEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeTar(TarArchiveOutputStream tar, String name, EncryptedValue data) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        byte[] iv = data.getIv();
        byte[] ciphertext = data.getCiphertext();
        entry.setSize(iv.length + ciphertext.length);

        tar.putArchiveEntry(entry);
        tar.write(iv);
        tar.write(ciphertext);
        tar.closeArchiveEntry();
    }

    public static EncryptedArchive importTar(InputStream in) {
        try (
                BufferedInputStream bis = new BufferedInputStream(in);
                GZIPInputStream gunzip = new GZIPInputStream(bis);
                TarArchiveInputStream tar = new TarArchiveInputStream(gunzip)
        ) {
            EncryptedValue db = null;
            EncryptedValue wal = null;
            EncryptedValue shm = null;
            byte[] salt = new byte[SALT_LENGTH];

            ArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (entry.getName().equals(TAR_SALT)) {
                    tar.read(salt);
                    continue;
                }
                byte[] iv = new byte[GCM_IV_LENGTH];
                byte[] ciphertext = new byte[(int) (entry.getSize() - GCM_IV_LENGTH)];
                tar.read(iv);
                tar.read(ciphertext);
                switch (entry.getName()) {
                    case TAR_DB: {
                        db = new EncryptedValue(iv, ciphertext);
                        break;
                    }
                    case TAR_WAL: {
                        wal = new EncryptedValue(iv, ciphertext);
                        break;
                    }
                    case TAR_SHM: {
                        shm = new EncryptedValue(iv, ciphertext);
                        break;
                    }
                }
            }
            return new EncryptedArchive(db, wal, shm, salt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ActivityResultCallback<Uri> exportCallback(LoginActivity activity) {
        return result -> {
            if (result == null) return;
            diskIO().execute(() -> {
                DocumentFile df = DocumentFile.fromSingleUri(activity, result);
                String chosenFilename = Objects.requireNonNull(df).getName();
                String ext = Objects.requireNonNull(chosenFilename).substring(chosenFilename.lastIndexOf('.'));
                if (!(ext.equals(V4))) {
                    uiThread().execute(() -> toast(activity.getString(R.string.accepted_ext), activity));
                    return;
                }
                byte[] ad = chosenFilename.getBytes(STRING_ENCODING);

                FileEntity src = activity.getExportSrc();
                File db = src.getFile(activity);
                File wal = new File(db.getPath() + WAL);
                File shm = new File(db.getPath() + SHM);
                FileKey key = activity.getExportKey();
                try (OutputStream out = activity.getContentResolver().openOutputStream(result)) {
                    if (out == null) throw new RuntimeException("output stream null");
                    checkpoint(activity);
                    exportTar(
                            key.encrypt(Files.readAllBytes(db.toPath()), ad),
                            key.encrypt(Files.readAllBytes(wal.toPath()), ad),
                            key.encrypt(Files.readAllBytes(shm.toPath()), ad),
                            src.getMetadata().getSalt(),
                            out
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                activity.clearExport();
            });
        };
    }

    public static ActivityResultCallback<Uri> importCallback(LoginActivity activity) {
        return result -> {
            // TODO: Test this shit :(
            if (result == null) return;

            DocumentFile df = DocumentFile.fromSingleUri(activity, result);
            String filename = Objects.requireNonNull(Objects.requireNonNull(df).getName());
            int splitIndex = filename.lastIndexOf('.');
            String displayName = filename.substring(0, splitIndex);
            String ext = filename.substring(splitIndex);

            CustomDialog dialog = new CustomDialog(
                    R.layout.dialog_password_only,
                    activity.getString(R.string.open_file),
                    activity.getString(R.string.open), activity.getString(R.string.cancel),
                    (dialogInterface, i, dialogView) -> {
                        char[] password = getFieldChars(R.id.password, dialogView);

                        diskIO().execute(() -> {
                            HashMap<String, Folder> folders = new HashMap<>();

                            FileDatabase fileDB = FileDatabase.singleton(activity);
                            FileEntity fileEntity = new FileEntity(displayName);
                            long fileID = fileDB.fileDAO().insert(fileEntity);
                            if (fileID == -1) return;
                            fileEntity.setId(fileID);
                            fileEntity.setDuplicateNumber(activity.getDuplicateFileCount(fileEntity));
                            fileEntity.setSize(getFolderCount(activity, fileEntity)); // TODO: probably isn't working here...
                            fileDB.fileDAO().update(fileEntity);
                            FolderDatabase folderDB = FolderDatabase.singleton(activity, fileEntity.getFilename());

                            FileKey key;
                            char[] pwclone;
                            switch (ext) {
                                case V2:
                                    upgradeV2(activity, result, folders, fileEntity, fileDB, filename, password);
                                    key = new FileKey(password, generateSalt());
                                    fileEntity.setMetadata(updateOrInsertMetadata(null, folderDB, folders.size(), key));
                                    finishUpgrade(
                                            activity, fileEntity, folders, folderDB, key
                                    );
                                    break;
                                case V3:
                                    pwclone = Arrays.copyOf(password, password.length);
                                    upgradeV3(activity, result, folders, fileEntity, fileDB, filename, pwclone);
                                    key = new FileKey(password, generateSalt());
                                    fileEntity.setMetadata(updateOrInsertMetadata(null, folderDB, folders.size(), key));
                                    finishUpgrade(
                                            activity, fileEntity, folders, folderDB, key
                                    );
                                    break;
                                default: {
                                    pwclone = Arrays.copyOf(password, password.length);
                                    File db = fileEntity.getFile(activity);
                                    File wal = new File(db.getPath() + WAL);
                                    File shm = new File(db.getPath() + SHM);
                                    byte[] ad = filename.getBytes(STRING_ENCODING);
                                    byte[] data;
                                    try (InputStream in = activity.getContentResolver().openInputStream(result)) {
                                        if (in == null) throw new IOException("in is null");
                                        EncryptedArchive archive = importTar(in);
                                        key = new FileKey(pwclone, archive.getSalt());
                                        data = key.decrypt(archive.getDb(), ad);
                                        IOUtils.copy(new ByteArrayInputStream(data), Files.newOutputStream(db.toPath()));
                                        data = key.decrypt(archive.getWal(), ad);
                                        IOUtils.copy(new ByteArrayInputStream(data), Files.newOutputStream(wal.toPath()));
                                        data = key.decrypt(archive.getShm(), ad);
                                        IOUtils.copy(new ByteArrayInputStream(data), Files.newOutputStream(shm.toPath()));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    } catch (Exception e) {
                                        uiThread().execute(() -> toast(R.string.wrong_password, activity));
                                        fileDB.fileDAO().delete(fileEntity);
                                        return;
                                    }
                                    checkpoint(activity);

                                    MetadataEntity meta = folderDB.metadataDAO().getMeta().get(0);
                                    meta.update(System.nanoTime(), folderDB.folderDAO().getFolders().size(), new FileKey(password, meta.getSalt()));
                                    folderDB.metadataDAO().update(meta);
                                    fileEntity.setMetadata(meta);

                                    updateFileList(activity, fileEntity, folders);
                                }
                            }
                        });
                    }, (dialogInterface, i, dialogView) -> dialogInterface.cancel()
            );
            dialog.show(activity.getSupportFragmentManager());
        };
    }

    public static void finishUpgrade(LoginActivity activity, FileEntity fileEntity, HashMap<String, Folder> folders, FolderDatabase folderDB, FileKey key) {
        writeFolders(activity, folders, folderDB, key);
        updateFileList(activity, fileEntity, folders);
    }

    public static void updateFileList(LoginActivity activity, FileEntity fileEntity, HashMap<String, Folder> folders) {
        fileEntity.setSize(folders.size());
        uiThread().execute(() -> activity.addFile(fileEntity));
    }

    /**
     * Takes a given folders hashmap and writes it to the prospective database
     *
     * @param folders  folders to write
     * @param folderDB database to write folder to
     * @param key FileKey to encrypt with
     */
    private static void writeFolders(LoginActivity activity, HashMap<String, Folder> folders, FolderDatabase folderDB, FileKey key) {
        int i = 0;
        activity.startImportBar(folders.size());
        for (String label : folders.keySet()) {
            Log.i("Writing", "File #" + ++i + " / " + folders.size());
            activity.incrementImportBar();
            updateOrInsertFolder(folders.get(label), null, folderDB, key);
        }
        activity.endImportBar();
    }

    /**
     * Upgrades the file given by result to version 4 from version 2
     * @noinspection deprecation Used for backwards compatibility
     * @param context Context for the activity
     * @param importUri URI for loading from import
     * @param folders Folders hashmap for parsing to
     * @param fileEntity Entity for the file created in fileDB, deletes in case of failure
     * @param fileDB File database for storing the new file
     * @param filename Filename of the imported file
     * @param password Password of the imported file
     */
    private static void upgradeV2(
            Context context,
            Uri importUri,
            HashMap<String, Folder> folders,
            FileEntity fileEntity, FileDatabase fileDB,
            String filename, char[] password
    ) {
        String data;
        try (InputStream in = context.getContentResolver().openInputStream(importUri)) {
            data = AES.parse(in, filename, new String(password));
        } catch (Exception e) {
            uiThread().execute(() -> toast(R.string.error, context));
            fileDB.fileDAO().delete(fileEntity);
            return;
        }

        AES.modernize(data, folders);
    }

    /**
     * Upgrades a version 3 file to version 4
     * @param context Activity context
     * @param result URI from user choice of input file
     * @param folders V4 data structure
     * @param fileEntity File entity from fileDB, deleted if conversion fails
     * @param fileDB File database for handling stored files in the database
     * @param filename Filename for the input file. Used as associated data
     * @param password Encrypted password for the file.
     */
    private static void upgradeV3(
            Context context,
            Uri result,
            HashMap<String, Folder> folders,
            FileEntity fileEntity, FileDatabase fileDB,
            String filename, char[] password
    ) {
        byte[] ad = filename.getBytes(STRING_ENCODING);

        V3EncryptedFile file;
        try (InputStream in = context.getContentResolver().openInputStream(result)) {
            if (in == null) throw new IOException("in is null");
            file = importV3File(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] data;
        try {
            FileKey key = new FileKey(password, file.getSalt());
            data = key.decrypt(file.getEncrypted(), ad);
        } catch (DestroyFailedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            uiThread().execute(() -> toast(R.string.wrong_password, context));
            fileDB.fileDAO().delete(fileEntity);
            return;
        }

        PasswordBank bank = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object read = ois.readObject();
            if (read.getClass() == PMFile.class) {
                bank = PMFileToBank((PMFile) read);
            } else if (read.getClass() == PasswordBank.class) {
                bank = (PasswordBank) read;
            }
        } catch (Exception e) {
            uiThread().execute(() -> toast(R.string.error, context));
            fileDB.fileDAO().delete(fileEntity);
            return;
        }
        if (bank == null) {
            uiThread().execute(() -> toast(R.string.error, context));
            fileDB.fileDAO().delete(fileEntity);
            return;
        }
        for (DomainEntry domain : bank.getEntries()) {
            folders.put(domain.getDomain(), new Folder(domain));
        }
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

    public static V3EncryptedFile importV3File(InputStream in) {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[GCM_IV_LENGTH_OLD];
        byte[] ciphertext;
        try {
            in.read(salt);
            in.read(iv);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int i;
            while ((i = in.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, i);
            }
            ciphertext = baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new V3EncryptedFile(iv, salt, ciphertext);
    }

    public static class V3EncryptedFile {
        private final EncryptedValue encrypted;
        private final byte[] salt;

        public V3EncryptedFile(byte[] iv, byte[] salt, byte[] ciphertext) {
            this.encrypted = new EncryptedValue(ciphertext, iv);
            this.salt = salt;
        }

        public EncryptedValue getEncrypted() {
            return encrypted;
        }

        public byte[] getSalt() {
            return salt;
        }
    }
}
