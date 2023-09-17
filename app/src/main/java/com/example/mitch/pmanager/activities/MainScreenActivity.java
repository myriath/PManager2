package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.Constants.IntentKeys.FILEDATA;
import static com.example.mitch.pmanager.Constants.IntentKeys.FILENAME;
import static com.example.mitch.pmanager.Constants.IntentKeys.PASSWORD;
import static com.example.mitch.pmanager.Util.getFieldChars;
import static com.example.mitch.pmanager.Util.getFieldString;
import static com.example.mitch.pmanager.Util.writeFile;
import static com.example.mitch.pmanager.activities.MainActivity.toast;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.PasswordEntry;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Activity for the main screen
 */
public class MainScreenActivity extends AppCompatActivity {
    /**
     * Filename used as associated data for encryption
     */
    byte[] filename;
    /**
     * Password for encryption
     */
    char[] password;
    /**
     * PMFile containing the file data
     */
    PMFile pmFile;
    /**
     * Reference to the file data array in pmFile
     */
    ArrayList<PasswordEntry> fileData;

    /**
     * Enum for sorting methods
     */
    private enum Sorts {
        NULL, INDEX, DOMAIN, USERNAME, PASSWORD
    }

    /**
     * Last-used sort method
     */
    private Sorts previousSort = Sorts.NULL;
    /**
     * Sort direction boolean
     */
    private boolean ascendingSort = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);
        Bundle bd = getIntent().getExtras();
        if (bd != null) {
            filename = bd.getByteArray(FILENAME.key);
            password = bd.getCharArray(PASSWORD.key);
            if (savedInstanceState == null) {
                pmFile = (PMFile) bd.getSerializable(FILEDATA.key);
            } else {
                pmFile = (PMFile) savedInstanceState.getSerializable(FILEDATA.key);
            }
        }
        if (pmFile != null) {
            fileData = pmFile.getPasswordEntries();
            rebuildTable();
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == MainActivity.EXIT) {
            setResult(MainActivity.EXIT);
            finish();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        setResult(MainActivity.EXIT);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(FILEDATA.key, pmFile);
    }

    /**
     * Rebuilds the table for the password display
     */
    private void rebuildTable() {
        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
        }
    }

    /**
     * Resets the sorting methods
     */
    public void resetSorting() {
        previousSort = Sorts.NULL;
        ascendingSort = true;
    }

    /**
     * Sorts the table with the given method
     * @param sortFunction Sort method to use
     */
    private void sort(Sorts sortFunction) {
        if (previousSort == sortFunction) {
            ascendingSort = !ascendingSort;
        } else {
            ascendingSort = true;
            previousSort = sortFunction;
        }

        fileData.sort((entry0, entry1) -> {
            Integer temp = null;
            char[] c0;
            char[] c1;
            switch (sortFunction) {
                case INDEX: {
                    temp = Integer.compare(entry0.index, entry1.index);
                    c0 = new char[0];
                    c1 = new char[0];
                    break;
                }
                case DOMAIN: {
                    c0 = entry0.domain;
                    c1 = entry1.domain;
                    break;
                }
                case USERNAME: {
                    c0 = entry0.username;
                    c1 = entry1.username;
                    break;
                }
                case PASSWORD:
                default: {
                    c0 = entry0.password;
                    c1 = entry1.password;
                    break;
                }
            }
            String s0 = String.valueOf(c0).toLowerCase();
            String s1 = String.valueOf(c1).toLowerCase();
            int res = temp == null ? s0.compareTo(s1) : temp;
            return ascendingSort ? res : -res;
        });

        rebuildTable();
    }

    /**
     * Button to add a new password entry
     * @param view View for onClick()
     */
    public void addButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_entry);
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_add, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            char[] domain = getFieldChars(R.id.dialog_domain, dialogLayout);
            char[] username = getFieldChars(R.id.dialog_username, dialogLayout);
            char[] password = getFieldChars(R.id.dialog_password, dialogLayout);
            PasswordEntry e = new PasswordEntry(domain, username, password, fileData.size() + 1);
            fileData.add(e);
            TableLayout tl = findViewById(R.id.tableLayout);

            resetSorting();

            clearTable();
            for (PasswordEntry entry : fileData) {
                createTable(tl, entry);
            }

            toast(R.string.added, self);
            writeFile(pmFile, pmFile.getFile(), filename, this.password);
        });

        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> toast(R.string.cancelled, self));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Deletes an entry
     * @param view view for onClick()
     */
    public void deleteButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_entry);
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_delete_entry, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            int id = Integer.parseInt(getFieldString(R.id.dialog_index, dialogLayout));
            ArrayList<PasswordEntry> temp = new ArrayList<>();
            TableLayout tl = findViewById(R.id.tableLayout);

            resetSorting();

            clearTable();
            for (PasswordEntry entry : fileData) {
                if (entry.index != id) {
                    if (entry.index > id) {
                        entry.index--;
                    }
                    createTable(tl, entry);
                    temp.add(entry);
                }
            }
            fileData = temp;

            toast(R.string.deleted, self);
            writeFile(pmFile, pmFile.getFile(), filename, password);
        });

        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> toast(R.string.cancelled, self));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Filters the table
     * @param view view for onClick()
     */
    public void filterButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.filter);
        builder.setMessage(R.string.filter_msg);
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            String filter = getFieldString(R.id.dialog_filter, dialogLayout).toLowerCase();
            TableLayout tl = findViewById(R.id.tableLayout);
            clearTable();

            RadioButton domainButton = dialogLayout.findViewById(R.id.dialog_radio_domain);
            RadioButton usernameButton = dialogLayout.findViewById(R.id.dialog_radio_username);
            RadioButton passwordButton = dialogLayout.findViewById(R.id.dialog_radio_password);

            if (domainButton.isChecked()) {
                for (PasswordEntry entry : fileData) {
                    if (String.valueOf(entry.domain).toLowerCase().contains(filter)) {
                        createTable(tl, entry);
                    }
                }
            } else if (usernameButton.isChecked()) {
                for (PasswordEntry entry : fileData) {
                    if (String.valueOf(entry.username).toLowerCase().contains(filter)) {
                        createTable(tl, entry);
                    }
                }
            } else if (passwordButton.isChecked()) {
                for (PasswordEntry entry : fileData) {
                    if (String.valueOf(entry.password).toLowerCase().contains(filter)) {
                        createTable(tl, entry);
                    }
                }
            }

            toast(R.string.filtered, self);
        });

        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> toast(R.string.cancelled, self));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Copies a given value
     * @param view view for onClick()
     */
    public void copyButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.copy);
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_copy, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            EditText ct = dialogLayout.findViewById(R.id.dialog_copy_index);

            RadioButton copyUsername = dialogLayout.findViewById(R.id.username);
            RadioButton copyPassword = dialogLayout.findViewById(R.id.password);

            if (copyUsername.isChecked()) {
                copy(ct, self, 0);
            } else if (copyPassword.isChecked()) {
                copy(ct, self, 1);
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> toast(R.string.cancelled, self));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Copies the desired data to the clipboard
     * @param index Index of the entry to copy
     * @param self Activity reference
     * @param function Function for deciding the copy value
     */
    private void copy(EditText index, Context self, int function) {
        int copy = Integer.parseInt(index.getText().toString());
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        for (PasswordEntry entry : fileData) {
            if (entry.index == copy) {
                ClipData clip;
                if (function == 0) {
                    clip = ClipData.newPlainText("username", CharBuffer.wrap(entry.username));
                    toast(getString(R.string.copied_username, copy), self);
                } else {
                    clip = ClipData.newPlainText("password", CharBuffer.wrap(entry.password));
                    toast(getString(R.string.copied_password, copy), self);
                }

                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                return;
            }
        }
    }

    /**
     * Resets the filters
     * @param view view for onClick()
     */
    public void resetFilterButton(View view) {
        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
        }
        toast(R.string.filter_reset, this);
    }

    /**
     * Creates the table from the given entry
     * @param tl Tablelayout to add rows to
     * @param entry entry to get data from
     */
    private void createTable(TableLayout tl, PasswordEntry entry) {
        TextView id = new TextView(this);
        id.setPadding(5, 5, 5, 5);
        id.setBackgroundColor(Color.LTGRAY);
        TextView dm = new TextView(this);
        dm.setBackgroundColor(Color.GRAY);
        dm.setPadding(5, 5, 5, 5);
        TextView un = new TextView(this);
        un.setBackgroundColor(Color.LTGRAY);
        un.setPadding(5, 5, 5, 5);
        TextView pw = new TextView(this);
        pw.setBackgroundColor(Color.GRAY);
        pw.setPadding(5, 5, 5, 5);
        id.setText(String.format(Locale.getDefault(), "%d", entry.index));
        dm.setText(entry.domain, 0, entry.domain.length);
        dm.setTextColor(Color.WHITE);
        un.setText(entry.username, 0, entry.username.length);
        pw.setText(entry.password, 0, entry.password.length);
        pw.setTextColor(Color.WHITE);
        TableRow tr = new TableRow(this);
        tr.addView(id);
        tr.addView(dm);
        tr.addView(un);
        tr.addView(pw);
        tl.addView(tr);
    }

    /**
     * Clears the table
     */
    private void clearTable() {
        TableLayout tl = findViewById(R.id.tableLayout);
        TableRow row1 = findViewById(R.id.row1);
        TextView id = new TextView(this);
        id.setPadding(5, 5, 5, 5);
        id.setBackgroundColor(Color.LTGRAY);
        id.setOnClickListener(view -> sort(Sorts.INDEX));
        TextView dm = new TextView(this);
        dm.setBackgroundColor(Color.GRAY);
        dm.setPadding(5, 5, 5, 5);
        dm.setOnClickListener(view -> sort(Sorts.DOMAIN));
        TextView un = new TextView(this);
        un.setBackgroundColor(Color.LTGRAY);
        un.setPadding(5, 5, 5, 5);
        un.setOnClickListener(view -> sort(Sorts.USERNAME));
        TextView pw = new TextView(this);
        pw.setBackgroundColor(Color.GRAY);
        pw.setPadding(5, 5, 5, 5);
        pw.setOnClickListener(view -> sort(Sorts.PASSWORD));
        id.setText(R.string.table_index);
        dm.setText(R.string.table_domain);
        dm.setTextColor(Color.WHITE);
        un.setText(R.string.table_username);
        pw.setText(R.string.table_password);
        pw.setTextColor(Color.WHITE);
        row1.removeAllViews();
        row1.addView(id);
        row1.addView(dm);
        row1.addView(un);
        row1.addView(pw);
        tl.removeAllViews();
        tl.addView(row1);
    }
}
