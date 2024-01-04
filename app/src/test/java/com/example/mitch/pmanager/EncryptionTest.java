package com.example.mitch.pmanager;

import static com.example.mitch.pmanager.util.Constants.Encryption.RANDOM;
import static com.example.mitch.pmanager.util.StringsUtil.bytesToChars;
import static org.junit.Assert.assertEquals;

import com.example.mitch.pmanager.models.EncryptedValue;
import com.example.mitch.pmanager.models.FileKey;
import com.example.mitch.pmanager.util.KeyStoreUtil;

import org.junit.Test;

import java.util.Arrays;

/**
 * Tests encryption stuff
 */
public class EncryptionTest {

    @Test
    public void testEncrypt() {
        byte[] key = new byte[256 / 8];
        KeyStoreUtil.setCustomApplicationKey(key);

        byte[] input = new byte[64];
        byte[] ad = new byte[16];
        RANDOM.nextBytes(ad);
        RANDOM.nextBytes(input);
        byte[] inputclone = Arrays.copyOf(input, input.length);

        byte[] password = new byte[16];
        RANDOM.nextBytes(password);
        byte[] pwclone = Arrays.copyOf(password, password.length);

        FileKey testKey = new FileKey(bytesToChars(password, true));
        EncryptedValue encrypted = testKey.encrypt(input, ad);
        FileKey decryptKey = new FileKey(bytesToChars(pwclone, true), testKey.getSalt());

        try {
            byte[] decrypted = decryptKey.decrypt(encrypted, ad);
            assertEquals(Arrays.toString(inputclone), Arrays.toString(decrypted));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
