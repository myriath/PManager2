package com.example.mitch.pmanager.models;

import static com.example.mitch.pmanager.util.FilesUtil.getFolders;
import static com.example.mitch.pmanager.util.FilesUtil.updateOrInsertFolder;

import com.example.mitch.pmanager.database.database.FolderDatabase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Works as workable storage for folders
 * Uses aan arraylist for ordered storage and hashmap for unordered storage in combination
 *
 * @author mitch
 */
public class FolderStore {
    /**
     * Hash map for linking labels to folder objects
     */
    private final HashMap<String, Folder> unorderedFolders;
    /**
     * ArrayList for storing the order of the folders
     */
    private final ArrayList<Folder> orderedFolders;
    /**
     * Comparator for comparing two different folders. For sorting
     */
    private static final Comparator<Folder> FOLDER_COMPARATOR = Comparator.comparing(Folder::getLabel);

    private final FolderDatabase folderDatabase;
    private final FileKey key;

    /**
     * Constructor from an array list of ordered folders
     *
     * @param folderDatabase Database of folders to open
     * @param key            Key to decrypt folders with
     */
    public FolderStore(FolderDatabase folderDatabase, FileKey key) {
        this.folderDatabase = folderDatabase;
        this.key = key;

        try {
            orderedFolders = new ArrayList<>(getFolders(folderDatabase.folderDAO().getFolders(), key));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        orderedFolders.sort(FOLDER_COMPARATOR);
        unorderedFolders = new HashMap<>();
        for (Folder folder : this.orderedFolders) {
            this.unorderedFolders.put(folder.getLabel(), folder);
        }
    }

    /**
     * Gets a folder based on its label
     *
     * @param label Label to get
     * @return Folder with that label in this store
     */
    public Folder getFolder(String label) {
        return unorderedFolders.get(label);
    }

    /**
     * Gets a folder based on its position
     *
     * @param position position to get
     * @return Folder with that position in this store
     */
    public Folder getFolder(int position) {
        return orderedFolders.get(position);
    }

    /**
     * Inserts a new folder if its label doesn't already exist
     *
     * @param folder Folder to insert
     * @return index of the inserted folder. -1 if insert failed
     */
    public int insertFolder(Folder folder) {
        Folder existingFolder = unorderedFolders.get(folder.getLabel());
        if (existingFolder != null) return -1;
        unorderedFolders.put(folder.getLabel(), folder);
        orderedFolders.add(folder);
        orderedFolders.sort(FOLDER_COMPARATOR);

        updateOrInsertFolder(folder, folderDatabase, key);

        return orderedFolders.indexOf(folder);
    }

    /**
     * Inserts a new empty folder with a given label, if it doesn't already exist
     *
     * @param label Label to create new folder with
     * @return index of the created folder. -1 if insert failed
     */
    public int createFolder(String label) {
        return insertFolder(new Folder(label));
    }

    /**
     * Deletes a folder from this store
     *
     * @param folder Folder to delete
     * @return Deleted folder, if it exists
     */
    public Folder deleteFolder(Folder folder) {
        Folder existingFolder = unorderedFolders.get(folder.getLabel());
        if (existingFolder == null) return null;
        unorderedFolders.remove(folder.getLabel());
        orderedFolders.remove(existingFolder);

        folderDatabase.folderDAO().delete(existingFolder.getEntity());

        return existingFolder;
    }

    /**
     * Deletes a folder from this store
     *
     * @param position Position to delete
     * @return Deleted folder, if it exists
     */
    public Folder deleteFolder(int position) {
        Folder existingFolder = orderedFolders.get(position);
        if (existingFolder == null) return null;
        orderedFolders.remove(position);
        unorderedFolders.remove(existingFolder.getLabel());

        folderDatabase.folderDAO().delete(existingFolder.getEntity());

        return existingFolder;
    }

    /**
     * Updates the given folder, if it exists
     *
     * @param folder Folder to update in the database
     * @return True if the folder got updated
     */
    public boolean update(Folder folder) {
        Folder existingFolder = unorderedFolders.get(folder.getLabel());
        if (existingFolder == null) return false;
        updateOrInsertFolder(existingFolder, folderDatabase, key);
        return true;
    }

    public HashMap<String, Folder> getUnordered() {
        return unorderedFolders;
    }

    public ArrayList<Folder> getOrdered() {
        return orderedFolders;
    }
}
