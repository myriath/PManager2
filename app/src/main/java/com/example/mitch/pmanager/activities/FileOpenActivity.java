package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.activities.LoginActivity.toast;
import static com.example.mitch.pmanager.util.AsyncUtil.diskIO;
import static com.example.mitch.pmanager.util.AsyncUtil.uiThread;
import static com.example.mitch.pmanager.util.Constants.BundleKeys.BUNDLE_FOLDER;
import static com.example.mitch.pmanager.util.Constants.DP16;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILE;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.KEY;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldString;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.adapters.FolderAdapter;
import com.example.mitch.pmanager.database.database.FolderDatabase;
import com.example.mitch.pmanager.database.entity.FileEntity;
import com.example.mitch.pmanager.dialogs.CustomDialog;
import com.example.mitch.pmanager.dialogs.EditFolderDialog;
import com.example.mitch.pmanager.models.Entry;
import com.example.mitch.pmanager.models.FileKey;
import com.example.mitch.pmanager.models.Folder;
import com.example.mitch.pmanager.models.FolderStore;

import java.util.ArrayList;

/**
 * Activity for the main screen
 */
public class FileOpenActivity extends AppCompatActivity {
    /**
     * Folder storage for this file
     */
    FolderStore folders;

    public static void startEditDialog(FileOpenActivity activity, int position, FolderAdapter adapter) {
        Folder folder = activity.folders.getFolder(position);
        EditFolderDialog dialog = new EditFolderDialog(folder, (bundle) -> {
            ArrayList<Entry> entries = bundle.getParcelableArrayList(BUNDLE_FOLDER, Entry.class);
            folder.setEntries(entries);
            activity.save(folder);
            adapter.notifyItemChanged(position);
        });
        dialog.show(activity.getSupportFragmentManager());
    }

    public void save(Folder folder) {
        diskIO().execute(() -> folders.update(folder));
    }

    /**
     * Goes back to login screen on changing apps
     */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        setResult(LoginActivity.EXIT);
        finish();
    }

    /**
     * Goes back to login screen on changing apps
     */
    @Override
    protected void onPause() {
        super.onPause();
        setResult(LoginActivity.EXIT);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_file_open);

        diskIO().execute(() -> {
            Bundle bd = getIntent().getExtras();
            if (bd != null) {
                FileEntity file = bd.getParcelable(FILE, FileEntity.class);
                if (file == null) throw new IllegalStateException("File is null");
                FileKey key = bd.getParcelable(KEY, FileKey.class);
                FolderDatabase folderDB = FolderDatabase.singleton(this, file.getFilename());
                folders = new FolderStore(folderDB, key);
            }
            if (folders == null) finish();

            uiThread().execute(() -> {
                FolderAdapter adapter = new FolderAdapter(this, folders.getOrdered(), (bundle) -> save(bundle.getParcelable(BUNDLE_FOLDER, Folder.class)));
                RecyclerView entriesList = findViewById(R.id.entriesList);
                ViewCompat.setOnApplyWindowInsetsListener(entriesList, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), insets.bottom + DP16 * 5);
                    return WindowInsetsCompat.CONSUMED;
                });
                entriesList.setItemAnimator(null); // TODO: Create animator
                entriesList.setLayoutManager(new LinearLayoutManager(this));
                entriesList.setAdapter(adapter);
                if (adapter.getItemCount() == 0) {
                    findViewById(R.id.newButtonText).setVisibility(View.VISIBLE);
                    findViewById(R.id.searchButtonText).setVisibility(View.VISIBLE);
                    findViewById(R.id.emptyText).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.newButtonText).setVisibility(View.GONE);
                    findViewById(R.id.searchButtonText).setVisibility(View.GONE);
                    findViewById(R.id.emptyText).setVisibility(View.GONE);
                }

                findViewById(R.id.searchButton).setOnClickListener(view -> {
                    // TODO: Search
                    // TODO: Search bar at top
                    //       Automatically updates with new text
                    //       Searches all (domain, username, password) at the same time
                });

                findViewById(R.id.newButton).setOnClickListener(view -> {
                    CustomDialog customDialog = new CustomDialog(
                            R.layout.dialog_create_domain,
                            getString(R.string.create_domain),
                            getString(R.string.create), getString(R.string.cancel),
                            (dialogInterface, i, dialogView) -> diskIO().execute(() -> {
                                int index = folders.createFolder(getFieldString(R.id.domain, dialogView));
                                if (index == -1) {
                                    dialogInterface.cancel();
                                    uiThread().execute(() -> toast(R.string.error_folder_exists, this));
                                    return;
                                }
                                uiThread().execute(() -> {
                                    adapter.notifyItemInserted(index);
                                    findViewById(R.id.newButtonText).setVisibility(View.GONE);
                                    findViewById(R.id.searchButtonText).setVisibility(View.GONE);
                                    findViewById(R.id.emptyText).setVisibility(View.GONE);
                                    dialogInterface.dismiss();
//                                    save(folders.getFolder(index));

                                    startEditDialog(this, index, adapter);
                                });
                            }), (dialogInterface, i, dialogView) -> dialogInterface.cancel()
                    );
                    customDialog.show(getSupportFragmentManager());
                });
            });
        });
    }

}
