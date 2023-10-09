package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.activities.LoginActivity.toast;
import static com.example.mitch.pmanager.util.ByteCharStringUtil.getFieldString;
import static com.example.mitch.pmanager.util.Constants.DP16;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILE;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILEDATA;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILENAME;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.PASSWORD;
import static com.example.mitch.pmanager.util.FileUtil.writeFile;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.adapters.DomainEntryAdapter;
import com.example.mitch.pmanager.objects.storage.PasswordBank;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

/**
 * Activity for the main screen
 */
public class FileOpenActivity extends AppCompatActivity {
    /**
     * File used to write to during saving
     */
    File file;
    /**
     * Filename used as associated data for encryption
     */
    byte[] filename;
    /**
     * Password for encryption
     */
    char[] password;
    /**
     * PasswordBank containing the file data
     */
    PasswordBank bank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_file_open);

        Bundle bd = getIntent().getExtras();
        if (bd != null) {
            file = (File) bd.getSerializable(FILE.key);
            filename = bd.getByteArray(FILENAME.key);
            password = bd.getCharArray(PASSWORD.key);
            if (savedInstanceState == null) {
                bank = (PasswordBank) bd.getSerializable(FILEDATA.key);
            } else {
                bank = (PasswordBank) savedInstanceState.getSerializable(FILEDATA.key);
            }
        }
        if (bank == null) finish();

        DomainEntryAdapter adapter = new DomainEntryAdapter(this, bank.getEntries(), (unused) -> save());
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
        });

        findViewById(R.id.newButton).setOnClickListener(view -> {
            final View dialogLayout = View.inflate(this, R.layout.dialog_create_domain, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                    .setView(dialogLayout)
                    .setTitle(R.string.create_domain)
                    .setPositiveButton(R.string.create, (dialogInterface, i) -> {
                        int beforeCreationSize = bank.getEntries().size() - 1;
                        bank.createDomain(getFieldString(R.id.domain, dialogLayout));
                        int afterCreationSize = bank.getEntries().size() - 1;
                        if (beforeCreationSize != afterCreationSize) {
                            adapter.notifyItemInserted(afterCreationSize);
                        }
                        findViewById(R.id.newButtonText).setVisibility(View.GONE);
                        findViewById(R.id.searchButtonText).setVisibility(View.GONE);
                        findViewById(R.id.emptyText).setVisibility(View.GONE);
                        dialogInterface.dismiss();
                        save();
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            builder.create();
            builder.show();
        });
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(FILEDATA.key, bank);
    }

    private void save() {
        if (!writeFile(bank, file, filename, password))
            toast(getString(R.string.error_failed_to_save), this);
    }

}
