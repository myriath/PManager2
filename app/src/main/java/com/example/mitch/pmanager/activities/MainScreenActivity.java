package com.example.mitch.pmanager.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.objects.PasswordEntry;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import javax.crypto.NoSuchPaddingException;

public class MainScreenActivity extends AppCompatActivity {

    File file;
    String filename;
    String password;
    ArrayList<PasswordEntry> fileData;
    private static final String STATE_FILEDATA = "filedata";
    private static final String STATE_FILENAME = "filename";
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_FILE = "file";

    private boolean ascendingIndex = true;
    private boolean ascendingDomain = true;
    private boolean ascendingUsername = true;
    private boolean ascendingPassword = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);
        Bundle bd = getIntent().getExtras();
        if (bd != null) {
            file = (File) bd.get(STATE_FILE);
            filename = (String) bd.get(STATE_FILENAME);
            password = (String) bd.get(STATE_PASSWORD);
            if (savedInstanceState == null) {
                fileData = (ArrayList<PasswordEntry>) bd.get(STATE_FILEDATA);
            } else {
                fileData = (ArrayList<PasswordEntry>) savedInstanceState.get(STATE_FILEDATA);
            }
        }
        assert fileData != null;
        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
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
        super.onUserLeaveHint();
        setResult(MainActivity.EXIT);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_FILEDATA, fileData);
    }

    private void resetFlags() {
        ascendingIndex = true;
        ascendingDomain = true;
        ascendingUsername = true;
        ascendingPassword = true;
    }

    public void sortIndex() {
        fileData.sort(new Comparator<PasswordEntry>() {
            @Override
            public int compare(PasswordEntry entry0, PasswordEntry entry1) {
                if (ascendingIndex) {
                    return Integer.compare(entry0.index, entry1.index);
                } else {
                    return -Integer.compare(entry0.index, entry1.index);
                }
            }
        });

        if (ascendingIndex) {
            resetFlags();
            ascendingIndex = false;
        } else {
            resetFlags();
        }

        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
        }
    }

    public void sortDomain() {
        fileData.sort(new Comparator<PasswordEntry>() {
            @Override
            public int compare(PasswordEntry entry0, PasswordEntry entry1) {
                if (ascendingDomain) {
                    return entry0.domain.toLowerCase().compareTo(entry1.domain.toLowerCase());
                } else {
                    return -entry0.domain.toLowerCase().compareTo(entry1.domain.toLowerCase());
                }
            }
        });

        if (ascendingDomain) {
            resetFlags();
            ascendingDomain = false;
        } else {
            resetFlags();
        }

        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
        }
    }

    public void sortUsername() {
        fileData.sort(new Comparator<PasswordEntry>() {
            @Override
            public int compare(PasswordEntry entry0, PasswordEntry entry1) {
                if (ascendingUsername) {
                    return entry0.username.toLowerCase().compareTo(entry1.username.toLowerCase());
                } else {
                    return -entry0.username.toLowerCase().compareTo(entry1.username.toLowerCase());
                }
            }
        });

        if (ascendingUsername) {
            resetFlags();
            ascendingUsername = false;
        } else {
            resetFlags();
        }

        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
        }
    }

    public void sortPassword() {
        fileData.sort(new Comparator<PasswordEntry>() {
            @Override
            public int compare(PasswordEntry entry0, PasswordEntry entry1) {
                if (ascendingPassword) {
                    return entry0.password.toLowerCase().compareTo(entry1.password.toLowerCase());
                } else {
                    return -entry0.password.toLowerCase().compareTo(entry1.password.toLowerCase());
                }
            }
        });

        if (ascendingPassword) {
            resetFlags();
            ascendingPassword = false;
        } else {
            resetFlags();
        }

        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        for (PasswordEntry entry : fileData) {
            createTable(tl, entry);
        }
    }

    public void addButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Entry");
        @SuppressLint("InflateParams")
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_add, null);
        final Context self = this;
        builder.setView(dialogLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText dt = dialogLayout.findViewById(R.id.dialog_domain);
                EditText ut = dialogLayout.findViewById(R.id.dialog_username);
                EditText pt = dialogLayout.findViewById(R.id.dialog_password);
                String d = dt.getText().toString();
                String u = ut.getText().toString();
                String p = pt.getText().toString();
                PasswordEntry e = new PasswordEntry(d, u, p, fileData.size() + 1);
                fileData.add(e);
                TableLayout tl = findViewById(R.id.tableLayout);
                clearTable();
                for (PasswordEntry entry : fileData) {
                    createTable(tl, entry);
                }

                MainActivity.toast("Added", self);
                resetFlags();
                sortIndex();
                save();
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.toast("Cancelled", self);
            }
        });

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
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText it = dialogLayout.findViewById(R.id.dialog_index);
                int id = Integer.parseInt(it.getText().toString());
                ArrayList<PasswordEntry> temp = new ArrayList<>();
                TableLayout tl = findViewById(R.id.tableLayout);
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
                resetFlags();
                sortIndex();
                save();
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.toast("Cancelled", self);
            }
        });

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
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText ft = dialogLayout.findViewById(R.id.dialog_filter);
                String filter = ft.getText().toString().toLowerCase();
                TableLayout tl = findViewById(R.id.tableLayout);
                clearTable();

                RadioButton domainButton = dialogLayout.findViewById(R.id.dialog_radio_domain);
                RadioButton usernameButton = dialogLayout.findViewById(R.id.dialog_radio_username);
                RadioButton passwordButton = dialogLayout.findViewById(R.id.dialog_radio_password);

                if (domainButton.isChecked()) {
                    for (PasswordEntry entry : fileData) {
                        if (entry.domain.toLowerCase().equals(filter)) {
                            createTable(tl, entry);
                        }
                    }
                } else if (usernameButton.isChecked()) {
                    for (PasswordEntry entry : fileData) {
                        if (entry.username.toLowerCase().equals(filter)) {
                            createTable(tl, entry);
                        }
                    }
                } else if (passwordButton.isChecked()) {
                    for (PasswordEntry entry : fileData) {
                        if (entry.password.toLowerCase().equals(filter)) {
                            createTable(tl, entry);
                        }
                    }
                }

                MainActivity.toast("Filtered", self);
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.toast("Cancelled", self);
            }
        });

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
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText ct = dialogLayout.findViewById(R.id.dialog_copy_index);

                RadioButton copyUsername = dialogLayout.findViewById(R.id.username);
                RadioButton copyPassword = dialogLayout.findViewById(R.id.password);

                if (copyUsername.isChecked()) {
                    copy(ct, self, 0);
                } else if (copyPassword.isChecked()) {
                    copy(ct, self, 1);
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.toast("Cancelled", self);
            }
        });

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
                    clip = ClipData.newPlainText("username", entry.username);
                    MainActivity.toast("Copied Username #" + copy, self);
                } else {
                    clip = ClipData.newPlainText("password", entry.password);
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

    public void save() {
        ArrayList<String> dat = new ArrayList<>();
        dat.add(filename);
        for (PasswordEntry entry : fileData) {
            dat.add(entry.domain);
            dat.add(entry.username);
            dat.add(entry.password);
        }
        StringBuilder sb = new StringBuilder();
        for (String str : dat) {
            sb.append(str);
            sb.append('\0');
        }
        try {
            AES f = new AES(AES.pad(password));
            f.encryptString(sb.toString(), file);
            if (!f.decrypt(file).split(System.lineSeparator())[0].equals(filename)) {
                save();
            }
            Toast.makeText(this, "Saved",
                    Toast.LENGTH_LONG).show();
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            Toast.makeText(this, "Warning: File not Saved!",
                    Toast.LENGTH_LONG).show();
            e1.printStackTrace();
        }
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
        dm.setText(entry.domain);
        un.setText(entry.username);
        pw.setText(entry.password);
        TableRow tr = new TableRow(this);
        tr.addView(id);
        tr.addView(dm);
        tr.addView(un);
        tr.addView(pw);
        tl.addView(tr);
    }

    public void clearTable() {
        TableLayout tl = findViewById(R.id.tableLayout);
        TableRow row1 = findViewById(R.id.row1);
        TextView id = new TextView(this);
        id.setPadding(5, 5, 5, 5);
        id.setBackgroundColor(Color.LTGRAY);
        id.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortIndex();
            }
        });
        TextView dm = new TextView(this);
        dm.setBackgroundColor(Color.GRAY);
        dm.setPadding(5, 5, 5, 5);
        dm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortDomain();
            }
        });
        TextView un = new TextView(this);
        un.setBackgroundColor(Color.LTGRAY);
        un.setPadding(5, 5, 5, 5);
        un.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortUsername();
            }
        });
        TextView pw = new TextView(this);
        pw.setBackgroundColor(Color.GRAY);
        pw.setPadding(5, 5, 5, 5);
        pw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortPassword();
            }
        });
        id.setText(R.string.table_index);
        dm.setText(R.string.table_domain);
        un.setText(R.string.table_username);
        pw.setText(R.string.table_password);
        row1.removeAllViews();
        row1.addView(id);
        row1.addView(dm);
        row1.addView(un);
        row1.addView(pw);
        tl.removeAllViews();
        tl.addView(row1);
    }
}
