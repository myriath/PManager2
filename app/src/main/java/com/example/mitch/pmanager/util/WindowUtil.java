package com.example.mitch.pmanager.util;

import android.view.View;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class WindowUtil {

    /**
     * Gets a string of the contents of a given EditText
     *
     * @param viewId R id of the edit text to read
     * @param view   Source view to find the edit text in
     * @return String of the edit text's contents.
     */
    public static String getFieldString(int viewId, View view) {
        TextInputLayout field = view.findViewById(viewId);
        return getFieldString(Objects.requireNonNull(field.getEditText()));
    }

    /**
     * Gets an edit text's data as a String
     *
     * @param editText Edit text to retrieve data from
     * @return String of editText's data
     */
    public static String getFieldString(EditText editText) {
        return editText.getText().toString();
    }

    /**
     * Gets a char[] of the contents of a given EditText.
     * Avoids creating any strings for secure access.
     *
     * @param viewId R id of the edit text to read
     * @param view   Source view to find the edit text in
     * @return char[] of the contents.
     */
    public static char[] getFieldChars(int viewId, View view) {
        TextInputLayout field = view.findViewById(viewId);
        return getFieldChars(field.getEditText());
    }

    /**
     * Gets an edit text's data as a char[]
     *
     * @param editText Edit text to retrieve data from
     * @return char[] of editText's data
     */
    public static char[] getFieldChars(EditText editText) {
        int length = Objects.requireNonNull(editText).length();
        char[] chars = new char[length];
        Objects.requireNonNull(editText.getText()).getChars(0, length, chars, 0);
        return chars;
    }

}
