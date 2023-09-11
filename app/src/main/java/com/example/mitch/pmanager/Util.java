package com.example.mitch.pmanager;

import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Utilities class for various functions used in multiple places.
 */
public class Util {
    /**
     * Copies the file at source to dest in 1MB chunks
     * @param source Path to the source file
     * @param dest Path to the destination file
     * @return True if the copy succeeded, false if it failed.
     */
    public static boolean copyFile(Path source, Path dest) {
        try (
                InputStream in = Files.newInputStream(source);
                OutputStream out = Files.newOutputStream(dest)
        ) {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets a string of the contents of a given EditText
     * @param viewId R id of the edit text to read
     * @param view Source view to find the edit text in
     * @return String of the edit text's contents.
     */
    public static String getFieldString(int viewId, View view) {
        EditText field = view.findViewById(viewId);
        return field.getText().toString();
    }

    /**
     * Gets a char[] of the contents of a given EditText.
     * Avoids creating any strings for secure access.
     * @param viewId R id of the edit text to read
     * @param view Source view to find the edit text in
     * @return char[] of the contents.
     */
    public static char[] getFieldChars(int viewId, View view) {
        EditText field = view.findViewById(viewId);
        int length = field.length();
        char[] chars = new char[length];
        field.getText().getChars(0, length, chars, 0);
        return chars;
    }

    /**
     * Splits a char[] into a char[][] by the given splitter char
     * @param toSplit char[] to split
     * @param splitter char to split toSplit by
     * @return char[][]
     */
    public static char[][] splitByChar(char[] toSplit, char splitter) {
        int i;
        int terms = 0;
        char[][] temp = new char[toSplit.length][];
        int last = 0;
        for (i = 0; i < toSplit.length; i++) {
            if (toSplit[i] == splitter) {
                temp[terms++] = new char[i - last];
                last = i+1;
            }
        }
        temp[terms++] = new char[i - last];
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
        char[][] ret = new char[terms][];
        System.arraycopy(temp, 0, ret, 0, terms);
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
}
