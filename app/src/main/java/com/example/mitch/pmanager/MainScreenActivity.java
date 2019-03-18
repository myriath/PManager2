package com.example.mitch.pmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Array;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

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
        for (PasswordEntry entry : fileData) {
            TextView id = new TextView(this);
            TextView dm = new TextView(this);
            TextView un = new TextView(this);
            TextView pw = new TextView(this);
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

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_FILEDATA, fileData);
    }

    public void clearTable() {
        TableLayout tl = findViewById(R.id.tableLayout);
        TableRow tr = findViewById(R.id.row1);
        tl.removeAllViews();
        tl.addView(tr);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == 0) {
            String d = data.getStringExtra("domain");
            String u = data.getStringExtra("username");
            String p = data.getStringExtra("password");
            PasswordEntry e = new PasswordEntry(d, u, p, fileData.size() + 1);
            fileData.add(e);
            TableLayout tl = findViewById(R.id.tableLayout);
            clearTable();
            int i = 0;
            for (PasswordEntry entry : fileData) {
                i++;
                TableRow tr = new TableRow(this);
                tr.setId(i);
                TextView id = new TextView(this);
                id.setText(Integer.toString(entry.index));
                tr.addView(id);
                TextView dom = new TextView(this);
                dom.setText(entry.domain);
                tr.addView(dom);
                TextView un = new TextView(this);
                un.setText(entry.username);
                tr.addView(un);
                TextView pwd = new TextView(this);
                pwd.setText(entry.password);
                tr.addView(pwd);

                tl.addView(tr);
            }
        } else if (requestCode == 1) {
            int i = Integer.parseInt(data.getStringExtra("id"));
            ArrayList<PasswordEntry> temp = new ArrayList<>();
            TableLayout tl = findViewById(R.id.tableLayout);
            clearTable();
            int i2 = 0;
            for (PasswordEntry entry : fileData) {
                if (entry.index != i) {
                    if (entry.index > i) {
                        entry.index--;
                    }
                    i2++;
                    TableRow tr = new TableRow(this);
                    tr.setId(i2);
                    TextView id = new TextView(this);
                    id.setText(Integer.toString(entry.index));
                    tr.addView(id);
                    TextView dom = new TextView(this);
                    dom.setText(entry.domain);
                    tr.addView(dom);
                    TextView un = new TextView(this);
                    un.setText(entry.username);
                    tr.addView(un);
                    TextView pwd = new TextView(this);
                    pwd.setText(entry.password);
                    tr.addView(pwd);

                    tl.addView(tr);
                    temp.add(entry);
                }
            }
            fileData = temp;
        } else if (requestCode == 2) {
            int i = Integer.parseInt(data.getStringExtra("operation"));
            String filter = data.getStringExtra("filter");
            if (i == 0) {
                TableLayout tl = findViewById(R.id.tableLayout);
                clearTable();
                int i2 = 0;
                for (PasswordEntry entry : fileData) {
                    if (entry.domain.equals(filter)) {
                        i2++;
                        TableRow tr = new TableRow(this);
                        tr.setId(i2);
                        TextView id = new TextView(this);
                        id.setText(Integer.toString(entry.index));
                        tr.addView(id);
                        TextView dom = new TextView(this);
                        dom.setText(entry.domain);
                        tr.addView(dom);
                        TextView un = new TextView(this);
                        un.setText(entry.username);
                        tr.addView(un);
                        TextView pwd = new TextView(this);
                        pwd.setText(entry.password);
                        tr.addView(pwd);

                        tl.addView(tr);
                    }
                }
            } else if (i == 1) {
                TableLayout tl = findViewById(R.id.tableLayout);
                clearTable();
                int i2 = 0;
                for (PasswordEntry entry : fileData) {
                    if (entry.username.equals(filter)) {
                        i2++;
                        TableRow tr = new TableRow(this);
                        tr.setId(i2);
                        TextView id = new TextView(this);
                        id.setText(Integer.toString(entry.index));
                        tr.addView(id);
                        TextView dom = new TextView(this);
                        dom.setText(entry.domain);
                        tr.addView(dom);
                        TextView un = new TextView(this);
                        un.setText(entry.username);
                        tr.addView(un);
                        TextView pwd = new TextView(this);
                        pwd.setText(entry.password);
                        tr.addView(pwd);

                        tl.addView(tr);
                    }
                }
            } else if (i == 2) {
                TableLayout tl = findViewById(R.id.tableLayout);
                clearTable();
                int i2 = 0;
                for (PasswordEntry entry : fileData) {
                    if (entry.password.equals(filter)) {
                        i2++;
                        TableRow tr = new TableRow(this);
                        tr.setId(i2);
                        TextView id = new TextView(this);
                        id.setText(Integer.toString(entry.index));
                        tr.addView(id);
                        TextView dom = new TextView(this);
                        dom.setText(entry.domain);
                        tr.addView(dom);
                        TextView un = new TextView(this);
                        un.setText(entry.username);
                        tr.addView(un);
                        TextView pwd = new TextView(this);
                        pwd.setText(entry.password);
                        tr.addView(pwd);

                        tl.addView(tr);
                    }
                }
            }
        } else if (requestCode == 3) {
            int i = Integer.parseInt(data.getStringExtra("operation"));
            int copy = Integer.parseInt(data.getStringExtra("copy"));
            if (i == 0) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                for (PasswordEntry entry : fileData) {
                    if (entry.index == copy) {
                        ClipData clip = ClipData.newPlainText("username", entry.username);
                        assert clipboard != null;
                        clipboard.setPrimaryClip(clip);
                    }
                }
            } else if (i == 1) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                for (PasswordEntry entry : fileData) {
                    if (entry.index == copy) {
                        ClipData clip = ClipData.newPlainText("username", entry.password);
                        assert clipboard != null;
                        clipboard.setPrimaryClip(clip);
                    }
                }
            }
        }
    }

    public void addButton(View view) {
        Intent intent = new Intent(this, AddActivity.class);
        startActivityForResult(intent, 0);
    }

    public void deleteButton(View view) {
        Intent intent = new Intent(this, DeleteActivity.class);
        startActivityForResult(intent, 1);
    }

    public void filterButton(View view) {
        Intent intent = new Intent(this, FilterActivity.class);
        startActivityForResult(intent, 2);
    }

    public void resetFilterButton(View view) {
        TableLayout tl = findViewById(R.id.tableLayout);
        clearTable();
        int i2 = 0;
        for (PasswordEntry entry : fileData) {
            i2++;
            TableRow tr = new TableRow(this);
            tr.setId(i2);
            TextView id = new TextView(this);
            id.setText(Integer.toString(entry.index));
            tr.addView(id);
            TextView dom = new TextView(this);
            dom.setText(entry.domain);
            tr.addView(dom);
            TextView un = new TextView(this);
            un.setText(entry.username);
            tr.addView(un);
            TextView pwd = new TextView(this);
            pwd.setText(entry.password);
            tr.addView(pwd);

            tl.addView(tr);
        }
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
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            e1.printStackTrace();
        }
    }

    public void copyButton(View view) {
        Intent intent = new Intent(this, CopyActivity.class);
        startActivityForResult(intent, 3);
    }
}
