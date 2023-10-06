package com.example.mitch.pmanager;

import static com.example.mitch.pmanager.Constants.Version.V3;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.mitch.pmanager.background.Encryptor;
import com.example.mitch.pmanager.interfaces.Writable;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.PasswordEntry;
import com.example.mitch.pmanager.objects.storage.PasswordBank;
import com.example.mitch.pmanager.objects.storage.UserEntry;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Utilities class for various functions used in multiple places.
 */
public class Util {

    public static int DP16;

    public static Comparator<File> FILE_COMPARATOR = Comparator.comparing(File::getName);

    /**
     * Copies the file at source to dest in 1MB chunks
     *
     * @param source Path to the source file
     * @param dest   Path to the destination file
     * @return True if the copy succeeded, false if it failed.
     */
    public static boolean copyFile(Path source, Path dest) {
        try (
                InputStream in = Files.newInputStream(source);
                OutputStream out = Files.newOutputStream(dest)
        ) {
            return copyFile(in, out);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyFile(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        return true;
    }

    /**
     * Gets a string of the contents of a given EditText
     *
     * @param viewId R id of the edit text to read
     * @param view   Source view to find the edit text in
     * @return String of the edit text's contents.
     */
    public static String getFieldString(int viewId, View view) {
        TextInputLayout field = view.findViewById(viewId);
        return Objects.requireNonNull(field.getEditText()).getText().toString();
    }

    /**
     * Gets a char[] of the contents of a given EditText.
     * Avoids creating any strings for secure access.
     * @param viewId R id of the edit text to read
     * @param view Source view to find the edit text in
     * @return char[] of the contents.
     */
    public static char[] getFieldChars(int viewId, View view) {
        TextInputLayout field = view.findViewById(viewId);
        EditText editText = field.getEditText();
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

    /**
     * Writes an encrypted file.
     *
     * @param writable       Object to write to the file
     * @param file           File to write to
     * @param associatedData Associated Data for the encryption
     * @param pwd            Password for the encryption
     * @return True if writing succeeds, false if it failed.
     */
    public static boolean writeFile(Writable writable, File file, byte[] associatedData, char[] pwd) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(writable);
            oos.flush();
            Encryptor.EncryptedData encrypted = Encryptor.encrypt(bos.toByteArray(), associatedData, pwd);
            Encryptor.writeEncrypted(encrypted, file);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Decrypts and retrieves an object from a given file
     *
     * @param associatedData Associated data for the decryption
     * @param pwd            Password for the decryption
     * @param file           File to decrypt
     * @return Object of the data
     * @throws Exception Thrown if decryption fails.
     */
    public static PasswordBank readFile(byte[] associatedData, char[] pwd, File file) throws Exception {
        Encryptor.EncryptedData encrypted = Encryptor.readFromFile(file);
        byte[] data = Encryptor.decrypt(encrypted, associatedData, pwd);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object read = ois.readObject();
            if (read.getClass() == PMFile.class) {
                ((PMFile) read).setFile(file);
                return PMFileToBank((PMFile) read);
            }
            return (PasswordBank) read;
        } catch (Exception e) {
            return PMFileToBank(parseV2Data(data, file));
        }
    }

    /**
     * Parses a byte[] of data into a PMFile for use with the rest of the program.
     * @param data Data to parse
     * @return Processed PMFile for easy use in the program
     */
    public static PMFile parseV2Data(byte[] data, File file) {
        ArrayList<PasswordEntry> entries = new ArrayList<>();
        char[][] dataList = splitByChar(bytesToChars(data), '\n');
        int entryCount = (dataList.length - 1) / 3;
        for (int i = 0; i < entryCount; i++) {
            int entryIndex = i * 3 + 1;
            entries.add(new PasswordEntry(dataList[entryIndex], dataList[entryIndex + 1], dataList[entryIndex + 2], i + 1));
        }
        return new PMFile(V3, entries, file);
    }

    /**
     * Converts a PMFile to a password bank
     * @param file PMFile to convert
     * @return converted PasswordBank
     */
    private static PasswordBank PMFileToBank(PMFile file) {
        PasswordBank bank = new PasswordBank();
        for (PasswordEntry entry : file.getPasswordEntries()) {
            String domain = String.valueOf(entry.domain);
            bank.getEntry(domain).add(new UserEntry(entry.username, entry.password));
        }
        return bank;
    }

    public static void setStatusBarColors(Activity activity) {
        setStatusBarColors(activity.getResources(), activity.getWindow());
    }

    public static void setStatusBarColors(Resources resources, Window window) {
        int nightModeFlags =
                resources.getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        boolean lightMode = nightModeFlags != Configuration.UI_MODE_NIGHT_YES;

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());

        windowInsetsController.setAppearanceLightNavigationBars(lightMode);
        windowInsetsController.setAppearanceLightStatusBars(lightMode);
    }

    public static void setWindowInsets(View view, int bottomMargin, int rightMargin, int leftMargin) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            mlp.bottomMargin = insets.bottom + bottomMargin;
            mlp.rightMargin = insets.right + rightMargin;
            mlp.leftMargin = insets.left + leftMargin;
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });
    }
}