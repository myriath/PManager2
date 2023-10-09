package com.example.mitch.pmanager.util;

import android.view.View;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Utilities class for various functions used in multiple places.
 */
public class ByteCharStringUtil {

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

    public static char[] getFieldChars(EditText editText) {
        int length = Objects.requireNonNull(editText).length();
        char[] chars = new char[length];
        Objects.requireNonNull(editText.getText()).getChars(0, length, chars, 0);
        return chars;
    }

    /**
     * Splits a char[] into a char[][] by the given splitter char
     * Mimics the String.split() command but with a single character regex
     * @param toSplit char[] to split
     * @param splitter char to split toSplit by
     * @return char[][]
     */
    public static char[][] splitByChar(char[] toSplit, char splitter) {
        int i;
        int terms = 0;

        // Create array with max possible term count
        char[][] temp = new char[toSplit.length + 1][];

        // Create term arrays for each term with the proper length
        int last = 0;
        for (i = 0; i < toSplit.length; i++) {
            if (toSplit[i] == splitter) {
                temp[terms++] = new char[i - last];
                last = i+1;
            }
        }
        temp[terms++] = new char[i - last];

        // Copy each term into the correct place
        int term = 0;
        i = 0;
        for (char c : toSplit) {
            if (c == splitter) {
                term++;
                i = 0;
                continue;
            }
            temp[term][i++] = c;
        }

        // Remove trailing empty terms
        int termsExcludeTrailing = terms;
        for (i = terms - 1; i > -1; i--) {
            if (temp[i] != null && temp[i].length != 0) break;
            else termsExcludeTrailing--;
        }

        // Reshape the temp array to the correct size
        char[][] ret = new char[termsExcludeTrailing][];
        System.arraycopy(temp, 0, ret, 0, termsExcludeTrailing);
        return ret;
    }

    /**
     * Converts an array of bytes to an array of chars
     * @param arr bytes to convert
     * @return converted chars
     */
    public static char[] bytesToChars(byte[] arr) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(arr);
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
        char[] chars = Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit());
        Arrays.fill(charBuffer.array(), (char) 0);
        return chars;
    }

    /**
     * Converts a char[] to a byte[]
     * @param arr array to convert
     * @return converted byte[]
     */
    public static byte[] charsToBytes(char[] arr) {
        CharBuffer charBuffer = CharBuffer.wrap(arr);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }

    /**
     * Removes the extension from a given string
     *
     * @param str String to format
     * @return Formatted string
     */
    public static String removeExtension(String str) {
        int sub = str.lastIndexOf('.');
        if (sub < 0) sub = str.length();
        return str.substring(0, sub);
    }

}
