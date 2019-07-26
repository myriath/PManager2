package com.example.mitch.pmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

public class MainScreenActivity extends AppCompatActivity {

    public static final int REQUEST_ADD = 0;
    public static final int REQUEST_DELETE = 1;
    public static final int REQUEST_FILTER = 2;
    public static final int REQUEST_COPY = 3;
    File file;
    String filename;
    String password;
    ArrayList<PasswordEntry> fileData;
    private static final String STATE_FILEDATA = "filedata";
    private static final String STATE_FILENAME = "filename";
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_FILE = "file";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_FILEDATA, fileData);
    }

    public void clearTable() {
        TableLayout tl = findViewById(R.id.tableLayout);
        TableRow row1 = findViewById(R.id.row1);
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
        id.setText("Index");
        dm.setText("Domain");
        un.setText("Username");
        pw.setText("Password");
        row1.removeAllViews();
        row1.addView(id);
        row1.addView(dm);
        row1.addView(un);
        row1.addView(pw);
        tl.removeAllViews();
        tl.addView(row1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_ADD: {
                String d = data.getStringExtra("domain");
                String u = data.getStringExtra("username");
                String p = data.getStringExtra("password");
                PasswordEntry e = new PasswordEntry(d, u, p, fileData.size() + 1);
                fileData.add(e);
                TableLayout tl = findViewById(R.id.tableLayout);
                clearTable();
                for (PasswordEntry entry : fileData) {
                    createTable(tl, entry);
                }
                break;
            }
            case REQUEST_DELETE: {
                int id = Integer.parseInt(data.getStringExtra("id"));
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
                break;
            }
            case REQUEST_FILTER: {
                int operation = Integer.parseInt(data.getStringExtra("operation"));
                String filter = data.getStringExtra("filter");
                switch (operation) {
                    case 0: {
                        TableLayout tl = findViewById(R.id.tableLayout);
                        clearTable();
                        for (PasswordEntry entry : fileData) {
                            if (entry.domain.equals(filter)) {
                                createTable(tl, entry);
                            }
                        }
                        break;
                    }
                    case 1: {
                        TableLayout tl = findViewById(R.id.tableLayout);
                        clearTable();
                        for (PasswordEntry entry : fileData) {
                            if (entry.username.equals(filter)) {
                                createTable(tl, entry);
                            }
                        }
                        break;
                    }
                    case 2: {
                        TableLayout tl = findViewById(R.id.tableLayout);
                        clearTable();
                        for (PasswordEntry entry : fileData) {
                            if (entry.password.equals(filter)) {
                                createTable(tl, entry);
                            }
                        }
                        break;
                    }
                }
                break;
            }
            case REQUEST_COPY: {
                int operation = Integer.parseInt(data.getStringExtra("operation"));
                int copy = Integer.parseInt(data.getStringExtra("copy"));
                if (operation == 0) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    for (PasswordEntry entry : fileData) {
                        if (entry.index == copy) {
                            ClipData clip = ClipData.newPlainText("username", entry.username);
                            assert clipboard != null;
                            clipboard.setPrimaryClip(clip);
                        }
                    }
                } else if (operation == 1) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    for (PasswordEntry entry : fileData) {
                        if (entry.index == copy) {
                            ClipData clip = ClipData.newPlainText("password", entry.password);
                            assert clipboard != null;
                            clipboard.setPrimaryClip(clip);
                        }
                    }
                }
                break;
            }
        }
    }

    public void addButton(View view) {
        Intent intent = new Intent(this, AddActivity.class);
        startActivityForResult(intent, REQUEST_ADD);
    }

    public void deleteButton(View view) {
        Intent intent = new Intent(this, DeleteActivity.class);
        startActivityForResult(intent, REQUEST_DELETE);
    }

    public void filterButton(View view) {
        Intent intent = new Intent(this, FilterActivity.class);
        startActivityForResult(intent, REQUEST_FILTER);
    }

    public void copyButton(View view) {
        Intent intent = new Intent(this, CopyActivity.class);
        startActivityForResult(intent, REQUEST_COPY);
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

    public void saveButton(View view) {
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
        AES f;
        try {
            f = new AES(AES.pad(password));
            f.encryptString(sb.toString(), file);
            if (!f.decrypt(file).split(System.lineSeparator())[0].equals(filename)) {
                saveButton(view);
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
        id.setText(Integer.toString(entry.index));
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
}
