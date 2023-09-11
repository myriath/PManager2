package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.Constants.STATE_FILE;
import static com.example.mitch.pmanager.Constants.STATE_FILEDATA;
import static com.example.mitch.pmanager.Constants.STATE_FILENAME;
import static com.example.mitch.pmanager.Constants.STATE_PASSWORD;
import static com.example.mitch.pmanager.Util.getFieldChars;
import static com.example.mitch.pmanager.Util.getFieldString;

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
import android.widget.Toast;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.PasswordEntry;

import java.io.File;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Locale;

public class MainScreenActivity extends AppCompatActivity {

    File file;
    byte[] filename;
    char[] password;
    PMFile pmFile;
    ArrayList<PasswordEntry> fileData;
    private static final int SORT_INDEX = 0;
    private static final int SORT_DOMAIN = 1;
    private static final int SORT_USERNAME = 2;
    private static final int SORT_PASSWORD = 3;

    private int previousSort = -1;
    private boolean ascendingSort = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);
        Bundle bd = getIntent().getExtras();
        if (bd != null) {
            file = (File) bd.get(STATE_FILE);
            filename = bd.getByteArray(STATE_FILENAME);
            password = bd.getCharArray(STATE_PASSWORD);
            if (savedInstanceState == null) {
                pmFile = (PMFile) bd.getSerializable(STATE_FILEDATA);
            } else {
                pmFile = (PMFile) savedInstanceState.getSerializable(STATE_FILEDATA);
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
        outState.putSerializable(STATE_FILEDATA, pmFile);
    }

    private void rebuildTable() {
        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
        }
    }

    public void resetSorting() {
        previousSort = -1;
        ascendingSort = true;
    }

    private void sort(int sortFunction) {
        if (previousSort == sortFunction) {
            ascendingSort = !ascendingSort;
        } else {
            ascendingSort = true;
        }

        previousSort = sortFunction;
        fileData.sort((entry0, entry1) -> {
            Integer temp = null;
            char[] c0;
            char[] c1;
            switch (sortFunction) {
                case SORT_INDEX: {
                    temp = Integer.compare(entry0.index, entry1.index);
                    c0 = new char[0];
                    c1 = new char[0];
                    break;
                }
                case SORT_DOMAIN: {
                    c0 = entry0.domain;
                    c1 = entry1.domain;
                    break;
                }
                case SORT_USERNAME: {
                    c0 = entry0.username;
                    c1 = entry1.username;
                    break;
                }
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

    public void addButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Entry");
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_add, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton("OK", (dialogInterface, i) -> {
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

            MainActivity.toast("Added", self);
            pmFile.writeFile(filename, this.password, file);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> MainActivity.toast("Cancelled", self));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void deleteButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Entry");
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_delete_entry, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton("OK", (dialogInterface, i) -> {
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

            MainActivity.toast("Deleted", self);
            pmFile.writeFile(filename, password, file);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> MainActivity.toast("Cancelled", self));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void filterButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter");
        builder.setMessage("Filter by domain, username, and password");
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton("OK", (dialogInterface, i) -> {
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

            MainActivity.toast("Filtered", self);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> MainActivity.toast("Cancelled", self));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void copyButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Copy");
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_copy, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton("Ok", (dialogInterface, i) -> {
            EditText ct = dialogLayout.findViewById(R.id.dialog_copy_index);

            RadioButton copyUsername = dialogLayout.findViewById(R.id.username);
            RadioButton copyPassword = dialogLayout.findViewById(R.id.password);

            if (copyUsername.isChecked()) {
                copy(ct, self, 0);
            } else if (copyPassword.isChecked()) {
                copy(ct, self, 1);
            }
        });

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> MainActivity.toast("Cancelled", self));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void copy(EditText index, Context self, int function) {
        int copy = Integer.parseInt(index.getText().toString());
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        for (PasswordEntry entry : fileData) {
            if (entry.index == copy) {
                ClipData clip;
                if (function == 0) {
                    clip = ClipData.newPlainText("username", CharBuffer.wrap(entry.username));
                    MainActivity.toast("Copied Username #" + copy, self);
                } else {
                    clip = ClipData.newPlainText("password", CharBuffer.wrap(entry.password));
                    MainActivity.toast("Copied Password #" + copy, self);
                }

                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                return;
            }
        }
    }

    public void resetFilterButton(View view) {
        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
        }
        Toast.makeText(this, "Filter Reset",
                Toast.LENGTH_LONG).show();
    }

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

    private void clearTable() {
        TableLayout tl = findViewById(R.id.tableLayout);
        TableRow row1 = findViewById(R.id.row1);
        TextView id = new TextView(this);
        id.setPadding(5, 5, 5, 5);
        id.setBackgroundColor(Color.LTGRAY);
        id.setOnClickListener(view -> sort(SORT_INDEX));
        TextView dm = new TextView(this);
        dm.setBackgroundColor(Color.GRAY);
        dm.setPadding(5, 5, 5, 5);
        dm.setOnClickListener(view -> sort(SORT_DOMAIN));
        TextView un = new TextView(this);
        un.setBackgroundColor(Color.LTGRAY);
        un.setPadding(5, 5, 5, 5);
        un.setOnClickListener(view -> sort(SORT_USERNAME));
        TextView pw = new TextView(this);
        pw.setBackgroundColor(Color.GRAY);
        pw.setPadding(5, 5, 5, 5);
        pw.setOnClickListener(view -> sort(SORT_PASSWORD));
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
