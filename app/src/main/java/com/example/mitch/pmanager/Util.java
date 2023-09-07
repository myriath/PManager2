package com.example.mitch.pmanager;

import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.background.Encryptor;
import com.example.mitch.pmanager.exceptions.DecryptionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.NoSuchPaddingException;

public class Util {
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

    public static void translateV2toV3(File in, File out, char[] pwd, String oldFilename, String filename) throws Exception {
        String data;
        try {
            String[] splitFile;
            AES decrypt = new AES(AES.pad(String.valueOf(pwd)));
            data = decrypt.decrypt(in);
            splitFile = data.split(System.lineSeparator());

            if (!splitFile[0].equals(oldFilename)) {
                translateV2toV3(in, out, pwd, oldFilename, filename);
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DecryptionException("");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        data = filename + data.substring(data.indexOf(System.lineSeparator()));
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] ad = filename.getBytes(StandardCharsets.UTF_8);
        Encryptor.EncryptedData encrypted = Encryptor.encrypt(dataBytes, ad, pwd);
        Encryptor.writeEncrypted(encrypted, out);
    }

    public static byte[] toBytes(char[] arr) {
        CharBuffer charBuffer = CharBuffer.wrap(arr);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }
}
